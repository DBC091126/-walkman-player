package com.aice.musicplayer.data.scanner

import android.media.MediaMetadataRetriever
import android.os.Environment
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderScanner @Inject constructor() {

    companion object {
        private val AUDIO_EXTENSIONS = setOf(
            "flac", "mp3", "wav", "ape", "ogg", "aac", "m4a",
            "alac", "wma", "opus", "aiff", "dsf", "dff"
        )

        private val COVER_ART_NAMES = setOf(
            "cover.jpg", "cover.png", "folder.jpg", "folder.png",
            "albumart.jpg", "albumart.png", "AlbumArt.jpg", "AlbumArt.png",
            "front.jpg", "front.png"
        )
    }

    /**
     * Scan a directory recursively and return a list of Songs.
     */
    suspend fun scanDirectory(rootPath: String): List<Song> = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext emptyList()

        val songs = mutableListOf<Song>()
        scanRecursive(rootDir, songs)
        songs
    }

    /**
     * Get subfolders of a directory with song counts.
     */
    suspend fun listFolders(parentPath: String): List<Folder> = withContext(Dispatchers.IO) {
        val parentDir = File(parentPath)
        if (!parentDir.exists() || !parentDir.isDirectory) return@withContext emptyList()

        parentDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { dir ->
                val songCount = countSongs(dir)
                val coverPath = findCoverArt(dir)
                val hasSubfolders = dir.listFiles()?.any { it.isDirectory } ?: false
                Folder(
                    path = dir.absolutePath,
                    name = dir.name,
                    songCount = songCount,
                    coverArtPath = coverPath,
                    hasSubfolders = hasSubfolders
                )
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /**
     * Scan a single directory (non-recursive) for audio files.
     */
    suspend fun scanDirectoryFlat(dirPath: String): List<Song> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
            ?.map { file -> extractMetadata(file, dirPath) }
            ?.sortedBy { it.trackNumber }
            ?: emptyList()
    }

    /**
     * Get the list of all available storage root paths (internal + external SD).
     */
    fun getStorageRoots(): List<String> {
        val roots = mutableListOf<String>()

        // Internal storage
        roots.add(Environment.getExternalStorageDirectory().absolutePath)

        // Also add primary external as fallback
        val primary = "/storage/emulated/0"
        if (primary !in roots) roots.add(primary)

        // External SD card — check multiple mount points
        val searchPaths = listOf("/storage", "/mnt/media_rw", "/mnt")
        for (searchPath in searchPaths) {
            File(searchPath).listFiles()?.forEach { dir ->
                if (dir.isDirectory
                    && dir.name != "emulated"
                    && dir.name != "self"
                    && dir.name != "sdcard"
                    && !dir.name.startsWith(".")
                    && dir.absolutePath !in roots
                ) {
                    roots.add(dir.absolutePath)
                }
            }
        }

        return roots
    }

    private fun scanRecursive(directory: File, songList: MutableList<Song>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanRecursive(file, songList)
            } else if (file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS) {
                val song = extractMetadata(file, directory.absolutePath)
                songList.add(song)
            }
        }
    }

    private fun countSongs(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS) {
                count++
            }
        }
        return count
    }

    private fun findCoverArt(directory: File): String {
        return COVER_ART_NAMES.firstOrNull { coverName ->
            File(directory, coverName).exists()
        }?.let { File(directory, it).absolutePath } ?: ""
    }

    private fun extractMetadata(file: File, folderPath: String): Song {
        var title = file.nameWithoutExtension
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var duration = 0L
        var sampleRate = 44100
        var bitDepth = 16
        var bitRate = 320
        var trackNumber = 0
        var year = 0
        var albumArtUri = ""
        var resolvedArtist = ""
        var resolvedAlbum = ""

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            resolvedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"
            resolvedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Unknown Album"
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val sampleRateStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
            )
            sampleRate = sampleRateStr?.toIntOrNull() ?: 44100

            val bitRateStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            )
            bitRate = (bitRateStr?.toIntOrNull() ?: 320) / 1000

            val bitsPerSample = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE
            )
            bitDepth = bitsPerSample?.toIntOrNull() ?: 16

            trackNumber = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
            )?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0

            year = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_YEAR
            )?.toIntOrNull() ?: 0

            // Check for embedded album art
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                albumArtUri = file.absolutePath  // Will be decoded from file
            } else {
                // Look for cover files in the same folder
                albumArtUri = findCoverArt(File(folderPath))
            }

            retriever.release()
        } catch (e: Exception) {
            // Fall back to filename if metadata extraction fails
            title = file.nameWithoutExtension
        }

        return Song(
            title = title.ifBlank { file.nameWithoutExtension },
            artist = resolvedArtist.ifBlank { "Unknown Artist" },
            album = resolvedAlbum.ifBlank { folderPath.substringAfterLast("/") },
            albumArtUri = albumArtUri,
            duration = duration,
            filePath = file.absolutePath,
            folderPath = folderPath,
            fileSize = file.length(),
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            bitRate = bitRate,
            format = file.extension.lowercase(),
            trackNumber = trackNumber,
            year = year
        )
    }
}
