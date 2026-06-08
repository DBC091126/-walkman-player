package com.aice.musicplayer.domain.model

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int = 0,
    val coverArtPath: String = "",
    val hasSubfolders: Boolean = false,
    val depth: Int = 0
)
