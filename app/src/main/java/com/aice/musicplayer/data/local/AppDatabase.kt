package com.aice.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aice.musicplayer.data.local.dao.FolderDao
import com.aice.musicplayer.data.local.dao.PlaylistDao
import com.aice.musicplayer.data.local.dao.SongDao
import com.aice.musicplayer.data.local.entity.FolderEntity
import com.aice.musicplayer.data.local.entity.PlaylistEntity
import com.aice.musicplayer.data.local.entity.PlaylistSongCrossRef
import com.aice.musicplayer.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        FolderEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun folderDao(): FolderDao
    abstract fun playlistDao(): PlaylistDao
}
