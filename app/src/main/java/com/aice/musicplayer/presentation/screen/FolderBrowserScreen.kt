package com.aice.musicplayer.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aice.musicplayer.domain.model.Folder
import com.aice.musicplayer.domain.model.Song
import com.aice.musicplayer.presentation.components.FolderListItem
import com.aice.musicplayer.presentation.components.SongListItem
import com.aice.musicplayer.presentation.folder.FolderViewModel
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.theme.*

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

    var showStoragePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                if (currentPath != null) {
                    // Breadcrumb — show current folder name
                    val folderName = currentPath!!.substringAfterLast("/").ifBlank {
                        currentPath!!
                    }
                    Text(
                        text = folderName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("文件夹")
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
            actions = {
                // Open storage picker
                IconButton(onClick = { showStoragePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "选择存储"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Storage picker dialog
        if (showStoragePicker) {
            AlertDialog(
                onDismissRequest = { showStoragePicker = false },
                title = { Text("选择存储位置") },
                text = {
                    LazyColumn {
                        items(storageRoots) { root ->
                            TextButton(
                                onClick = {
                                    folderViewModel.enterFolder(root)
                                    showStoragePicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SdCard,
                                        contentDescription = null,
                                        tint = GoldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = root,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showStoragePicker = false }) {
                        Text("取消")
                    }
                },
                containerColor = BlackCard,
                titleContentColor = WhiteText,
                textContentColor = WhiteSecondary
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (currentPath == null) {
                // Welcome / root view — show storage roots
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    item {
                        Text(
                            text = "选择文件夹浏览音乐",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhiteMuted,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    items(storageRoots) { root ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { folderViewModel.enterFolder(root) },
                            colors = CardDefaults.cardColors(containerColor = BlackCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdCard,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = root,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WhiteText
                                    )
                                    Text(
                                        text = "点击浏览",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WhiteMuted
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else {
                // Folder content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // space for mini player
                ) {
                    // Folders section
                    if (folders.isNotEmpty()) {
                        item {
                            Text(
                                text = "文件夹",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldPrimary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(folders) { folder ->
                            FolderListItem(
                                folder = folder,
                                onClick = { folderViewModel.enterFolder(folder.path) }
                            )
                            HorizontalDivider(
                                color = WhiteMuted.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Songs section
                    if (songs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "歌曲 (${songs.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GoldPrimary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                TextButton(onClick = { folderViewModel.playAllSongsInFolder() }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("播放全部", color = GoldPrimary)
                                }
                            }
                        }
                        items(songs) { song ->
                            SongListItem(
                                song = song,
                                isActive = currentSong?.filePath == song.filePath,
                                onClick = {
                                    folderViewModel.playSong(song, songs)
                                }
                            )
                            HorizontalDivider(
                                color = WhiteMuted.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Empty state
                    if (folders.isEmpty() && songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = null,
                                        tint = WhiteMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "此文件夹为空",
                                        color = WhiteMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
