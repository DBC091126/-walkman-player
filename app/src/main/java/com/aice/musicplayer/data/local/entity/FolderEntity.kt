package com.aice.musicplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aice.musicplayer.domain.model.Folder

@Entity(tableName = "folder_cache")
data class FolderEntity(
    @PrimaryKey @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "song_count") val songCount: Int,
    @ColumnInfo(name = "cover_art_path") val coverArtPath: String,
    @ColumnInfo(name = "has_subfolders") val hasSubfolders: Boolean,
    @ColumnInfo(name = "last_scanned") val lastScanned: Long = System.currentTimeMillis()
) {
    fun toDomain(): Folder = Folder(
        path = path,
        name = name,
        songCount = songCount,
        coverArtPath = coverArtPath,
        hasSubfolders = hasSubfolders
    )
}
