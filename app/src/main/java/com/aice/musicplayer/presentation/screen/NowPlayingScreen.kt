package com.aice.musicplayer.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aice.musicplayer.domain.player.RepeatMode
import com.aice.musicplayer.presentation.components.WaveformSeekBar
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.playerState.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val song = state.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowDown,
                    contentDescription = "返回",
                    tint = WhiteText,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = WhiteMuted
                )
            }
            IconButton(onClick = { /* menu */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = WhiteText
                )
            }
        }

        if (song == null) {
            // No song playing
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
                        text = "未在播放",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhiteMuted
                    )
                    Text(
                        text = "从文件夹选择歌曲开始播放",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WhiteMuted
                    )
                }
            }
            return
        }

        // Album art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri.ifBlank { null },
                contentDescription = "专辑封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BlackCard),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Song info + Hi-Res badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                color = WhiteText,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = WhiteSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Hi-Res info badge
            if (song.bitDepth >= 24 || song.sampleRate > 48000) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = GoldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = buildString {
                            append(song.format.uppercase())
                            if (song.sampleRate > 0) {
                                append(" · ${song.sampleRate / 1000}kHz")
                            }
                            if (song.bitDepth > 0) {
                                append(" · ${song.bitDepth}bit")
                            }
                            if (song.bitRate > 0) {
                                append(" · ${song.bitRate}kbps")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seek bar
        WaveformSeekBar(
            currentPosition = position,
            duration = state.duration,
            onSeek = { viewModel.seekTo(it) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transport controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "随机播放",
                    tint = if (state.shuffleMode) GoldPrimary else WhiteSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { viewModel.skipPrevious() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一首",
                    tint = WhiteText,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Play / Pause
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "暂停" else "播放",
                    tint = BlackPure,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Next
            IconButton(
                onClick = { viewModel.skipNext() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一首",
                    tint = WhiteText,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { viewModel.cycleRepeatMode() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = when (state.repeatMode) {
                        RepeatMode.OFF -> Icons.Default.Repeat
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                    },
                    contentDescription = "循环模式",
                    tint = if (state.repeatMode != RepeatMode.OFF) GoldPrimary else WhiteSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation bar padding
        Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
    }
}
