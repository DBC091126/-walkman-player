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

private fun storageLabel(path: String): String = when {
    path.contains("/emulated") || path.endsWith("/0") -> "内部存储"
    path.contains("/usb") || path.contains("USB") -> "USB 存储"
    else -> "SD 卡"
}

private fun storageSub(path: String): String {
    val f = File(path)
    val total = f.totalSpace
    val free = f.freeSpace
    return if (total > 0) "剩余 %.1f GB / 共 %.1f GB".format(free / 1e9, total / 1e9) else path
}

private fun storageIcon(path: String) = when {
    path.contains("/emulated") || path.endsWith("/0") -> Icons.Default.PhoneAndroid
    path.contains("/usb") || path.contains("USB") -> Icons.Default.Usb
    else -> Icons.Default.SdCard
}

private fun uriToPath(uri: Uri): String? {
    val docId = try { DocumentsContract.getTreeDocumentId(uri) } catch (_: Exception) { return null }
    val parts = docId.split(":", limit = 2)
    if (parts.size < 2) return null
    val (volume, sub) = parts[0] to parts[1]
    return when (volume) {
        "primary", "home" -> "/storage/emulated/0/$sub"
        else -> "/storage/$volume/$sub"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderViewModel: FolderViewModel,
    playerViewModel: PlayerViewModel,
    onNowPlaying: () -> Unit
) {
    val ctx = LocalContext.current
    val currentPath by folderViewModel.currentPath.collectAsState()
    val folders by folderViewModel.folders.collectAsState()
    val songs by folderViewModel.songs.collectAsState()
    val isLoading by folderViewModel.isLoading.collectAsState()
    val isScanning by folderViewModel.isScanning.collectAsState()
    val storageRoots by folderViewModel.storageRoots.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val path = uriToPath(uri) ?: uri.lastPathSegment ?: uri.toString()
            folderViewModel.enterFolder(path)
        }
    }

    val isRoot = currentPath == null

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                if (!isRoot) {
                    val parts = currentPath!!.split("/").filter { it.isNotBlank() }
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        parts.forEachIndexed { i, p ->
                            if (i > 0) Text(" / ", color = WhiteMuted, style = MaterialTheme.typography.titleSmall)
                            Text(p, color = if (i == parts.lastIndex) GoldPrimary else WhiteSecondary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (i == parts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1)
                        }
                    }
                } else Text("文件夹", fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                if (!isRoot) IconButton(onClick = { folderViewModel.goUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Box(Modifier.fillMaxSize()) {
            when {
                isRoot -> StorageRootView(storageRoots, { folderViewModel.enterFolder(it) }, { safLauncher.launch(null) })
                isScanning -> ScanningView(currentPath!!)
                else -> MusicFolderView(
                    folders = folders,
                    songs = songs,
                    currentSong = currentSong,
                    onFolderClick = { folderViewModel.openSubfolder(it.path) },
                    onSongClick = { folderViewModel.playSong(it, songs) },
                    onPlayAll = { folderViewModel.playAllSongsInFolder() }
                )
            }
        }
    }
}

@Composable
private fun ScanningView(path: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text("正在扫描音乐文件夹...", color = WhiteSecondary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(path, color = WhiteMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StorageRootView(
    roots: List<String>,
    onSelect: (String) -> Unit,
    onSystemPicker: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("选择存储位置", style = MaterialTheme.typography.headlineMedium, color = WhiteText, fontWeight = FontWeight.Bold)
            Text("浏览本地文件夹中的音乐", style = MaterialTheme.typography.bodyMedium, color = WhiteMuted, modifier = Modifier.padding(bottom = 24.dp, top = 4.dp))
        }
        items(roots) { root ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect(root) },
                colors = CardDefaults.cardColors(containerColor = BlackCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(52.dp), RoundedCornerShape(14.dp), color = GoldPrimary.copy(alpha = 0.12f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(storageIcon(root), null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(storageLabel(root), style = MaterialTheme.typography.titleMedium, color = WhiteText, fontWeight = FontWeight.SemiBold)
                        Text(storageSub(root), style = MaterialTheme.typography.bodySmall, color = WhiteMuted)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = WhiteMuted, modifier = Modifier.size(24.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = WhiteMuted.copy(alpha = 0.15f))
            Spacer(Modifier.height(20.dp))
            Text("找不到 SD 卡？", style = MaterialTheme.typography.titleSmall, color = WhiteMuted)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSystemPicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
            ) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("使用系统文件选择器", Modifier.padding(vertical = 6.dp))
            }
            Text("通过系统界面选择 SD 卡上的音乐文件夹", style = MaterialTheme.typography.bodySmall, color = WhiteMuted, modifier = Modifier.padding(top = 6.dp))
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MusicFolderView(
    folders: List<Folder>,
    songs: List<Song>,
    currentSong: Song?,
    onFolderClick: (Folder) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        if (folders.isNotEmpty()) {
            item {
                Text("文件夹 (${folders.size})", style = MaterialTheme.typography.titleSmall,
                    color = GoldPrimary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            items(folders, key = { it.path }) { folder ->
                FolderListItem(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    indent = folder.depth.coerceAtMost(5)
                )
                HorizontalDivider(color = WhiteMuted.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (songs.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("歌曲 (${songs.size})", style = MaterialTheme.typography.titleSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = onPlayAll,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = GoldPrimary.copy(alpha = 0.15f), contentColor = GoldPrimary)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部")
                    }
                }
            }
            items(songs, key = { it.filePath }) { song ->
                SongListItem(song = song, isActive = currentSong?.filePath == song.filePath, onClick = { onSongClick(song) })
                HorizontalDivider(color = WhiteMuted.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
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
