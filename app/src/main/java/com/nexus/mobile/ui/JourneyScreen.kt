package com.nexus.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexus.mobile.NexusViewModel
import com.nexus.mobile.UiState

@Composable fun JourneyScreen(vm: NexusViewModel, state: UiState, back: () -> Unit, story: () -> Unit) {
    val progress = state.saved.progress
    val visited = (progress.history + progress.sceneId).distinct()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                PageTitle("Meu percurso", back); Spacer(Modifier.height(24.dp)); Eyebrow("A HISTÓRIA QUE VOCÊ DESENHOU", Cyan)
                Text("Cada salto\ndeixa uma marca.", fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1.2).sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                Text("Reveja suas cenas e decisões. Seu progresso permanece no ponto em que você parou.", color = Muted, fontSize = 14.sp,
                    lineHeight = 22.sp, modifier = Modifier.padding(vertical = 20.dp))
                LinearProgressIndicator(progress = { vm.engine.fraction(progress) }, color = Cyan, trackColor = Glass, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(30.dp))
            }
            itemsIndexed(visited, key = { _, id -> id }) { index, id ->
                val scene = vm.engine.scenes.getValue(id)
                val decision = progress.decisions.firstOrNull { it.sceneId == id }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(28.dp).background(if (id == progress.sceneId) Cyan else Glass, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = if (id == progress.sceneId) Ink else Color.White, fontSize = 10.sp)
                        }
                        if (index < visited.lastIndex) Box(Modifier.width(1.dp).weight(1f).background(Cyan.copy(.24f)))
                    }
                    GlassPanel(Modifier.weight(1f).padding(bottom = 16.dp).clickable { selected = id }) {
                        Column(Modifier.padding(20.dp)) {
                            Eyebrow(if (id == progress.sceneId && !progress.completed) "AGORA" else "MEMÓRIA", Cyan)
                            Text(scene.title, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                            decision?.let { Text(it.label, color = Pink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                            Text("Rever cena  ↗", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
            }
            item {
                if (!progress.completed) Notice("O próximo caminho ainda está por descobrir.")
                Spacer(Modifier.height(24.dp))
                NexusButton(if (progress.completed) "Revisitar meu final" else "Continuar minha história", Modifier.fillMaxWidth(), onClick = story)
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        selected?.let { id ->
            val scene = vm.engine.scenes[id]
            if (scene != null) Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                GlassPanel(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(scene.title, Modifier.weight(1f), fontSize = 20.sp)
                            IconButton({ selected = null }) { Icon(Icons.Outlined.Close, "Fechar memória") }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1920f / 1080f).clipToBounds()) {
                            SceneCanvas(scene, maxWidth.value / 1920f, scene.layers.maxOfOrNull { it.text.length } ?: 0)
                        }
                        scene.layers.filter { it.kind == "text" }.forEach { layer ->
                            Text(layer.text, color = if (layer.font == "Bangers") Pink else Color.White,
                                fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                        Text("Memória do seu percurso. Sua história segue salva.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp))
                    }
                }
            }
        }
    }
}
