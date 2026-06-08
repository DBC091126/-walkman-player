package com.aice.musicplayer.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

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
        val primary = "/storage/emulated/0"
        if (primary !in roots) roots.add(primary)

        // External SD card — check multiple mount points
        val searchPaths = listOf("/storage", "/mnt/media_rw", "/mnt")
        for (searchPath in searchPaths) {
            File(searchPath).listFiles()?.forEach { dir ->
                if (dir.isDirectory
                    && dir.name != "emulated"
                    && dir.name != "self"
                    && !dir.name.startsWith(".")
                    && dir.absolutePath !in roots
                ) {
                    roots.add(dir.absolutePath)
                }
            }
        }

        return roots
    }

    /**
     * Like Poweramp: recursively find ALL folders that contain music files under rootPath.
     * Returns folders grouped by depth for hierarchy display.
     */
    suspend fun findAllMusicFolders(rootPath: String): List<Folder> = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext emptyList()

        val result = mutableListOf<Folder>()

        // Use a queue for breadth-first traversal
        val queue = ArrayDeque<File>()
        rootDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            val audioFiles = mutableListOf<File>()
            val subDirs = mutableListOf<File>()

            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.startsWith(".")) {
                    subDirs.add(file)
                } else if (file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS) {
                    audioFiles.add(file)
                }
            }

            // Add subdirs to queue
            subDirs.sortedBy { it.name }.forEach { queue.add(it) }

            // Only include folders that actually have audio files
            if (audioFiles.isNotEmpty()) {
                val coverPath = findCoverArt(dir)
                val depth = dir.absolutePath
                    .removePrefix(rootPath)
                    .count { it == '/' }

                result.add(
                    Folder(
                        path = dir.absolutePath,
                        name = dir.name,
                        songCount = audioFiles.size,
                        coverArtPath = coverPath,
                        hasSubfolders = subDirs.isNotEmpty(),
                        depth = depth
                    )
                )
            }
        }

        result
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

    /**
     * Save embedded album art bitmap to app cache directory.
     * Returns the cache file path that can be loaded by Coil/AsyncImage.
     */
    private fun saveArtToCache(data: ByteArray, songName: String, folderPath: String): String {
        return try {
            val cacheDir = File(context.cacheDir, "album_art")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${folderPath.hashCode()}_${songName.hashCode()}.jpg")
            FileOutputStream(file).use { it.write(data) }
            file.absolutePath
        } catch (_: Exception) {
            ""
        }
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
                // Save embedded art to cache so AsyncImage can load it
                albumArtUri = saveArtToCache(embeddedPicture, file.nameWithoutExtension, folderPath)
            }
            if (albumArtUri.isBlank()) {
                // Fall back to folder cover files
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
