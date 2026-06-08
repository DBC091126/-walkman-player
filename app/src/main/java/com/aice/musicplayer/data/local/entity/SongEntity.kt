package com.aice.musicplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aice.musicplayer.domain.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["folder_path"]),
        Index(value = ["file_path"], unique = true),
        Index(value = ["album"]),
        Index(value = ["artist"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String,
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "sample_rate") val sampleRate: Int,
    @ColumnInfo(name = "bit_depth") val bitDepth: Int,
    @ColumnInfo(name = "bit_rate") val bitRate: Int,
    @ColumnInfo(name = "format") val format: String,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "date_added") val dateAdded: Long
) {
    fun toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumArtUri = albumArtUri,
        duration = duration,
        filePath = filePath,
        folderPath = folderPath,
        fileSize = fileSize,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        bitRate = bitRate,
        format = format,
        trackNumber = trackNumber,
        year = year,
        dateAdded = dateAdded
    )
}

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumArtUri = albumArtUri,
    duration = duration,
    filePath = filePath,
    folderPath = folderPath,
    fileSize = fileSize,
    sampleRate = sampleRate,
    bitDepth = bitDepth,
    bitRate = bitRate,
    format = format,
    trackNumber = trackNumber,
    year = year,
    dateAdded = dateAdded
)
