package com.nexus.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.NexusViewModel
import com.nexus.mobile.UiState
import com.nexus.mobile.data.Character

@Composable fun CatalogScreen(cast: List<Character>, search: () -> Unit, profile: () -> Unit, back: () -> Unit,
    open: (Character) -> Unit, favorites: Set<String> = emptySet(), favoriteCharacters: List<Character> = emptyList()) {
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    val collection = (cast.filter { it.id in favorites } + favoriteCharacters).distinctBy { it.id }
    val shown = if (onlyFavorites) collection else cast
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 124.dp)) {
            item {
                Header(search, profile, back)
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(20.dp)); Eyebrow("ARQUIVO DO ARANHAVERSO", Pink)
                    Text("Cada herói.\nUm universo.", fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.padding(top = 20.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(!onlyFavorites, { onlyFavorites = false }, label = { Text("Todos · ${cast.size}") })
                        FilterChip(onlyFavorites, { onlyFavorites = true }, label = { Text("Favoritos · ${collection.size}") },
                            leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(15.dp)) })
                    }
                }
            }
            if (shown.isEmpty()) item { Box(Modifier.padding(horizontal = 24.dp)) { Notice("Sua coleção começa com uma conexão. Favorite um personagem na ficha.") } }
            items(shown, key = { it.id }) { c ->
                Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                    CharacterPoster(c, c.id in favorites) { open(c) }
                }
            }
        }
    }
}

@Composable private fun CharacterPoster(c: Character, favorite: Boolean, open: () -> Unit) {
    val accent = when (c.id) { "miles" -> Pink; "gwen" -> Color(0xFFD2A8FF); else -> Cyan }
    Box(Modifier.fillMaxWidth().aspectRatio(.95f).clip(CardShape).background(Glass)
        .border(.6.dp, Color.White.copy(.18f), CardShape).clickable(onClick = open)) {
        Artwork(characterArt(c), Modifier.fillMaxSize(), c.name)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.18f), Color.Transparent, Ink.copy(.92f)))))
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("NEXUS / ${c.id.uppercase()}", Color.White)
            if (favorite) Icon(Icons.Outlined.Favorite, "Favorito", Modifier.size(18.dp), tint = accent)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Text(c.name, fontSize = 30.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.role, Modifier.weight(1f), color = Muted, fontSize = 12.sp)
                Icon(Icons.Outlined.ArrowOutward, "Conhecer personagem", tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}
@Composable fun SearchScreen(vm: NexusViewModel, state: UiState, back: () -> Unit, open: (Character) -> Unit) {
    var input by rememberSaveable { mutableStateOf(state.query) }
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() { keyboard?.hide(); vm.search(input) }
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Voltar") }; Text("Buscar personagens", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), placeholder = { Text("Pesquisar personagens", fontSize = 13.sp) }, singleLine = true,
                    shape = CircleShape, leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = { if (input.isNotEmpty()) IconButton({ input = "" }) { Icon(Icons.Outlined.Close, "Limpar pesquisa") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submit() }))
                TextButton(onClick = { submit() }, enabled = input.trim().length >= 2 && !state.searching) { Text("Pesquisar") }
                if (!vm.online) Text("Catálogo local • Configure a Comic Vine para pesquisar online.", color = Muted, fontSize = 12.sp)
            }
            if (state.query.isEmpty()) {
                item { Heading("Pesquisas recentes", if (state.saved.recentSearches.isEmpty()) null else "Limpar", vm::clearRecents) }
                if (state.saved.recentSearches.isEmpty()) item { Notice("Busque um personagem pelo nome para conhecer sua história.") }
                items(state.saved.recentSearches) { q ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Glass).clickable { input = q; vm.search(q) }.padding(18.dp)) { Icon(Icons.Outlined.History, null); Spacer(Modifier.width(12.dp)); Text(q) }
                }
            } else {
                item { Heading("Resultados para “${state.query}”") }
                items(state.searchResults, key = { it.id }) { c -> CharacterRow(c) { open(c) } }
                if (state.searching) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                state.searchError?.let { error -> item { Notice(error, "Tentar novamente") { vm.search(state.query, state.searchResults.isNotEmpty()) } } }
                if (!state.searching && state.searchError == null && state.searchResults.isEmpty()) item { Notice("Nenhum personagem encontrado. Tente outro nome.") }
                if (!state.searching && state.searchResults.size < state.total && state.searchError == null) item { NexusButton("Carregar mais", Modifier.fillMaxWidth(), true) { vm.search(state.query, true) } }
                if (vm.online) item { Text("Dados de personagens: Comic Vine", color = Muted, fontSize = 12.sp) }
            }
        }
    }
}

@Composable fun DetailScreen(state: UiState, favorite: (Character) -> Unit, retry: () -> Unit,
    search: () -> Unit, profile: () -> Unit, back: () -> Unit) {
    val c = state.selected
    val uri = LocalUriHandler.current
    if (c == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NexusButton("Voltar", onClick = back) }; return }
    var expanded by rememberSaveable(c.id) { mutableStateOf(false) }
    val isFavorite = state.saved.favorites.any { it.id == c.id }
    val phase = rememberMotionPhase()
    AmbientBackground {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 142.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(510.dp).clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))) {
                    Artwork(characterArt(c), Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.035f; scaleY = 1.035f
                        translationX = kotlin.math.sin(phase.value) * 6.dp.toPx()
                    }, c.name)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.2f), Color.Transparent, Ink))))
                    Box(Modifier.statusBarsPadding()) { Header(search, profile, back) }
                    Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
                        Eyebrow(c.role, Pink); Spacer(Modifier.height(12.dp))
                        Text(c.name, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 28.dp).animateContentSize()) {
                    Spacer(Modifier.height(12.dp)); Eyebrow("POR TRÁS DA MÁSCARA")
                    Text(c.description.ifBlank { "Descrição não disponível." }, color = Muted, fontSize = 16.sp,
                        lineHeight = 25.sp, maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 18.dp))
                    if (c.description.length > 200) TextButton({ expanded = !expanded }) {
                        Text(if (expanded) "Ler menos" else "Continuar lendo", color = Color.White)
                    }
                    if (state.detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp), color = Pink)
                    state.detailError?.let { Notice(it, "Tentar novamente", retry) }
                    Spacer(Modifier.height(30.dp))
                    DimensionalCard(c)
                    Spacer(Modifier.height(30.dp))
                }
            }
            item {
                Box(Modifier.fillMaxWidth().background(Paper)) {
                    Column(Modifier.padding(28.dp)) {
                        Eyebrow("POTENCIAL EXTRAORDINÁRIO", Color(0xFF646474))
                        Text("O que faz\num herói.", color = Color(0xFF1D1D1F), fontSize = 34.sp, lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, modifier = Modifier.padding(top = 16.dp, bottom = 26.dp))
                        if (c.powers.isEmpty()) Text("Poderes não informados pela fonte.", color = Color(0xFF646474))
                        c.powers.forEachIndexed { index, power ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (index % 2 == 0) Icons.Outlined.Bolt else Icons.Outlined.AutoAwesome, null, Modifier.size(30.dp), tint = Color(0xFF7650A8))
                                Spacer(Modifier.width(18.dp))
                                Text(power, fontSize = 18.sp, color = Color(0xFF1D1D1F), fontWeight = FontWeight.Medium)
                            }
                            HorizontalDivider(color = Color(0xFFDADAE0), thickness = .6.dp)
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 28.dp)) {
                    Heading("Universos por onde passou.")
                    if (c.appearances.isEmpty()) Text("Aparições não informadas pela fonte.", color = Muted)
                    c.appearances.forEach { appearance ->
                        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoStories, null, tint = Cyan, modifier = Modifier.size(22.dp))
                                Text(appearance, Modifier.padding(start = 14.dp), color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    Text("Fonte: ${c.source}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 22.dp))
                    if (c.sourceUrl.startsWith("https://")) TextButton({ runCatching { uri.openUri(c.sourceUrl) } }) {
                        Text("Abrir na Comic Vine", color = Cyan)
                    }
                }
            }
        }
        GlassPanel(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Voltar", tint = Color.White) }
                NexusButton(if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos", Modifier.weight(1f)) { favorite(c) }
            }
        }
    }
}
