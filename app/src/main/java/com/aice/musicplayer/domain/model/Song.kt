package com.aice.musicplayer.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumArtUri: String = "",
    val duration: Long = 0,
    val filePath: String,
    val folderPath: String = "",
    val fileSize: Long = 0,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val bitRate: Int = 320,
    val format: String = "",       // flac, mp3, wav, etc.
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)
