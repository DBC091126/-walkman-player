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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

// ── Sony color palette ──
private val GoldHiRes = Color(0xFFD8C28A)
private val White85 = Color(0xFFDADADA)
private val White65 = Color(0xFFA6A6A6)
private val White40 = Color(0xFF666666)

private suspend fun extractAlbumColors(path: String): Pair<Int, Int>? = withContext(Dispatchers.IO) {
    try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return@withContext null
        val palette = Palette.from(bitmap).generate()
        val dominant = palette.getDominantColor(android.graphics.Color.BLACK)
        val darkVibrant = palette.getDarkVibrantColor(dominant)
        val darkened = android.graphics.Color.argb(
            255,
            (android.graphics.Color.red(darkVibrant) * 0.55).toInt(),
            (android.graphics.Color.green(darkVibrant) * 0.50).toInt(),
            (android.graphics.Color.blue(darkVibrant) * 0.45).toInt()
        )
        Pair(dominant, darkened)
    } catch (_: Exception) { null }
}

@Composable
private fun rememberAlbumColors(path: String?): Pair<Color, Color> {
    var colors by remember { mutableStateOf(Pair(Color.Black, Color.Black)) }
    LaunchedEffect(path) {
        colors = if (path.isNullOrBlank()) Pair(Color.Black, Color.Black)
        else extractAlbumColors(path)?.let { Pair(Color(it.first), Color(it.second)) } ?: Pair(Color.Black, Color.Black)
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
    val animTop by animateColorAsState(topColor, tween(600, easing = FastOutSlowInEasing))
    val animMid by animateColorAsState(midColor, tween(600, easing = FastOutSlowInEasing))

    val bgBrush = Brush.verticalGradient(
        colors = listOf(animTop, animMid, Color.Black, Color.Black),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(bgBrush)
            .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), radius = 1200f))
    ) {
        if (song == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.MusicNote, null, tint = White40, Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("未在播放", color = WhiteMuted, style = MaterialTheme.typography.titleMedium)
            }
            return
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar — no card, no shadow
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp), interactionSource = remember { MutableInteractionSource() }) {
                    Icon(Icons.Default.KeyboardArrowDown, "返回", tint = White85, Modifier.size(28.dp))
                }
                Row {
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp), interactionSource = remember { MutableInteractionSource() }) {
                        Icon(Icons.Default.Lyrics, "歌词", tint = White85, Modifier.size(24.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp), interactionSource = remember { MutableInteractionSource() }) {
                        Icon(Icons.Default.MoreVert, "菜单", tint = White85, Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))

            // Square album art ~60% width
            AsyncImage(
                model = song.albumArtUri.ifBlank { null },
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxWidth(0.60f).aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(12.dp))

            // Hi-Res badge
            if (song.bitDepth >= 24 || song.sampleRate > 48000) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append(song.format.uppercase())
                            if (song.sampleRate > 0) append("  ${song.sampleRate / 1000}kHz")
                            if (song.bitDepth > 0) append(" / ${song.bitDepth}bit")
                        },
                        fontSize = 13.sp, color = GoldHiRes
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("HR", fontSize = 11.sp, color = GoldHiRes,
                        modifier = Modifier.border(1.dp, GoldHiRes, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Title + Favorite
            Row(Modifier.padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(song.title, fontSize = 30.sp, fontWeight = FontWeight.Normal, color = WhiteText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {}, modifier = Modifier.size(32.dp), interactionSource = remember { MutableInteractionSource() }) {
                    Icon(Icons.Default.StarBorder, "收藏", tint = White85, Modifier.size(24.dp))
                }
            }

            // Artist
            Text(song.artist, fontSize = 18.sp, color = White85, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp))

            // Album
            Text(song.album, fontSize = 16.sp, color = White65, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp))

            Spacer(Modifier.height(24.dp))

            // Progress
            Column(Modifier.padding(horizontal = 32.dp)) {
                SonySeekBar(position, state.duration) { viewModel.seekTo(it) }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatSony(position), color = White65, fontSize = 12.sp)
                    Text("${state.currentIndex + 1}/${state.totalCount}", color = White40, fontSize = 12.sp)
                    Text(formatSony(state.duration), color = White65, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Transport controls
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                SonyBtn(Icons.Default.Shuffle, state.shuffleMode, { viewModel.toggleShuffle() }, 44.dp, 26.dp)
                SonyBtn(Icons.Default.SkipPrevious, true, { viewModel.skipPrevious() }, 56.dp, 38.dp)
                // Center play/pause with ring
                CenterPlayBtn(state.isPlaying) { viewModel.togglePlayPause() }
                SonyBtn(Icons.Default.SkipNext, true, { viewModel.skipNext() }, 56.dp, 38.dp)
                SonyBtn(
                    when (state.repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                    state.repeatMode != RepeatMode.OFF, { viewModel.cycleRepeatMode() }, 44.dp, 26.dp
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SonyBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit,
                    size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120))
    IconButton(
        onClick = { pressed = true; onClick() },
        modifier = Modifier.size(size).scale(scale),
        interactionSource = remember { MutableInteractionSource() }
    ) { Icon(icon, null, tint = if (active) White85 else White40, modifier = Modifier.size(iconSize)) }
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
}

@Composable
private fun CenterPlayBtn(isPlaying: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(150))

    Box(Modifier.size(76.dp).scale(scale), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = White85, radius = size.minDimension / 2, style = Stroke(2.dp.toPx()))
        }
        IconButton(
            onClick = { pressed = true; onClick() },
            modifier = Modifier.size(68.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = WhiteText, Modifier.size(40.dp))
        }
        LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
    }
}

@Composable
private fun SonySeekBar(currentPosition: Long, duration: Long, onSeek: (Long) -> Unit) {
    var barWidth by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableStateOf(0f) }

    val frac = if (duration > 0) {
        if (isDragging) dragFrac else (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    Box(Modifier.fillMaxWidth().height(24.dp).onSizeChanged { barWidth = it.width.toFloat() }
        .pointerInput(Unit) {
            detectTapGestures { off -> onSeek(((off.x / barWidth).coerceIn(0f, 1f) * duration).toLong()) }
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { off -> isDragging = true; dragFrac = (off.x / barWidth).coerceIn(0f, 1f) },
                onDragEnd = { isDragging = false; onSeek((dragFrac * duration).toLong()) },
                onDragCancel = { isDragging = false },
                onHorizontalDrag = { _, dx -> dragFrac = (dragFrac + dx / barWidth).coerceIn(0f, 1f) }
            )
        },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track bg
        Box(Modifier.fillMaxWidth().height(2.dp).background(White40.copy(alpha = 0.25f)))
        // Progress
        Box(Modifier.fillMaxWidth(frac).height(2.dp).background(White65))
        // Dot
        if (barWidth > 0) {
            val dotOffsetDp = androidx.compose.ui.unit.Dp((barWidth * frac - 6f).coerceAtLeast(0f) / androidx.compose.ui.platform.LocalDensity.current.density)
            Box(Modifier.offset(x = dotOffsetDp).size(12.dp).clip(CircleShape).background(Color.White))
        }
    }
}

private fun formatSony(ms: Long): String {
    val sec = ms / 1000
    return "%d:%02d".format(sec / 60, sec % 60)
}
