package com.nexus.mobile.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.mobile.NexusViewModel
import com.nexus.mobile.UiState
import com.nexus.mobile.data.Scene
import kotlinx.coroutines.delay

private tailrec fun Context.activity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    vm: NexusViewModel,
    state: UiState,
    exit: () -> Unit
) {
    val activity = LocalContext.current.activity()

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    val progress = state.saved.progress
    val scene = vm.engine.scene(progress)
    val preferences = state.saved.preferences

    var menu by rememberSaveable {
        mutableStateOf(false)
    }

    var reading by rememberSaveable {
        mutableStateOf(false)
    }

    var reset by rememberSaveable {
        mutableStateOf(false)
    }

    val longest = scene.layers
        .filter {
            it.kind == "text" && it.font != "Bangers"
        }
        .maxOfOrNull {
            it.text.length
        }
        ?: 0

    var reveal by remember(scene.id) {
        mutableIntStateOf(
            if (
                preferences.reducedMotion ||
                preferences.textDelayMs == 0L ||
                scene.choices.isNotEmpty()
            ) {
                longest
            } else {
                0
            }
        )
    }

    LaunchedEffect(
        scene.id,
        preferences.textDelayMs,
        preferences.reducedMotion,
        menu,
        reading
    ) {
        if (
            preferences.reducedMotion ||
            preferences.textDelayMs == 0L ||
            scene.choices.isNotEmpty()
        ) {
            reveal = longest
        } else {
            while (
                reveal < longest &&
                !menu &&
                !reading
            ) {
                delay(
                    preferences.textDelayMs.coerceAtLeast(1L)
                )

                if (reveal < longest) {
                    reveal += 1
                }
            }
        }
    }

    fun next() {
        if (reveal < longest) {
            reveal = longest
        } else {
            vm.advance()
        }
    }

    BackHandler {
        if (reading) {
            reading = false
        } else {
            menu = !menu
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (progress.completed) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Pink
                )

                Text(
                    text = "Episódio concluído",
                    color = Color.White,
                    fontSize = 30.sp,
                    modifier = Modifier.padding(12.dp)
                )

                Text(
                    text = "${progress.decisions.size} escolhas. " +
                            "Um caminho que é seu.",
                    color = Muted
                )

                progress.decisions
                    .lastOrNull()
                    ?.label
                    ?.let { label ->
                        Text(
                            text = label,
                            color = Pink,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                NexusButton(
                    text = "Voltar ao Nexus",
                    onClick = exit
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                NexusButton(
                    text = "Explorar outro caminho",
                    secondary = true,
                    onClick = {
                        reset = true
                    }
                )

                TextButton(
                    onClick = vm::backScene
                ) {
                    Text(
                        text = "Rever última cena",
                        color = Color.White
                    )
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val scale = minOf(
                    maxWidth.value / 1920f,
                    maxHeight.value / 1080f
                )

                Box(
                    modifier = Modifier
                        .size(
                            width = (1920f * scale).dp,
                            height = (1080f * scale).dp
                        )
                        .clipToBounds()
                ) {
                    AnimatedContent(
                        targetState = scene,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = if (
                                        preferences.reducedMotion
                                    ) {
                                        0
                                    } else {
                                        180
                                    }
                                )
                            ) togetherWith fadeOut(
                                animationSpec = tween(
                                    durationMillis = if (
                                        preferences.reducedMotion
                                    ) {
                                        0
                                    } else {
                                        120
                                    }
                                )
                            )
                        },
                        label = "Mudança de cena"
                    ) { shown ->
                        val shownReveal = if (
                            shown.id == scene.id
                        ) {
                            reveal
                        } else {
                            shown.layers.maxOfOrNull {
                                it.text.length
                            } ?: 0
                        }

                        SceneCanvas(
                            scene = shown,
                            scale = scale,
                            reveal = shownReveal
                        )
                    }

                    if (scene.choices.isNotEmpty()) {
                        scene.choices.forEachIndexed { index, choice ->
                            val x = if (index == 0) {
                                if (scene.id == "100:655") {
                                    193f
                                } else {
                                    128f
                                }
                            } else {
                                1022f
                            }

                            val choiceHeight = if (
                                scene.id == "100:655"
                            ) {
                                184f
                            } else {
                                164f
                            }

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (x * scale).dp,
                                        y = (854f * scale).dp
                                    )
                                    .size(
                                        width = (763f * scale).dp,
                                        height = (choiceHeight * scale).dp
                                    )
                                    .clickable(
                                        onClickLabel = choice.label
                                    ) {
                                        vm.advance(index)
                                    }
                                    .semantics {
                                        contentDescription = choice.label
                                        role = Role.Button
                                    }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp)
                                .clickable(
                                    onClickLabel = "Avançar diálogo"
                                ) {
                                    next()
                                }
                        )

                        TextButton(
                            onClick = {
                                next()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = when {
                                    reveal < longest -> "Mostrar texto"
                                    scene.next == null -> "Concluir episódio"
                                    else -> "Continuar"
                                },
                                color = Color.White,
                                fontSize = 11.sp
                            )

                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Ink.copy(alpha = 0.8f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            menu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = "Pausar episódio",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = scene.title,
                        fontSize = 12.sp,
                        color = Muted
                    )

                    IconButton(
                        onClick = {
                            reading = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = "Abrir modo leitura",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (menu) {
        AlertDialog(
            onDismissRequest = {
                menu = false
            },
            containerColor = Ink,
            title = {
                Text(
                    text = "Uma pausa no multiverso",
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Seu progresso é salvo automaticamente.",
                        color = Muted
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    TextButton(
                        onClick = {
                            vm.backScene()
                            menu = false
                        },
                        enabled = progress.history.isNotEmpty() ||
                                progress.completed
                    ) {
                        Text(
                            text = "Voltar uma cena"
                        )
                    }

                    TextButton(
                        onClick = {
                            menu = false
                            reading = true
                        }
                    ) {
                        Text(
                            text = "Ler diálogo em tamanho maior"
                        )
                    }

                    TextButton(
                        onClick = {
                            menu = false
                            reset = true
                        }
                    ) {
                        Text(
                            text = "Recomeçar episódio"
                        )
                    }

                    TextButton(
                        onClick = {
                            menu = false
                            exit()
                        }
                    ) {
                        Text(
                            text = "Sair para o Nexus"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        menu = false
                    }
                ) {
                    Text(
                        text = "Continuar"
                    )
                }
            }
        )
    }

    if (reading) {
        ModalBottomSheet(
            onDismissRequest = {
                reading = false
            },
            containerColor = Ink
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = scene.title,
                    color = Pink
                )

                scene.layers
                    .filter {
                        it.kind == "text"
                    }
                    .forEach { layer ->
                        Text(
                            text = layer.text,
                            color = Color.White,
                            fontSize = if (
                                layer.font == "Bangers"
                            ) {
                                20.sp
                            } else {
                                18.sp
                            },
                            modifier = Modifier.padding(
                                vertical = 8.dp
                            )
                        )
                    }

                if (scene.choices.isEmpty()) {
                    NexusButton(
                        text = "Continuar",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reading = false
                        vm.advance()
                    }
                } else {
                    scene.choices.forEachIndexed { index, choice ->
                        NexusButton(
                            text = choice.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            secondary = true
                        ) {
                            reading = false
                            vm.advance(index)
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }

    if (reset) {
        AlertDialog(
            onDismissRequest = {
                reset = false
            },
            containerColor = Ink,
            title = {
                Text(
                    text = "Recomeçar episódio?",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Isso substitui seu percurso " +
                            "e suas escolhas atuais.",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.restart()
                        reset = false
                    }
                ) {
                    Text(
                        text = "Recomeçar"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reset = false
                    }
                ) {
                    Text(
                        text = "Cancelar"
                    )
                }
            }
        )
    }
}

@Composable
internal fun SceneCanvas(
    scene: Scene,
    scale: Float,
    reveal: Int
) {
    val mono = assetFont(
        file = "dm_mono_medium.ttf",
        fallback = FontFamily.Monospace
    )

    val bangers = assetFont(
        file = "bangers.ttf",
        fallback = FontFamily.Cursive
    )

    val fontScale = LocalDensity.current.fontScale

    Box(
        modifier = Modifier
            .size(
                width = (1920f * scale).dp,
                height = (1080f * scale).dp
            )
            .background(Ink)
    ) {
        if (scene.choices.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height((342f * scale).dp)
                    .background(
                        Ink.copy(alpha = 0.9f)
                    )
            )
        }

        scene.layers.forEach { layer ->
            val bounds = Modifier
                .offset(
                    x = (layer.x * scale).dp,
                    y = (layer.y * scale).dp
                )
                .wrapContentSize(
                    align = Alignment.TopStart,
                    unbounded = true
                )
                .requiredSize(
                    width = (layer.w * scale).dp,
                    height = (layer.h * scale).dp
                )
                .graphicsLayer {
                    rotationZ = -layer.rot
                    alpha = layer.a.coerceIn(0f, 1f)
                }

            when (layer.kind) {
                "image" -> {
                    Artwork(
                        path = layer.asset,
                        modifier = bounds,
                        scale = if (
                            layer.asset.endsWith(
                                suffix = ".svg",
                                ignoreCase = true
                            )
                        ) {
                            ContentScale.FillBounds
                        } else {
                            ContentScale.Crop
                        }
                    )
                }

                "panel" -> {
                    Box(
                        modifier = bounds
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xA0200000),
                                        Ink,
                                        Color(0xA0000028)
                                    )
                                )
                            )
                            .border(
                                width = (1.5f * scale).dp,
                                color = Pink.copy(
                                    alpha = 0.42f
                                )
                            )
                    )
                }

                "text" -> {
                    val isSpeakerName =
                        layer.font == "Bangers"

                    val visibleText = if (
                        isSpeakerName ||
                        scene.choices.isNotEmpty()
                    ) {
                        layer.text
                    } else {
                        layer.text.take(
                            reveal.coerceIn(
                                minimumValue = 0,
                                maximumValue = layer.text.length
                            )
                        )
                    }

                    val lineHeightMultiplier = if (
                        isSpeakerName
                    ) {
                        1.06f
                    } else {
                        1.295f
                    }

                    Text(
                        text = visibleText,
                        modifier = bounds.clearAndSetSemantics {
                            contentDescription = layer.text
                        },
                        color = Color.White,
                        fontFamily = if (isSpeakerName) {
                            bangers
                        } else {
                            mono
                        },
                        fontSize = (
                                layer.size * scale / fontScale
                                ).sp,
                        letterSpacing = (
                                if (isSpeakerName) {
                                    1.44f * scale / fontScale
                                } else {
                                    0f
                                }
                                ).sp,
                        lineHeight = (
                                layer.size *
                                        lineHeightMultiplier *
                                        scale /
                                        fontScale
                                ).sp,
                        textAlign = if (layer.center) {
                            TextAlign.Center
                        } else {
                            TextAlign.Start
                        },
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
            }
        }
    }
}
