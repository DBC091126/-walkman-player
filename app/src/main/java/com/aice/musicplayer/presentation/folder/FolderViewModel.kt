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

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val playerState = playbackController.playerState

    /**
     * Enter a folder — recursive scan for all music subfolders (Poweramp-style).
     */
    fun enterFolder(path: String) {
        _currentPath.value = path
        viewModelScope.launch {
            _isScanning.value = true
            try {
                _folders.value = folderRepository.findAllMusicFolders(path)
                _songs.value = folderRepository.scanDirectoryFlat(path)
            } catch (e: Exception) {
                _folders.value = emptyList()
                _songs.value = emptyList()
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Drill into subfolder — show songs + immediate subfolders only.
     */
    fun openSubfolder(path: String) {
        _currentPath.value = path
        viewModelScope.launch {
            _isScanning.value = true
            try {
                _songs.value = folderRepository.scanDirectoryFlat(path)
                _folders.value = folderRepository.listFolders(path)
            } catch (e: Exception) {
                _songs.value = emptyList()
                _folders.value = emptyList()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun goUp() {
        val current = _currentPath.value ?: return
        val parent = java.io.File(current).parent ?: return
        if (parent == java.io.File(current).path) {
            _currentPath.value = null
            _folders.value = emptyList()
            _songs.value = emptyList()
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
