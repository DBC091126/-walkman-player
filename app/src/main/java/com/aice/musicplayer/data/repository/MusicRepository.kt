package com.aice.musicplayer.data.repository

import com.aice.musicplayer.data.local.dao.SongDao
import com.aice.musicplayer.data.local.entity.toEntity
import com.aice.musicplayer.data.scanner.FolderScanner
import com.aice.musicplayer.data.scanner.MediaScanner
import com.aice.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
    private val folderScanner: FolderScanner,
    private val mediaScanner: MediaScanner
) {

    fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs().map { entities -> entities.map { it.toDomain() } }

    fun getSongsInFolder(folderPath: String): Flow<List<Song>> =
        songDao.getSongsInFolder(folderPath)
            .map { entities -> entities.map { it.toDomain() } }

    fun getSongsInFolderRecursive(folderPath: String): Flow<List<Song>> =
        songDao.getSongsInFolderRecursive(folderPath)
            .map { entities -> entities.map { it.toDomain() } }

    fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query).map { entities -> entities.map { it.toDomain() } }

    fun getSongsByAlbum(album: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(album).map { entities -> entities.map { it.toDomain() } }

    fun getSongsByArtist(artist: String): Flow<List<Song>> =
        songDao.getSongsByArtist(artist).map { entities -> entities.map { it.toDomain() } }

    fun getAllAlbums(): Flow<List<String>> = songDao.getAllAlbums()

    fun getAllArtists(): Flow<List<String>> = songDao.getAllArtists()

    suspend fun scanDirectory(path: String) {
        val songs = folderScanner.scanDirectory(path)
        if (songs.isNotEmpty()) {
            songDao.deleteAll()
            songDao.insertAll(songs.map { it.toEntity() })
        }
    }

    suspend fun scanFromMediaStore() {
        val songs = mediaScanner.scanMediaStore()
        if (songs.isNotEmpty()) {
            songDao.deleteAll()
            songDao.insertAll(songs.map { it.toEntity() })
        }
    }

    suspend fun getSongById(id: Long): Song? =
        songDao.getSongById(id)?.toDomain()

    suspend fun getSongByFilePath(filePath: String): Song? =
        songDao.getSongByFilePath(filePath)?.toDomain()
}
