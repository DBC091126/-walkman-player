package com.aice.musicplayer.data.repository

import com.aice.musicplayer.data.local.dao.FolderDao
import com.aice.musicplayer.data.scanner.FolderScanner
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val folderScanner: FolderScanner
) {

    fun getStorageRoots(): List<String> = folderScanner.getStorageRoots()

    suspend fun listFolders(parentPath: String): List<Folder> {
        return folderScanner.listFolders(parentPath)
    }

    suspend fun scanDirectoryFlat(dirPath: String): List<Song> =
        folderScanner.scanDirectoryFlat(dirPath)

    /**
     * Poweramp-style: find ALL music folders under a storage root.
     */
    suspend fun findAllMusicFolders(rootPath: String): List<Folder> {
        return folderScanner.findAllMusicFolders(rootPath)
    }
}
