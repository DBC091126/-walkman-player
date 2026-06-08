package com.aice.musicplayer.presentation.screen

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import com.aice.musicplayer.presentation.components.FolderListItem
import com.aice.musicplayer.presentation.components.SongListItem
import com.aice.musicplayer.presentation.folder.FolderViewModel
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.theme.*
import java.io.File

private fun storageLabel(path: String): String {
    return when {
        path.contains("/emulated") || path.endsWith("/0") -> "内部存储"
        path.contains("/sdcard") || path.contains("SD") -> "SD 卡"
        path.contains("/usb") || path.contains("USB") -> "USB 存储"
        else -> File(path).name.ifBlank { "外部存储" }
    }
}

private fun storageSubLabel(path: String): String {
    val file = File(path)
    val free = file.freeSpace
    val total = file.totalSpace
    return if (total > 0) {
        val freeGb = free / (1024.0 * 1024 * 1024)
        "剩余 %.1f GB".format(freeGb)
    } else {
        path
    }
}

private fun storageIcon(path: String) = when {
    path.contains("/emulated") || path.endsWith("/0") -> Icons.Default.PhoneAndroid
    path.contains("/usb") || path.contains("USB") -> Icons.Default.Usb
    else -> Icons.Default.SdCard
}

/**
 * Try to convert a SAF content URI to a real file path.
 */
private fun uriToPath(uri: Uri): String? {
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (e: Exception) {
        return null
    }

    // Format: "primary:Music" or "XXXX-XXXX:Music"
    val parts = docId.split(":", limit = 2)
    if (parts.size < 2) return null

    val volume = parts[0]
    val subPath = parts[1]

    return when (volume) {
        "primary" -> "/storage/emulated/0/$subPath"
        "home" -> "/storage/emulated/0/$subPath"
        else -> "/storage/$volume/$subPath"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderViewModel: FolderViewModel,
    playerViewModel: PlayerViewModel,
    onNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    val currentPath by folderViewModel.currentPath.collectAsState()
    val folders by folderViewModel.folders.collectAsState()
    val songs by folderViewModel.songs.collectAsState()
    val isLoading by folderViewModel.isLoading.collectAsState()
    val storageRoots by folderViewModel.storageRoots.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    // SAF folder picker
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Take persistent permission
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) { }

            // Try to convert to file path
            val path = uriToPath(uri)
            if (path != null) {
                folderViewModel.enterFolder(path)
            } else {
                // Fallback: just use the URI path part
                val fallback = uri.lastPathSegment ?: uri.toString()
                folderViewModel.enterFolder(fallback)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                if (currentPath != null) {
                    val parts = currentPath!!.split("/").filter { it.isNotBlank() }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        parts.forEachIndexed { index, part ->
                            if (index > 0) {
                                Text(" / ", color = WhiteMuted, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                part,
                                color = if (index == parts.lastIndex) GoldPrimary else WhiteSecondary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (index == parts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Text("文件夹", fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                if (currentPath != null) {
                    IconButton(onClick = { folderViewModel.goUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }

                currentPath == null -> StorageRootView(
                    storageRoots = storageRoots,
                    onRootSelected = { folderViewModel.enterFolder(it) },
                    onSystemPicker = { safLauncher.launch(null) }
                )

                else -> FolderContentView(
                    folders = folders,
                    songs = songs,
                    currentSong = currentSong,
                    onFolderClick = { folderViewModel.enterFolder(it.path) },
                    onSongClick = { folderViewModel.playSong(it, songs) },
                    onPlayAll = { folderViewModel.playAllSongsInFolder() }
                )
            }
        }
    }
}

@Composable
private fun StorageRootView(
    storageRoots: List<String>,
    onRootSelected: (String) -> Unit,
    onSystemPicker: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text(
                "选择存储位置",
                style = MaterialTheme.typography.headlineMedium,
                color = WhiteText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "浏览本地文件夹中的音乐文件",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Detected storage roots
        items(storageRoots) { root ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onRootSelected(root) },
                colors = CardDefaults.cardColors(containerColor = BlackCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = GoldPrimary.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                storageIcon(root), null,
                                tint = GoldPrimary, modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(storageLabel(root), style = MaterialTheme.typography.titleMedium, color = WhiteText, fontWeight = FontWeight.SemiBold)
                        Text(storageSubLabel(root), style = MaterialTheme.typography.bodySmall, color = WhiteMuted)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = WhiteMuted, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Divider + system picker
        item {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = WhiteMuted.copy(alpha = 0.15f))
            Spacer(Modifier.height(24.dp))
            Text(
                "如果看不到 SD 卡",
                style = MaterialTheme.typography.titleSmall,
                color = WhiteMuted
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSystemPicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("使用系统文件选择器", modifier = Modifier.padding(vertical = 6.dp))
            }
            Text(
                "通过系统界面选择 SD 卡上的音乐文件夹",
                style = MaterialTheme.typography.bodySmall,
                color = WhiteMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun FolderContentView(
    folders: List<Folder>,
    songs: List<Song>,
    currentSong: Song?,
    onFolderClick: (Folder) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (folders.isNotEmpty()) {
            item {
                Text(
                    "文件夹",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(folders) { folder ->
                FolderListItem(folder = folder, onClick = { onFolderClick(folder) })
                HorizontalDivider(color = WhiteMuted.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (songs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("歌曲 (${songs.size})", style = MaterialTheme.typography.titleSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    FilledTonalButton(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GoldPrimary.copy(alpha = 0.15f),
                            contentColor = GoldPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部")
                    }
                }
            }
            items(songs) { song ->
                SongListItem(
                    song = song,
                    isActive = currentSong?.filePath == song.filePath,
                    onClick = { onSongClick(song) }
                )
                HorizontalDivider(color = WhiteMuted.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (folders.isEmpty() && songs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOff, null, tint = WhiteMuted, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("此文件夹为空", color = WhiteMuted, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
