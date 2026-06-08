package com.aice.musicplayer.domain.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aice.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    _playerState.update {
                        it.copy(
                            isPlaying = true,
                            duration = exoPlayer.duration
                        )
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSongFromMediaItem(mediaItem)
            }
        })
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex.coerceIn(0, songs.lastIndex.coerceAtLeast(0))

        _playerState.update { it.copy(currentIndex = currentIndex, totalCount = songs.size) }

        val mediaItems = songs.map { song -> song.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems, currentIndex, 0L)
        exoPlayer.prepare()
    }

    fun play() {
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun togglePlayPause() {
        if (exoPlayer.playWhenReady) pause() else play()
    }

    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        val newShuffle = !exoPlayer.shuffleModeEnabled
        exoPlayer.shuffleModeEnabled = newShuffle
        _playerState.update { it.copy(shuffleMode = newShuffle) }
    }

    fun getCurrentSong(): Song? {
        val idx = exoPlayer.currentMediaItemIndex
        return if (idx in playlist.indices) playlist[idx] else null
    }

    fun release() {
        exoPlayer.release()
    }

    private fun updateCurrentSongFromMediaItem(mediaItem: MediaItem?) {
        val idx = exoPlayer.currentMediaItemIndex
        currentIndex = idx
        if (idx in playlist.indices) {
            _playerState.update {
                it.copy(
                    currentSong = playlist[idx],
                    duration = exoPlayer.duration,
                    currentIndex = idx,
                    totalCount = playlist.size
                )
            }
        }
    }
}

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(filePath)
        .setMediaId(filePath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()
}
