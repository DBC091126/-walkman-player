package com.aice.musicplayer.presentation.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aice.musicplayer.data.repository.FolderRepository
import com.aice.musicplayer.data.repository.MusicRepository
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import com.aice.musicplayer.domain.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _storageRoots = MutableStateFlow<List<String>>(emptyList())
    val storageRoots: StateFlow<List<String>> = _storageRoots.asStateFlow()

    val playerState = playbackController.playerState

    init {
        _storageRoots.value = folderRepository.getStorageRoots()
    }

    /**
     * Enter a storage root — do a full recursive scan like Poweramp.
     */
    fun enterFolder(path: String) {
        _currentPath.value = path
        viewModelScope.launch {
            _isScanning.value = true
            _isLoading.value = true
            try {
                // Full recursive scan for all music folders
                _folders.value = folderRepository.findAllMusicFolders(path)
                // Also load immediate songs in the root
                _songs.value = folderRepository.scanDirectoryFlat(path)
            } catch (e: Exception) {
                _folders.value = emptyList()
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
                _isScanning.value = false
            }
        }
    }

    /**
     * Drill into a subfolder — show its songs only.
     */
    fun openSubfolder(path: String) {
        _currentPath.value = path
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = folderRepository.scanDirectoryFlat(path)
                // Also show subfolders of this folder
                _folders.value = folderRepository.listFolders(path)
            } catch (e: Exception) {
                _songs.value = emptyList()
                _folders.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateToRoot() {
        _currentPath.value = null
        _folders.value = emptyList()
        _songs.value = emptyList()
    }

    fun goUp() {
        val current = _currentPath.value ?: return
        val parent = java.io.File(current).parent ?: return
        // Check if parent is a storage root
        val isStorageRoot = _storageRoots.value.any { it == parent }
        if (isStorageRoot) {
            // Go back to storage root view — but keep scanning
            _currentPath.value = parent
            enterFolder(parent)
        } else if (_currentPath.value == parent) {
            navigateToRoot()
        } else {
            openSubfolder(parent)
        }
    }

    fun playSong(song: Song, songList: List<Song>) {
        val index = songList.indexOfFirst { it.filePath == song.filePath }.coerceAtLeast(0)
        playbackController.setPlaylist(songList, index)
        playbackController.play()
    }

    fun playAllSongsInFolder() {
        val songList = _songs.value
        if (songList.isNotEmpty()) {
            playbackController.setPlaylist(songList, 0)
            playbackController.play()
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun skipNext() = playbackController.skipToNext()
    fun skipPrevious() = playbackController.skipToPrevious()
}
