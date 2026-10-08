package com.nexus.mobile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI

@Composable fun rememberMotionPhase(running: Boolean = true): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    val enabled = LocalMotionEnabled.current && running
    LaunchedEffect(enabled) {
        if (enabled) {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val elapsed = ((now - previous) / 1_000_000_000f).coerceIn(0f, .05f)
                phase.floatValue = (phase.floatValue + elapsed * (PI * 2 / 24).toFloat()) % (PI * 2).toFloat()
                previous = now
            }
        }
    }
    return phase
}

@Composable fun TiltFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val enabled = LocalMotionEnabled.current
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    val rx by animateFloatAsState(if (enabled) x else 0f, spring(stiffness = Spring.StiffnessLow), label = "Inclinação X")
    val ry by animateFloatAsState(if (enabled) y else 0f, spring(stiffness = Spring.StiffnessLow), label = "Inclinação Y")
    Box(modifier.graphicsLayer { rotationX = if (enabled) rx else 0f; rotationY = if (enabled) ry else 0f; cameraDistance = 18f * density }
        .pointerInput(enabled) {
            if (enabled) detectDragGestures(onDragEnd = { x = 0f; y = 0f }, onDragCancel = { x = 0f; y = 0f }) { change, amount ->
                change.consume()
                x = (x - amount.y / 22f).coerceIn(-9f, 9f)
                y = (y + amount.x / 22f).coerceIn(-12f, 12f)
            }
        }) { content() }
}
