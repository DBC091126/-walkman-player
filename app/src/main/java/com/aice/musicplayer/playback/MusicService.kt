package com.aice.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.aice.musicplayer.MainActivity
import com.aice.musicplayer.R
import com.aice.musicplayer.domain.player.PlaybackController
import com.aice.musicplayer.domain.player.RepeatMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject lateinit var playbackController: PlaybackController

    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        const val CHANNEL_ID = "playback_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "WalkmanPlayer").apply {
            setCallback(SessionCallback())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    private fun updateMetadata() {
        val song = playbackController.getCurrentSong() ?: return

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .build()

        mediaSession.setMetadata(metadata)

        val state = if (playbackController.playerState.value.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, playbackController.exoPlayer.currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun buildNotification(): android.app.Notification {
        val song = playbackController.getCurrentSong()

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playPauseAction = if (playbackController.playerState.value.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                buildMediaButtonIntent(PlaybackStateCompat.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                buildMediaButtonIntent(PlaybackStateCompat.ACTION_PLAY)
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "WalkmanPlayer")
            .setContentText("${song?.artist} - ${song?.album}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous, "Previous",
                buildMediaButtonIntent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            )
            .addAction(playPauseAction)
            .addAction(
                android.R.drawable.ic_media_next, "Next",
                buildMediaButtonIntent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(playbackController.playerState.value.isPlaying)
            .build()
    }

    private fun buildMediaButtonIntent(action: Long): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            setAction("android.intent.action.MEDIA_BUTTON")
            putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, 0))
        }
        return PendingIntent.getService(
            this, action.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun startForegroundNotification() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    fun updateNotification() {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    private inner class SessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            playbackController.play()
            updateMetadata()
            updateNotification()
        }

        override fun onPause() {
            playbackController.pause()
            updateMetadata()
            updateNotification()
        }

        override fun onSkipToNext() {
            playbackController.skipToNext()
            updateMetadata()
            updateNotification()
        }

        override fun onSkipToPrevious() {
            playbackController.skipToPrevious()
            updateMetadata()
            updateNotification()
        }

        override fun onSeekTo(pos: Long) {
            playbackController.seekTo(pos)
            updateMetadata()
        }

        override fun onStop() {
            playbackController.pause()
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }
}
