package com.nexus.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.data.Character
import kotlin.math.*

@Composable fun PortalOrbit(modifier: Modifier = Modifier, accent: Color = Pink, interactive: Boolean = true, paused: Boolean = false) {
    val phase = rememberMotionPhase(!paused)
    var yaw by remember { mutableFloatStateOf(.25f) }
    var pitch by remember { mutableFloatStateOf(.45f) }
    Canvas(modifier.semantics { contentDescription = "Portal tridimensional; arraste para girar" }
        .pointerInput(interactive) {
            if (interactive) detectDragGestures { change, delta ->
                change.consume()
                yaw = (yaw + delta.x * .007f).coerceIn(-6.28f, 6.28f)
                pitch = (pitch + delta.y * .007f).coerceIn(-1.2f, 1.2f)
            }
        }) {
        val radius = min(size.width, size.height) * .34f
        val center = this.center
        val t = phase.value
        drawCircle(Brush.radialGradient(listOf(accent.copy(.17f), Color.Transparent), center, radius * 1.5f), radius * 1.5f)
        fun project(x: Float, y: Float, z: Float): Offset {
            val angle = yaw + t * .12f
            val xx = x * cos(angle) + z * sin(angle)
            val zz = -x * sin(angle) + z * cos(angle)
            val yy = y * cos(pitch) - zz * sin(pitch)
            val depth = y * sin(pitch) + zz * cos(pitch)
            val perspective = 3f / (3f + depth)
            return center + Offset(xx, yy) * radius * perspective
        }
        repeat(6) { ring ->
            val path = Path()
            val tilt = ring * .47f
            for (step in 0..120) {
                val a = step / 120f * (PI * 2).toFloat()
                val r = .78f + ring * .045f
                val x = cos(a) * r
                val y = sin(a) * cos(tilt) * r
                val z = sin(a) * sin(tilt) * r
                val point = project(x, y, z)
                if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, accent.copy(.04f), style = Stroke(9.dp.toPx()))
            drawPath(path, accent.copy(.15f + ring * .07f), style = Stroke(if (ring % 2 == 0) 1.dp.toPx() else .5.dp.toPx()))
            val a = t * (1f + ring * .13f) + ring
            val r = .78f + ring * .045f
            val point = project(cos(a) * r, sin(a) * cos(tilt) * r, sin(a) * sin(tilt) * r)
            drawCircle(accent.copy(.08f), 11.dp.toPx(), point)
            drawCircle(accent.copy(.8f), 2.dp.toPx(), point)
        }
        repeat(36) { i ->
            val a = i * 2.39996f + t * .045f
            val r = 1.05f + (i % 7) * .055f
            val point = project(cos(a) * r, sin(a) * r, sin(i * .8f) * .5f)
            drawCircle(Color.White.copy(.15f + (i % 4) * .1f), if (i % 5 == 0) 1.4.dp.toPx() else .7.dp.toPx(), point)
        }
    }
}

@Composable fun PortalScreen(cast: List<Character>, favorites: Set<String>, back: () -> Unit,
    open: (Character) -> Unit, favorite: (Character) -> Unit) {
    if (cast.isEmpty()) { AmbientBackground { Notice("Nenhum personagem disponível.", "Voltar", back) }; return }
    var selectedId by rememberSaveable { mutableStateOf(cast.first().id) }
    var palette by rememberSaveable { mutableIntStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val c = cast.firstOrNull { it.id == selectedId } ?: cast.first()
    val accents = listOf(Pink, Cyan, Color(0xFFD7ABFF), Color(0xFFFFC583))
    val accent = accents[palette]
    val haptic = LocalHapticFeedback.current
    val motion = LocalMotionEnabled.current
    AmbientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            PageTitle("Laboratório", back)
            Spacer(Modifier.height(12.dp))
            Eyebrow("CONEXÃO COM O MULTIVERSO", accent)
            Text("Tudo está\nconectado.", fontSize = 39.sp, lineHeight = 41.sp, letterSpacing = (-1.3).sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                PortalOrbit(Modifier.fillMaxSize(), accent, paused = paused)
                AnimatedContent(c, transitionSpec = {
                    fadeIn(tween(if (motion) 350 else 0)) togetherWith fadeOut(tween(if (motion) 180 else 0))
                }, label = "Herói no portal") { hero ->
                    Artwork(characterArt(hero), Modifier.size(110.dp).graphicsLayer { rotationY = if (motion) -8f else 0f; cameraDistance = 18f * density }
                        .clip(CircleShape).background(Glass), hero.name)
                }
                Row(Modifier.align(Alignment.TopEnd)) {
                    IconButton({ paused = !paused }) {
                        Icon(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            if (paused) "Animar portal" else "Pausar portal", tint = accent)
                    }
                }
            }
            Text(c.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(c.role, color = Muted, fontSize = 13.sp)
            Text("Arraste o portal para explorar sua profundidade.", color = Muted, fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cast, key = { it.id }) { hero ->
                    FilterChip(selected = hero.id == c.id, onClick = {
                        selectedId = hero.id
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }, label = { Text(hero.name.substringBefore(" ")) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                accents.forEachIndexed { index, color ->
                    IconButton({ palette = index }, Modifier.size(48.dp)) {
                        Icon(if (palette == index) Icons.Outlined.RadioButtonChecked else Icons.Outlined.Circle,
                            "Cor do portal ${index + 1}", tint = color, modifier = Modifier.size(25.dp))
                    }
                }
                IconButton({ favorite(c) }) {
                    Icon(if (c.id in favorites) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        if (c.id in favorites) "Remover favorito" else "Favoritar", tint = Pink)
                }
            }
            NexusButton("Conhecer ${c.name.substringBefore(" ")}", Modifier.fillMaxWidth()) { open(c) }
            Spacer(Modifier.height(14.dp))
        }
    }
}
