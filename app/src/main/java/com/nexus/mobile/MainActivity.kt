package com.nexus.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.nexus.mobile.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NexusTheme { NexusApp() } }
    }
}

@Composable fun NexusApp(vm: NexusViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val motion = resumed && !state.saved.preferences.reducedMotion
    CompositionLocalProvider(LocalMotionEnabled provides motion) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val duration = if (motion) 420 else 0
        fun go(path: String) { nav.navigate(path) { launchSingleTop = true } }
        fun character(c: com.nexus.mobile.data.Character) { vm.select(c); go("character/${c.id}") }
        if (!state.ready) {
            Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
        } else Box(Modifier.fillMaxSize().background(Ink)) {
            NavHost(nav, startDestination = "splash", enterTransition = {
                fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { it / 14 }
            }, exitTransition = { fadeOut(tween(duration / 2)) }, popEnterTransition = { fadeIn(tween(duration)) },
                popExitTransition = { fadeOut(tween(duration / 2)) + slideOutHorizontally(tween(duration)) { it / 14 } }) {
                composable("splash") { VideoSplash { nav.navigate("home") { popUpTo("splash") { inclusive = true }; launchSingleTop = true } } }
                composable("home") {
                    HomeScreen(vm, state, { go("story") }, { go("episodes") }, { go("characters") }, { go("search") },
                        { go("profile") }, ::character, { go("portal") }, { go("journey") })
                }
                composable("characters") {
                    CatalogScreen(vm.cast, { go("search") }, { go("profile") }, { nav.popBackStack() }, ::character,
                        state.saved.favorites.map { it.id }.toSet(), state.saved.favorites)
                }
                composable("search") { SearchScreen(vm, state, { nav.popBackStack() }, ::character) }
                composable("character/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id").orEmpty()
                    LaunchedEffect(id) { vm.selectId(id) }
                    DetailScreen(state, vm::favorite, { state.selected?.let(vm::select) }, { go("search") }, { go("profile") }, { nav.popBackStack() })
                }
                composable("episodes") { EpisodesScreen(vm, state, { go("story") }, { nav.popBackStack() }) }
                composable("story") { StoryScreen(vm, state) { nav.popBackStack() } }
                composable("journal") { JournalScreen(vm, state, { nav.popBackStack() }, { go("story") }, { go("journey") }) }
                composable("journey") { JourneyScreen(vm, state, { nav.popBackStack() }, { go("story") }) }
                composable("portal") { PortalScreen(vm.cast, state.saved.favorites.map { it.id }.toSet(), { nav.popBackStack() }, ::character, vm::favorite) }
                composable("profile") {
                    ProfileScreen(vm, state, { nav.popBackStack() }, { go("journal") }, ::character, { go("portal") }, { go("journey") })
                }
            }
            if (route in listOf("home", "characters", "episodes")) {
                GlassPanel(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp).widthIn(max = 460.dp).fillMaxWidth()) {
                    Row(Modifier.padding(7.dp)) {
                        val tabs = listOf(Triple("home", "Início", Icons.Outlined.Home), Triple("episodes", "História", Icons.Outlined.AutoStories), Triple("characters", "Elenco", Icons.Outlined.People))
                        tabs.forEach { (path, label, icon) ->
                            TextButton(onClick = {
                                nav.navigate(path) { popUpTo("home") { saveState = true }; restoreState = true; launchSingleTop = true }
                            }, modifier = Modifier.weight(1f), shape = CircleShape,
                                colors = ButtonDefaults.textButtonColors(containerColor = if (route == path) Color.White.copy(.12f) else Color.Transparent)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = if (route == path) Color.White else Muted, modifier = Modifier.size(21.dp))
                                    Text(label, fontSize = 10.sp, color = if (route == path) Color.White else Muted,
                                        fontWeight = if (route == path) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
            state.storageError?.let { message ->
                Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp), color = Glass, shape = CircleShape) {
                    TextButton(onClick = vm::retrySave) { Text("$message • Tentar salvar", color = Color.White) }
                }
            }
        }
    }
}
