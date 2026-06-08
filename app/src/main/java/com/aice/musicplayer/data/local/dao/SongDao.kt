package com.aice.musicplayer.data.local.dao

import androidx.room.*
import com.aice.musicplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY folder_path ASC, track_number ASC, title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder_path = :folderPath ORDER BY track_number ASC, title ASC")
    fun getSongsInFolder(folderPath: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder_path LIKE :folderPath || '%' ORDER BY folder_path ASC, title ASC")
    fun getSongsInFolderRecursive(folderPath: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE file_path = :filePath LIMIT 1")
    suspend fun getSongByFilePath(filePath: String): SongEntity?

    @Query("SELECT DISTINCT folder_path FROM songs ORDER BY folder_path ASC")
    fun getAllFolderPaths(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT album FROM songs ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY track_number ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, track_number ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("DELETE FROM songs WHERE file_path NOT IN (:existingPaths)")
    suspend fun deleteMissing(existingPaths: List<String>)
}
