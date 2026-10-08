package com.nexus.mobile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.data.Character

@Composable fun DimensionalCard(c: Character) {
    var back by rememberSaveable(c.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val enabled = LocalMotionEnabled.current
    val angle by animateFloatAsState(if (back) 180f else 0f, tween(if (enabled) 650 else 0), label = "Cartão dimensional")
    TiltFrame(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.55f).graphicsLayer { rotationY = angle; cameraDistance = 20f * density }
            .clip(CardShape).background(Brush.linearGradient(listOf(Color(0xFF3D2855), Color(0xFF112C36))))
            .border(.7.dp, Color.White.copy(.25f), CardShape).clickable(role = Role.Button, onClickLabel = "Virar cartão dimensional") {
                back = !back
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }) {
            if (angle <= 90f) {
                Artwork(characterArt(c), Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Ink.copy(.85f), Ink.copy(.1f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                    Eyebrow("NEXUS / IDENTIDADE", Cyan)
                    Text(c.name, fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp).widthIn(max = 200.dp))
                    Text("Toque para virar", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Icon(Icons.Outlined.Flip, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).size(20.dp))
            } else {
                Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Eyebrow("ALÉM DA MÁSCARA", Cyan)
                    Text(c.role, color = Color.White, fontSize = 20.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium)
                    Text(c.powers.take(2).joinToString(" • ").ifBlank { "Uma conexão com o multiverso." }, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    Text("NEXUS  /  ${c.id.uppercase()}", fontSize = 9.sp, color = Cyan, letterSpacing = 1.sp)
                }
            }
        }
    }
}
