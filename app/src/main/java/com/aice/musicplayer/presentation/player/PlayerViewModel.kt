package com.aice.musicplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aice.musicplayer.domain.model.Song
import com.aice.musicplayer.domain.player.PlaybackController
import com.aice.musicplayer.domain.player.PlayerState
import com.aice.musicplayer.domain.player.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playbackController.playerState

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    init {
        // Poll ExoPlayer for current position every 250ms
        viewModelScope.launch {
            while (true) {
                _playbackPosition.value = playbackController.exoPlayer.currentPosition
                delay(250)
            }
        }
    }

    val currentSong: Song?
        get() = playerState.value.currentSong

    val isPlaying: Boolean
        get() = playerState.value.isPlaying

    fun playSong(song: Song, playlist: List<Song>) {
        val index = playlist.indexOfFirst { it.filePath == song.filePath }.coerceAtLeast(0)
        playbackController.setPlaylist(playlist, index)
        playbackController.play()
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            playbackController.setPlaylist(songs, startIndex)
            playbackController.play()
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun skipNext() = playbackController.skipToNext()
    fun skipPrevious() = playbackController.skipToPrevious()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun toggleShuffle() = playbackController.toggleShuffle()

    fun cycleRepeatMode() {
        val current = playerState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playbackController.setRepeatMode(next)
    }

    fun getFormattedPosition(): String {
        val pos = _playbackPosition.value
        val dur = playerState.value.duration
        return "${formatTime(pos)} / ${formatTime(dur)}"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        // Don't release ExoPlayer here — it's a singleton managed by PlaybackController
    }
}
