package com.aice.musicplayer.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aice.musicplayer.data.repository.MusicRepository
import com.aice.musicplayer.domain.model.Song
import com.aice.musicplayer.presentation.components.SongListItem
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    musicRepository: MusicRepository,
    playerViewModel: PlayerViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("歌曲", "专辑", "艺术家")

    val allSongs by musicRepository.getAllSongs().collectAsState(initial = emptyList())
    val allAlbums by musicRepository.getAllAlbums().collectAsState(initial = emptyList())
    val allArtists by musicRepository.getAllArtists().collectAsState(initial = emptyList())

    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    val albumSongs by if (selectedAlbum != null) {
        musicRepository.getSongsByAlbum(selectedAlbum!!).collectAsState(initial = emptyList())
    } else remember { mutableStateOf(emptyList()) }

    val artistSongs by if (selectedArtist != null) {
        musicRepository.getSongsByArtist(selectedArtist!!).collectAsState(initial = emptyList())
    } else remember { mutableStateOf(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = when {
                        selectedAlbum != null -> selectedAlbum!!
                        selectedArtist != null -> selectedArtist!!
                        else -> "曲库"
                    }
                )
            },
            navigationIcon = {
                if (selectedAlbum != null || selectedArtist != null) {
                    IconButton(onClick = {
                        selectedAlbum = null
                        selectedArtist = null
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Album detail view
        if (selectedAlbum != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "${albumSongs.size} 首歌曲",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhiteMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(albumSongs) { song ->
                    SongListItem(
                        song = song,
                        isActive = currentSong?.filePath == song.filePath,
                        onClick = { playerViewModel.playSong(song, albumSongs) }
                    )
                    HorizontalDivider(
                        color = WhiteMuted.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            return
        }

        // Artist detail view
        if (selectedArtist != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "${artistSongs.size} 首歌曲",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhiteMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(artistSongs) { song ->
                    SongListItem(
                        song = song,
                        isActive = currentSong?.filePath == song.filePath,
                        onClick = { playerViewModel.playSong(song, artistSongs) }
                    )
                    HorizontalDivider(
                        color = WhiteMuted.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            return
        }

        // Tab bar for main library
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = GoldPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) GoldPrimary else WhiteSecondary
                        )
                    }
                )
            }
        }

        // Tab content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SongsTab(allSongs, currentSong) { song ->
                    playerViewModel.playSong(song, allSongs)
                }
                1 -> AlbumsTab(allAlbums, allSongs) { selectedAlbum = it }
                2 -> ArtistsTab(allArtists, allSongs) { selectedArtist = it }
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = WhiteMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "曲库为空",
                    style = MaterialTheme.typography.titleMedium,
                    color = WhiteMuted
                )
                Text(
                    text = "请从文件夹扫描音乐文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WhiteMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(songs) { song ->
                SongListItem(
                    song = song,
                    isActive = currentSong?.filePath == song.filePath,
                    onClick = { onSongClick(song) }
                )
                HorizontalDivider(
                    color = WhiteMuted.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<String>,
    allSongs: List<Song>,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = WhiteMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "没有专辑", color = WhiteMuted)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                val albumFirstSong = allSongs.firstOrNull { it.album == album }
                val albumSongCount = allSongs.count { it.album == album }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) },
                    colors = CardDefaults.cardColors(containerColor = BlackCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (albumFirstSong != null && albumFirstSong.albumArtUri.isNotBlank()) {
                            AsyncImage(
                                model = albumFirstSong.albumArtUri,
                                contentDescription = album,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = BlackElevated
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = WhiteMuted,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album,
                            style = MaterialTheme.typography.titleSmall,
                            color = WhiteText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$albumSongCount 首",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhiteMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<String>,
    allSongs: List<Song>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = WhiteMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "没有艺术家", color = WhiteMuted)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(artists) { artist ->
                val songCount = allSongs.count { it.artist == artist }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = BlackElevated
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleSmall,
                            color = WhiteText
                        )
                        Text(
                            text = "$songCount 首歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhiteMuted
                        )
                    }
                }
                HorizontalDivider(
                    color = WhiteMuted.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
