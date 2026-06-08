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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
        val bmp = BitmapFactory.decodeFile(path) ?: return@withContext null
        val p = Palette.from(bmp).generate()
        val d = p.getDominantColor(android.graphics.Color.BLACK)
        val dv = p.getDarkVibrantColor(d)
        Pair(d, android.graphics.Color.argb(255,
            (android.graphics.Color.red(dv) * 0.5).toInt(),
            (android.graphics.Color.green(dv) * 0.45).toInt(),
            (android.graphics.Color.blue(dv) * 0.4).toInt()))
    } catch (_: Exception) { null }
}

@Composable
private fun rememberAlbumColors(path: String?): Pair<Color, Color> {
    var c by remember { mutableStateOf(Pair(Color.Black, Color.Black)) }
    LaunchedEffect(path) {
        c = if (!path.isNullOrBlank()) extractAlbumColors(path)?.let {
            Pair(Color(it.first), Color(it.second))
        } ?: Pair(Color.Black, Color.Black) else Pair(Color.Black, Color.Black)
    }
    return c
}

@Composable
fun NowPlayingScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.playerState.collectAsState()
    val pos by viewModel.playbackPosition.collectAsState()
    val song = state.currentSong

    val artPath = song?.albumArtUri?.takeIf { File(it).exists() }
    val (topC, midC) = rememberAlbumColors(artPath)
    val aTop by animateColorAsState(topC, tween(600, easing = FastOutSlowInEasing), label = "t")
    val aMid by animateColorAsState(midC, tween(600, easing = FastOutSlowInEasing), label = "m")

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(aTop, aMid, Color.Black, Color.Black)))
            .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), radius = 1200f))
    ) {
        if (song == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.MusicNote, null, tint = White40, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("未在播放", color = WhiteMuted)
            }
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp),
                    interactionSource = remember { MutableInteractionSource() }) {
                    Icon(Icons.Default.KeyboardArrowDown, "返回", tint = White85, modifier = Modifier.size(28.dp))
                }
                Row {
                    IconButton(onClick = {}, modifier = Modifier.size(44.dp),
                        interactionSource = remember { MutableInteractionSource() }) {
                        Icon(Icons.Default.Lyrics, "歌词", tint = White85, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(44.dp),
                        interactionSource = remember { MutableInteractionSource() }) {
                        Icon(Icons.Default.MoreVert, "菜单", tint = White85, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Top spacing (adapts via screen height) ──
            Spacer(Modifier.height(56.dp))

            // ── Album art ──
            Box(
                modifier = Modifier.fillMaxWidth(0.58f).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (artPath != null) {
                    AsyncImage(
                        model = artPath,
                        contentDescription = "专辑封面",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no cover
                    Surface(
                        Modifier.fillMaxSize(),
                        color = BlackCard,
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Album, null, tint = White40, modifier = Modifier.size(64.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Hi-Res badge ──
            if (song.bitDepth >= 24 || song.sampleRate > 48000) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(buildString {
                        append(song.format.uppercase())
                        if (song.sampleRate > 0) append("  ${song.sampleRate / 1000}kHz")
                        if (song.bitDepth > 0) append(" / ${song.bitDepth}bit")
                    }, fontSize = 13.sp, color = GoldHiRes)
                    Spacer(Modifier.width(8.dp))
                    Text("HR", fontSize = 11.sp, color = GoldHiRes,
                        modifier = Modifier.border(1.dp, GoldHiRes, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp))
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(10.dp))
            }

            // ── Title + Favorite ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(song.title, fontSize = 28.sp, fontWeight = FontWeight.Normal, color = WhiteText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = {}, modifier = Modifier.size(36.dp),
                    interactionSource = remember { MutableInteractionSource() }) {
                    Icon(Icons.Default.StarBorder, "收藏", tint = White85, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Artist ──
            Text(song.artist, fontSize = 18.sp, color = White85, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 40.dp))

            Spacer(Modifier.height(2.dp))

            // ── Album ──
            Text(song.album, fontSize = 16.sp, color = White65, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 40.dp))

            Spacer(Modifier.height(28.dp))

            // ── Progress ──
            Column(Modifier.padding(horizontal = 40.dp)) {
                SonySeekBar(pos, state.duration) { viewModel.seekTo(it) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(pos), color = White65, fontSize = 12.sp)
                    Text("${state.currentIndex + 1}/${state.totalCount}", color = White40, fontSize = 12.sp)
                    Text(fmt(state.duration), color = White65, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Transport ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                SonyBtn(Icons.Default.Shuffle, state.shuffleMode, { viewModel.toggleShuffle() }, 48.dp, 26.dp)
                SonyBtn(Icons.Default.SkipPrevious, true, { viewModel.skipPrevious() }, 60.dp, 38.dp)
                CenterPlay(state.isPlaying) { viewModel.togglePlayPause() }
                SonyBtn(Icons.Default.SkipNext, true, { viewModel.skipNext() }, 60.dp, 38.dp)
                SonyBtn(
                    when (state.repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                    state.repeatMode != RepeatMode.OFF, { viewModel.cycleRepeatMode() }, 48.dp, 26.dp)
            }

            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.navigationBarsPadding())
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable Sony button ──
@Composable
private fun SonyBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean,
                    onClick: () -> Unit, size: Dp, iconSize: Dp) {
    var p by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (p) 0.92f else 1f, tween(120), label = "s")
    IconButton(onClick = { p = true; onClick() }, modifier = Modifier.size(size).scale(s),
        interactionSource = remember { MutableInteractionSource() }) {
        Icon(icon, null, tint = if (active) White85 else White40, modifier = Modifier.size(iconSize))
    }
    LaunchedEffect(p) { if (p) { delay(120); p = false } }
}

// ── Center play button ──
@Composable
private fun CenterPlay(isPlaying: Boolean, onClick: () -> Unit) {
    var p by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (p) 0.95f else 1f, tween(150), label = "c")
    Box(Modifier.size(76.dp).scale(s), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(White85, size.minDimension / 2, style = Stroke(2.dp.toPx()))
        }
        IconButton(onClick = { p = true; onClick() }, modifier = Modifier.size(68.dp),
            interactionSource = remember { MutableInteractionSource() }) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                tint = WhiteText, modifier = Modifier.size(40.dp))
        }
        LaunchedEffect(p) { if (p) { delay(150); p = false } }
    }
}

// ── Thin seek bar ──
@Composable
private fun SonySeekBar(pos: Long, dur: Long, onSeek: (Long) -> Unit) {
    var w by remember { mutableStateOf(0f) }
    var d by remember { mutableStateOf(false) }
    var df by remember { mutableStateOf(0f) }
    val f = if (dur > 0) (if (d) df else (pos.toFloat() / dur).coerceIn(0f, 1f)) else 0f
    val den = androidx.compose.ui.platform.LocalDensity.current.density

    Box(Modifier.fillMaxWidth().height(28.dp).onSizeChanged { w = it.width.toFloat() }
        .pointerInput(Unit) { detectTapGestures { onSeek(((it.x / w).coerceIn(0f, 1f) * dur).toLong()) } }
        .pointerInput(Unit) { detectHorizontalDragGestures(
            onDragStart = { d = true; df = (it.x / w).coerceIn(0f, 1f) },
            onDragEnd = { d = false; onSeek((df * dur).toLong()) },
            onDragCancel = { d = false },
            onHorizontalDrag = { _, dx -> df = (df + dx / w).coerceIn(0f, 1f) })
        }, contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(White40.copy(alpha = 0.25f)))
        Box(Modifier.fillMaxWidth(f).height(2.dp).background(White65))
        if (w > 0) Box(Modifier.offset(x = Dp((w * f - 6f).coerceAtLeast(0f) / den)).size(12.dp)
            .clip(CircleShape).background(Color.White))
    }
}

private fun fmt(ms: Long): String {
    val sec = ms / 1000
    return "%d:%02d".format(sec / 60, sec % 60)
}
