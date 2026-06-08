package com.aice.musicplayer.domain.player

import com.aice.musicplayer.domain.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val currentIndex: Int = 0,
    val totalCount: Int = 0
)

enum class RepeatMode {
    OFF, ONE, ALL
}
