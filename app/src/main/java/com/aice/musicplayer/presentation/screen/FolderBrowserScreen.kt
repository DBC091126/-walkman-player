package com.aice.musicplayer.presentation.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Human-readable label for a storage path.
 */
private fun storageLabel(path: String): String {
    return when {
        path.contains("/emulated") || path.endsWith("/0") -> "内部存储"
        path.contains("/sdcard") || path.contains("SD") -> "SD 卡"
        path.contains("/usb") || path.contains("USB") -> "USB 存储"
        else -> {
            val name = File(path).name
            if (name.isNotBlank() && name != "0") name else "外部存储"
        }
    }
}

private fun storageSubLabel(path: String): String {
    val free = File(path).freeSpace
    val total = File(path).totalSpace
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderViewModel: FolderViewModel,
    playerViewModel: PlayerViewModel,
    onNowPlaying: () -> Unit
) {
    val currentPath by folderViewModel.currentPath.collectAsState()
    val folders by folderViewModel.folders.collectAsState()
    val songs by folderViewModel.songs.collectAsState()
    val isLoading by folderViewModel.isLoading.collectAsState()
    val storageRoots by folderViewModel.storageRoots.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                if (currentPath != null) {
                    // Scrollable breadcrumb
                    val parts = currentPath!!.split("/").filter { it.isNotBlank() }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        parts.forEachIndexed { index, part ->
                            if (index > 0) {
                                Text(
                                    " / ",
                                    color = WhiteMuted,
                                    style = MaterialTheme.typography.titleSmall
                                )
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回上级"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Content area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // Loading
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }

                // Root — show storage devices
                currentPath == null -> StorageRootView(storageRoots) { root ->
                    folderViewModel.enterFolder(root)
                }

                // Browse folder
                else -> FolderContentView(
                    folders = folders,
                    songs = songs,
                    currentSong = currentSong,
                    onFolderClick = { folder -> folderViewModel.enterFolder(folder.path) },
                    onSongClick = { song -> folderViewModel.playSong(song, songs) },
                    onPlayAll = { folderViewModel.playAllSongsInFolder() }
                )
            }
        }
    }
}

@Composable
private fun StorageRootView(
    storageRoots: List<String>,
    onRootSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text(
                text = "选择存储位置",
                style = MaterialTheme.typography.headlineMedium,
                color = WhiteText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "浏览本地文件夹中的音乐文件",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = GoldPrimary.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = storageIcon(root),
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = storageLabel(root),
                            style = MaterialTheme.typography.titleMedium,
                            color = WhiteText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = storageSubLabel(root),
                            style = MaterialTheme.typography.bodySmall,
                            color = WhiteMuted
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = WhiteMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
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
        // Folders
        if (folders.isNotEmpty()) {
            item {
                SectionHeader("文件夹", folders.size.toString())
            }
            items(folders) { folder ->
                FolderListItem(folder = folder, onClick = { onFolderClick(folder) })
                HorizontalDivider(
                    color = WhiteMuted.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Songs
        if (songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("歌曲", songs.size.toString())
                    FilledTonalButton(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GoldPrimary.copy(alpha = 0.15f),
                            contentColor = GoldPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("播放全部", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            items(songs) { song ->
                SongListItem(
                    song = song,
                    isActive = currentSong?.filePath == song.filePath,
                    onClick = { onSongClick(song) }
                )
                HorizontalDivider(
                    color = WhiteMuted.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Empty
        if (folders.isEmpty() && songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = WhiteMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("此文件夹为空", color = WhiteMuted, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = GoldPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall,
            color = WhiteMuted
        )
    }
}
