package com.nexus.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable fun VideoSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onFinished)
    var finished by remember { mutableStateOf(false) }
    fun finish() { if (!finished) { finished = true; callback() } }
    val exists = remember { runCatching { context.assets.openFd("video/splash_nexus.mp4").close(); true }.getOrDefault(false) }
    val player = remember(exists) { if (exists) ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri("asset:///video/splash_nexus.mp4")); volume = 0f; prepare(); playWhenReady = true
    } else null }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_ENDED) finish() }
            override fun onPlayerError(error: PlaybackException) { finish() }
        }
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_STOP -> player?.pause()
            Lifecycle.Event.ON_START -> player?.play()
            else -> Unit
        } }
        player?.addListener(listener); lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); player?.removeListener(listener); player?.release() }
    }
    LaunchedEffect(exists) { if (!exists) { delay(1000); finish() } }
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        if (player != null) AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM } }, modifier = Modifier.fillMaxSize())
        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("NEXUS", fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = 9.sp); Text("INFINITAS POSSIBILIDADES", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp) }
        TextButton(onClick = { finish() }, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) { Text("Pular") }
    }
}
