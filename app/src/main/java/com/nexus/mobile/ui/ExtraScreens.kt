package com.nexus.mobile.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.NexusViewModel
import com.nexus.mobile.UiState
import com.nexus.mobile.data.Character

@Composable fun PageTitle(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Voltar", tint = Color.White) }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable fun EpisodesScreen(vm: NexusViewModel, state: UiState, story: () -> Unit, back: () -> Unit) {
    var reset by rememberSaveable { mutableStateOf(false) }
    val p = state.saved.progress
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 124.dp, top = 16.dp)) {
            item {
                PageTitle("História", back); Spacer(Modifier.height(30.dp)); Eyebrow("TEMPORADA 01", Pink)
                Text("Uma escolha\nmuda tudo.", fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                Text("Acompanhe Miles em uma jornada entre universos. Descubra onde suas escolhas podem levar você.",
                    color = Muted, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 16.dp, bottom = 28.dp))
            }
            item { EpisodeCard("images/episode_latest.png", "Entre universos", "Episódio 01 • Disponível", "Entrar", vm.engine.fraction(p), story) }
            item {
                Heading("Você está aqui.")
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Eyebrow(if (p.completed) "CONCLUÍDO" else "PROGRESSO SALVO", Cyan)
                        Text(vm.engine.scene(p).title, fontSize = 22.sp, modifier = Modifier.padding(vertical = 14.dp))
                        Text("${p.decisions.size} decisões • ${(vm.engine.fraction(p) * 100).toInt()}% do seu caminho", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(22.dp))
                        NexusButton(if (p.completed) "Revisitar meu final" else "Continuar episódio", Modifier.fillMaxWidth(), onClick = story)
                        if (p.history.isNotEmpty()) TextButton({ reset = true }) { Text("Explorar outro caminho", color = Muted) }
                    }
                }
                Heading("O multiverso continua.")
                Notice("Novos episódios ainda não estão disponíveis. Explore o elenco enquanto sua próxima aventura não chega.")
            }
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, containerColor = Ink,
        title = { Text("Recomeçar episódio?") }, text = { Text("Seu percurso e as decisões serão substituídos. Seus favoritos permanecem salvos.", color = Muted) },
        confirmButton = { TextButton({ reset = false; vm.restart(); story() }) { Text("Recomeçar", color = Pink) } },
        dismissButton = { TextButton({ reset = false }) { Text("Cancelar", color = Color.White) } })
}

private fun shareJourney(context: Context, state: UiState) {
    val decisions = state.saved.progress.decisions
    val text = buildString {
        append("Meu percurso no Nexus\nEntre universos • Episódio 01\n\n")
        decisions.forEachIndexed { index, d -> append("${index + 1}. ${d.label}\n") }
        append(if (state.saved.progress.completed) "\nEpisódio concluído. Cada escolha fez parte da minha história." else "\nMinha história continua.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(intent, "Compartilhar meu percurso"))
}

@Composable fun JournalScreen(vm: NexusViewModel, state: UiState, back: () -> Unit, story: () -> Unit, journey: () -> Unit) {
    val context = LocalContext.current
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                PageTitle("Diário", back); Spacer(Modifier.height(22.dp)); Eyebrow("SEU MULTIVERSO", Pink)
                Text("O caminho\nque é seu.", fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.2).sp, modifier = Modifier.padding(top = 16.dp))
                Text("Um registro das decisões do seu percurso atual.", color = Muted, modifier = Modifier.padding(top = 14.dp))
            }
            if (state.saved.progress.decisions.isEmpty()) item { Notice("Sua primeira decisão ainda está por vir. Comece o episódio para construir sua história.") }
            items(state.saved.progress.decisions, key = { it.sceneId }) { choice ->
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.CallSplit, null, tint = Pink)
                        Eyebrow(vm.engine.scenes.getValue(choice.sceneId).title, Muted)
                        Text(choice.label, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
                    }
                }
            }
            item {
                NexusButton("Ver mapa do meu percurso", Modifier.fillMaxWidth(), true, journey)
                Spacer(Modifier.height(12.dp))
                if (state.saved.progress.decisions.isNotEmpty()) {
                    NexusButton("Compartilhar decisões", Modifier.fillMaxWidth(), true) { shareJourney(context, state) }
                    Spacer(Modifier.height(12.dp))
                }
                NexusButton(if (state.saved.progress.completed) "Revisitar desfecho" else "Continuar minha história", Modifier.fillMaxWidth(), onClick = story)
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable fun ProfileScreen(vm: NexusViewModel, state: UiState, back: () -> Unit, journal: () -> Unit,
    character: (Character) -> Unit, portal: () -> Unit, journey: () -> Unit) {
    val prefs = state.saved.preferences
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                PageTitle("Meu Nexus", back); Spacer(Modifier.height(26.dp)); Eyebrow("SEU UNIVERSO PARTICULAR", Cyan)
                Text("Você é parte\ndesta história.", fontSize = 39.sp, lineHeight = 42.sp, letterSpacing = (-1.3).sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("${state.saved.progress.decisions.size}" to "Decisões", "${state.saved.favorites.size}" to "Favoritos", "${(vm.engine.fraction(state.saved.progress) * 100).toInt()}%" to "Percurso").forEach { (value, title) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                                Text(title, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                NexusButton("Meu diário de decisões", Modifier.fillMaxWidth(), true, journal)
                Spacer(Modifier.height(10.dp)); NexusButton("Explorar meu percurso", Modifier.fillMaxWidth(), true, journey)
                Spacer(Modifier.height(10.dp)); NexusButton("Entrar no laboratório", Modifier.fillMaxWidth(), true, portal)
            }
            item {
                Heading("No seu ritmo.")
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Reduzir animações", fontSize = 16.sp)
                                Text("Movimento reduzido e diálogos completos", fontSize = 11.sp, color = Muted)
                            }
                            Switch(prefs.reducedMotion, { vm.preferences(prefs.copy(reducedMotion = it)) })
                        }
                        Spacer(Modifier.height(22.dp)); Text("Velocidade dos diálogos", fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Calma" to 40L, "Normal" to 22L, "Instantânea" to 0L).forEach { (label, speed) ->
                                FilterChip(prefs.textDelayMs == speed, { vm.preferences(prefs.copy(textDelayMs = speed)) }, label = { Text(label, fontSize = 11.sp) })
                            }
                        }
                    }
                }
                Heading("Suas conexões.")
                if (state.saved.favorites.isEmpty()) Notice("Favorite personagens para guardar suas fichas aqui.")
            }
            items(state.saved.favorites, key = { it.id }) { c -> CharacterRow(c) { character(c) } }
            item {
                Text("NEXUS\nSua história fica salva neste aparelho.", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 26.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
