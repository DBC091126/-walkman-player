package com.aice.musicplayer.presentation.screen

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.aice.musicplayer.domain.player.RepeatMode
import com.aice.musicplayer.presentation.player.PlayerViewModel
import com.aice.musicplayer.presentation.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private val GoldHiRes = Color(0xFFD8C28A)
private val White85 = Color(0xFFDADADA)
private val White65 = Color(0xFFA6A6A6)
private val White40 = Color(0xFF666666)

private suspend fun extractAlbumColors(path: String): Pair<Int, Int>? = withContext(Dispatchers.IO) {
    try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return@withContext null
        val palette = Palette.from(bitmap).generate()
        val dominant = palette.getDominantColor(android.graphics.Color.BLACK)
        val dark = palette.getDarkVibrantColor(dominant)
        val darkened = android.graphics.Color.argb(
            255,
            (android.graphics.Color.red(dark) * 0.5).toInt(),
            (android.graphics.Color.green(dark) * 0.45).toInt(),
            (android.graphics.Color.blue(dark) * 0.4).toInt()
        )
        Pair(dominant, darkened)
    } catch (_: Exception) { null }
}

@Composable
private fun rememberAlbumColors(path: String?): Pair<Color, Color> {
    var colors by remember { mutableStateOf(Pair(Color.Black, Color.Black)) }
    LaunchedEffect(path) {
        val extracted = if (!path.isNullOrBlank()) extractAlbumColors(path) else null
        colors = if (extracted != null) Pair(Color(extracted.first), Color(extracted.second))
        else Pair(Color.Black, Color.Black)
    }
    return colors
}

@Composable
fun NowPlayingScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.playerState.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val song = state.currentSong

    val albumPath = song?.albumArtUri?.takeIf { File(it).exists() }
    val (topColor, midColor) = rememberAlbumColors(albumPath)
    val animTop by animateColorAsState(topColor, tween(600, easing = FastOutSlowInEasing), label = "bgTop")
    val animMid by animateColorAsState(midColor, tween(600, easing = FastOutSlowInEasing), label = "bgMid")

    // Sony-style gradient background
    val bgBrush = Brush.verticalGradient(
        colors = listOf(animTop, animMid, Color.Black, Color.Black),
        startY = 0f, endY = Float.POSITIVE_INFINITY
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(bgBrush)
            .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), radius = 1200f))
    ) {
        if (song == null) {
            // Empty state
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = White40, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("未在播放", color = WhiteMuted, style = MaterialTheme.typography.titleMedium)
            }
            return
        }

        // ── Main layout ──
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ Top bar: back | [empty] | lyrics + menu ═══
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "返回", tint = White85, modifier = Modifier.size(28.dp))
                }
                Row {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(Icons.Default.Lyrics, contentDescription = "歌词", tint = White85, modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "菜单", tint = White85, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))

            // ═══ Album art — square, 58% width ═══
            AsyncImage(
                model = song.albumArtUri.ifBlank { null },
                contentDescription = "专辑封面",
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            // ═══ Hi-Res badge ═══
            if (song.bitDepth >= 24 || song.sampleRate > 48000) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(song.format.uppercase())
                            if (song.sampleRate > 0) append("  ${song.sampleRate / 1000}kHz")
                            if (song.bitDepth > 0) append(" / ${song.bitDepth}bit")
                        },
                        fontSize = 13.sp,
                        color = GoldHiRes
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "HR",
                        fontSize = 11.sp,
                        color = GoldHiRes,
                        modifier = Modifier
                            .border(1.dp, GoldHiRes, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // ═══ Title + Favorite ═══
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = WhiteText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(Icons.Default.StarBorder, contentDescription = "收藏", tint = White85, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            // ═══ Artist ═══
            Text(
                text = song.artist,
                fontSize = 18.sp,
                color = White85,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(Modifier.height(2.dp))

            // ═══ Album ═══
            Text(
                text = song.album,
                fontSize = 16.sp,
                color = White65,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ═══ Progress ═══
            Column(Modifier.padding(horizontal = 40.dp)) {
                SonySeekBar(
                    currentPosition = position,
                    duration = state.duration,
                    onSeek = { viewModel.seekTo(it) }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), color = White65, fontSize = 12.sp)
                    Text(
                        "${state.currentIndex + 1}/${state.totalCount}",
                        color = White40,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(formatTime(state.duration), color = White65, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ═══ Transport controls — strict symmetry ═══
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                SonyControlBtn(
                    icon = Icons.Default.Shuffle,
                    active = state.shuffleMode,
                    onClick = { viewModel.toggleShuffle() },
                    size = 48.dp,
                    iconSize = 26.dp
                )

                // Previous
                SonyControlBtn(
                    icon = Icons.Default.SkipPrevious,
                    active = true,
                    onClick = { viewModel.skipPrevious() },
                    size = 60.dp,
                    iconSize = 38.dp
                )

                // Play/Pause — center ring
                CenterPlayButton(isPlaying = state.isPlaying) {
                    viewModel.togglePlayPause()
                }

                // Next
                SonyControlBtn(
                    icon = Icons.Default.SkipNext,
                    active = true,
                    onClick = { viewModel.skipNext() },
                    size = 60.dp,
                    iconSize = 38.dp
                )

                // Repeat
                SonyControlBtn(
                    icon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    active = state.repeatMode != RepeatMode.OFF,
                    onClick = { viewModel.cycleRepeatMode() },
                    size = 48.dp,
                    iconSize = 26.dp
                )
            }

            // ── Large bottom space (Sony signature) ──
            Spacer(Modifier.height(48.dp))
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
}

// ── Reusable Sony-style icon button ──
@Composable
private fun SonyControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120), label = "scale")
    IconButton(
        onClick = { pressed = true; onClick() },
        modifier = Modifier.size(size).scale(scale),
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Icon(icon, contentDescription = null, tint = if (active) White85 else White40, modifier = Modifier.size(iconSize))
    }
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
}

// ── Center play button with ring ──
@Composable
private fun CenterPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(150), label = "playScale")

    Box(Modifier.size(76.dp).scale(scale), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = White85, radius = size.minDimension / 2, style = Stroke(2.dp.toPx()))
        }
        IconButton(
            onClick = { pressed = true; onClick() },
            modifier = Modifier.size(68.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = WhiteText,
                modifier = Modifier.size(40.dp)
            )
        }
        LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
    }
}

// ── Sony-style thin seek bar ──
@Composable
private fun SonySeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var barWidth by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableStateOf(0f) }

    val frac = if (duration > 0) {
        if (isDragging) dragFrac else (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { barWidth = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    onSeek(((off.x / barWidth).coerceIn(0f, 1f) * duration).toLong())
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        isDragging = true
                        dragFrac = (off.x / barWidth).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragFrac * duration).toLong())
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { _, dx ->
                        dragFrac = (dragFrac + dx / barWidth).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(Modifier.fillMaxWidth().height(2.dp).background(White40.copy(alpha = 0.25f)))
        // Progress
        Box(Modifier.fillMaxWidth(frac).height(2.dp).background(White65))
        // Dot
        if (barWidth > 0) {
            val dotOffset = (barWidth * frac - 6f).coerceAtLeast(0f)
            Box(
                Modifier
                    .offset(x = Dp(dotOffset / density.density))
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    return "%d:%02d".format(sec / 60, sec % 60)
}
