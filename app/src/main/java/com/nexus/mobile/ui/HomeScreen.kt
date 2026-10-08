package com.nexus.mobile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.NexusViewModel
import com.nexus.mobile.UiState
import com.nexus.mobile.data.Character
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable fun HomeScreen(vm: NexusViewModel, state: UiState, onStory: () -> Unit, onEpisodes: () -> Unit,
                           onCatalog: () -> Unit, onSearch: () -> Unit, onProfile: () -> Unit, onCharacter: (Character) -> Unit,
                           onPortal: () -> Unit, onJourney: () -> Unit) {
    val p = state.saved.progress
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 124.dp)) {
            item { HomeHero(p.history.isNotEmpty(), onStory, onEpisodes, onCatalog, onSearch, onProfile) }
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Heading(if (p.completed) "Você fez história." else if (p.history.isEmpty()) "Seu primeiro salto." else "Seu próximo salto.")
                    EpisodeCard("images/episode_continue.png", "Entre universos", "Episódio 01 • ${vm.engine.scene(p).title}",
                        if (p.completed) "Ver final" else if (p.history.isEmpty()) "Começar" else "Continuar", vm.engine.fraction(p), onStory)
                    Text("Seu progresso acompanha cada escolha.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                    Heading("Conexões.", "Ver elenco", onCatalog)
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(vm.cast, key = { it.id }) { c ->
                        Column(Modifier.width(108.dp).clickable { onCharacter(c) }) {
                            Box(Modifier.size(108.dp).clip(RoundedCornerShape(32.dp)).background(Glass)
                                .border(.6.dp, Color.White.copy(.18f), RoundedCornerShape(32.dp))) {
                                Artwork(characterArt(c), Modifier.fillMaxSize(), c.name)
                            }
                            Text(c.name.substringBefore(" "), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
                            Text(if (state.saved.favorites.any { it.id == c.id }) "Na sua coleção" else "Conhecer", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(46.dp))
                Box(Modifier.fillMaxWidth().background(Color.Black)) {
                    Column(Modifier.padding(28.dp)) {
                        Eyebrow("NOVAS DIMENSÕES", Color(0xFF646474))
                        Spacer(Modifier.height(18.dp))
                        Text("Um universo.\nInfinitas conexões.", color = Color.White, fontSize = 34.sp,
                            lineHeight = 37.sp, letterSpacing = (-1.2).sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(14.dp))
                        Text("Explore o portal. Encontre seu herói. Veja o caminho que suas escolhas desenharam.", color = Color.White, fontSize = 15.sp, lineHeight = 23.sp)
                        Spacer(Modifier.height(24.dp))
                        Box(Modifier.fillMaxWidth().height(240.dp).clip(CardShape).background(Ink).clickable(onClick = onPortal), contentAlignment = Alignment.Center) {
                            PortalOrbit(Modifier.fillMaxSize(), Cyan, interactive = false)
                            Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                                Text("Laboratório do multiverso", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text("Toque para entrar  ↗", color = Cyan, fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = onJourney, contentPadding = PaddingValues(top = 18.dp, bottom = 0.dp)) {
                            Text("Explorar meu percurso", color = Color(0xFF0066CC)); Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(17.dp), tint = Color(0xFF0066CC))
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Heading("Sua história.\nSeu universo.", "Episódios", onEpisodes)
                    EpisodeCard("images/episode_latest.png", "Entre universos", "Episódio 01 • Uma história, diferentes caminhos", "Explorar", null, onEpisodes)
                    Heading("Mais perto dos heróis.", "Buscar", onSearch)
                    vm.cast.forEach { c -> CharacterRow(c) { onCharacter(c) }; Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable private fun HomeHero(started: Boolean, story: () -> Unit, episodes: () -> Unit, characters: () -> Unit, search: () -> Unit, profile: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val motion = LocalMotionEnabled.current
    var playing by remember { mutableStateOf(true) }
    LaunchedEffect(pager.currentPage, playing, motion, pager.isScrollInProgress) {
        if (playing && motion && !pager.isScrollInProgress) {
            delay(7000)
            pager.animateScrollToPage((pager.currentPage + 1) % 3, animationSpec = tween(800))
        }
    }
    val titles = listOf("Toda origem\ncomeça com\numa escolha.", "O destino\nnão está\nescrito.", "Encontros\nque atravessam\nuniversos.")
    val subtitles = listOf("01 / SUA HISTÓRIA COMEÇA AQUI", "02 / SUAS ESCOLHAS IMPORTAM", "03 / CONHEÇA O ARANHAVERSO")
    val buttons = listOf(if (started) "Continuar história" else "Começar história", "Explorar episódios", "Conhecer personagens")
    Box(Modifier.fillMaxWidth().heightIn(min = 660.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth().height(660.dp)) { page ->
            val offset = (pager.currentPage - page + pager.currentPageOffsetFraction).absoluteValue
            Box(Modifier.fillMaxSize()) {
                Artwork("images/hero_${page + 1}.png", Modifier.fillMaxSize().graphicsLayer {
                    scaleX = if (motion) 1.05f + offset * .08f else 1f; scaleY = scaleX
                    translationX = if (motion) (pager.currentPage - page + pager.currentPageOffsetFraction) * size.width * .16f else 0f
                })
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.35f), Color.Transparent, Ink.copy(.55f), Ink))))
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 82.dp)) {
                    Eyebrow(subtitles[page], Color.White.copy(.75f))
                    Spacer(Modifier.height(18.dp))
                    Text(titles[page], fontWeight = FontWeight.SemiBold, fontSize = 43.sp, lineHeight = 44.sp, letterSpacing = (-1.7).sp)
                    Spacer(Modifier.height(26.dp))
                    NexusButton(buttons[page], onClick = listOf(story, episodes, characters)[page])
                }
            }
        }
        Box(Modifier.statusBarsPadding()) { Header(search, profile) }
        Row(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 24.dp).clip(CircleShape)
            .background(Color.White.copy(.13f)).border(.5.dp, Color.White.copy(.18f), CircleShape).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ playing = !playing }, Modifier.size(44.dp)) {
                Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    if (playing) "Pausar carrossel" else "Reproduzir carrossel", Modifier.size(17.dp))
            }
            repeat(3) { i ->
                val width by animateFloatAsState(if (pager.currentPage == i) 26f else 6f, tween(if (motion) 250 else 0), label = "Página")
                Box(Modifier.size(44.dp).clickable(onClickLabel = "Slide ${i + 1}") {
                    scope.launch { if (motion) pager.animateScrollToPage(i) else pager.scrollToPage(i) }
                }, contentAlignment = Alignment.Center) {
                    Box(Modifier.width(width.dp).height(5.dp).clip(CircleShape).background(if (pager.currentPage == i) Color.White else Color.White.copy(.4f)))
                }
            }
        }
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable fun EpisodeCard(image: String, title: String, subtitle: String, action: String, progress: Float?, onClick: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(CardShape).background(Glass).clickable(onClick = onClick)
        .border(.6.dp, Color.White.copy(.16f), CardShape)) {
        val artWidth = maxWidth
        val artHeight = maxHeight
        Artwork(image, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(.08f), Ink.copy(.72f)))))
        Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
            Eyebrow("É A SUA VEZ", Color.White)
            if (progress != null) Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).clip(RoundedCornerShape(0.dp))) {
            Artwork(image, Modifier.offset(y = -(artHeight - 96.dp)).wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(artWidth, artHeight).blur(22.dp))
            Box(Modifier.fillMaxSize().background(Ink.copy(.52f)))
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp)
                    Text(subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = Color.White.copy(.75f), maxLines = 2)
                }
                Spacer(Modifier.width(10.dp))
                Surface(shape = CircleShape, color = Color.White, contentColor = Ink) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(action, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ArrowForward, null, Modifier.padding(start = 5.dp).size(15.dp), tint = Ink)
                    }
                }
            }
        }
        if (progress != null) LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, color = Pink,
            trackColor = Color.White.copy(.1f), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp))
    }
}