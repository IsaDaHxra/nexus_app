package com.nexus.mobile.ui

import android.graphics.Typeface
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.mobile.data.Character

val Ink = Color(0xFF090A10)
val Muted = Color(0xFFB8B7C4)
val Pink = Color(0xFFFF648C)
val Cyan = Color(0xFF76DBF5)
val Glass = Color(0xFF272732)
val Paper = Color(0xFFF5F5F7)
val CardShape = RoundedCornerShape(28.dp)
val LocalMotionEnabled = staticCompositionLocalOf { true }

private object ArtResources {
    private var imageLoader: ImageLoader? = null
    private var aliases: Map<String, String>? = null

    private fun normalize(path: String): String {
        return path.trim()
            .replace('\\', '/')
            .removePrefix("file:///android_asset/")
            .removePrefix("android_asset/")
            .removePrefix("app/src/main/assets/")
            .removePrefix("assets/")
            .trimStart('/')
    }

    fun exists(
        context: android.content.Context,
        path: String
    ): Boolean {
        val normalized = normalize(path)

        if (normalized.isBlank()) return false

        return runCatching {
            context.assets.open(normalized).use { }
        }.isSuccess
    }

    fun loader(context: android.content.Context): ImageLoader {
        return imageLoader ?: ImageLoader.Builder(context.applicationContext)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(180)
            .build()
            .also {
                imageLoader = it
            }
    }

    private fun mappings(
        context: android.content.Context
    ): Map<String, String> {
        aliases?.let { return it }

        val loaded = runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type

            context.assets.open("asset_aliases.json")
                .bufferedReader()
                .use { reader ->
                    Gson().fromJson<Map<String, String>>(
                        reader.readText(),
                        type
                    ) ?: emptyMap()
                }
        }.getOrDefault(emptyMap())

        return loaded.also {
            aliases = it
        }
    }
    fun resolve(
        context: android.content.Context,
        path: String
    ): String {
        val original = path.trim()

        if (
            original.startsWith("https://") ||
            original.startsWith("http://")
        ) {
            return original
        }

        val normalized = normalize(original)
        val mapping = mappings(context)
        val alias = mapping[original] ?: mapping[normalized]

        if (!alias.isNullOrBlank()) {
            val destination = alias.trim()

            if (
                destination.startsWith("https://") ||
                destination.startsWith("http://")
            ) {
                return destination
            }
            val normalizedAlias = normalize(destination)
            if (exists(context, normalizedAlias)) {
                return normalizedAlias
            }
        }

        if (exists(context, normalized)) {
            return normalized
        }

        Log.e(
            "NexusAssets",
            "Imagem ausente em assets. Original: $normalized; alias: $alias"
        )

        return normalized
    }
}

@Composable fun characterArt(c: Character, large: Boolean = true): String {
    val context = LocalContext.current
    return remember(c, large) {
        if (c.image.startsWith("https://")) c.portrait.ifBlank { c.image }
        else {
            val options = if (large) listOf("images/catalog_${c.id}.png", c.portrait, if (c.id == "peter") "images/episode_latest.png" else c.image, c.image) else listOf(c.image, c.portrait)
            options.firstOrNull { ArtResources.exists(context, ArtResources.resolve(context, it)) }
                ?: if (c.id == "peter") "images/episode_latest.png" else c.image
        }
    }
}

@Composable fun assetFont(file: String, fallback: FontFamily = FontFamily.SansSerif): FontFamily {
    val context = LocalContext.current
    return remember(file) { runCatching { FontFamily(Typeface.createFromAsset(context.assets, "fonts/$file")) }.getOrDefault(fallback) }
}

@Composable fun NexusTheme(content: @Composable () -> Unit) {
    val font = assetFont("dm_sans.ttf")
    val defaults = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Color.White, onPrimary = Ink, primaryContainer = Glass,
            onPrimaryContainer = Color.White, background = Ink, surface = Ink, onSurface = Color.White,
            onSurfaceVariant = Muted, secondary = Pink, onSecondary = Ink, outline = Color(0xFF62616F)),
        typography = Typography(
            displayLarge = defaults.displayLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            headlineLarge = defaults.headlineLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            headlineMedium = defaults.headlineMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
            bodyLarge = defaults.bodyLarge.copy(fontFamily = font), bodyMedium = defaults.bodyMedium.copy(fontFamily = font),
            bodySmall = defaults.bodySmall.copy(fontFamily = font), titleLarge = defaults.titleLarge.copy(fontFamily = font),
            titleMedium = defaults.titleMedium.copy(fontFamily = font), labelLarge = defaults.labelLarge.copy(fontFamily = font),
            labelMedium = defaults.labelMedium.copy(fontFamily = font), labelSmall = defaults.labelSmall.copy(fontFamily = font)
        )
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(color = Color.Unspecified),
            LocalContentColor provides Color.White, content = content)
    }
}

@Composable
fun Artwork(
    path: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    scale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val motion = LocalMotionEnabled.current

    val loader = remember(context) {
        ArtResources.loader(context)
    }

    val resolved = remember(context, path) {
        ArtResources.resolve(context, path)
    }

    val request = remember(context, resolved, motion) {
        val source = if (
            resolved.startsWith("https://") ||
            resolved.startsWith("http://")
        ) {
            resolved
        } else {
            android.net.Uri.parse("file:///android_asset/")
                .buildUpon()
                .appendEncodedPath(android.net.Uri.encode(resolved, "/"))
                .build()
        }

        coil.request.ImageRequest.Builder(context)
            .data(source)
            .crossfade(if (motion) 180 else 0)
            .build()
    }

    AsyncImage(
        model = request,
        imageLoader = loader,
        contentDescription = description,
        contentScale = scale,
        modifier = modifier,
        onError = {
            Log.e(
                "NexusAssets",
                "Erro carregando imagem. Solicitada: $path; utilizada: $resolved",
                it.result.throwable
            )
        }
    )
}

@Composable fun AmbientBackground(content: @Composable BoxScope.() -> Unit) {
    val phase = rememberMotionPhase()
    Box(Modifier.fillMaxSize().background(Ink)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val p = phase.value
            drawRect(Brush.radialGradient(listOf(Color(0xFF332044).copy(.65f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * (.75f + .12f * kotlin.math.sin(p)), size.height * .3f), radius = size.width * 1.1f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF6C273D).copy(.28f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * .1f, size.height * (.65f + .08f * kotlin.math.cos(p))), radius = size.width * .95f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF174554).copy(.24f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width, size.height), radius = size.width * 1.3f))
        }
        content()
    }
}

@Composable fun GlassPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(CardShape)
        .background(Brush.linearGradient(listOf(Color(0xE52E2B39), Color(0xD0181923))))
        .border(.7.dp, Brush.linearGradient(listOf(Color.White.copy(.25f), Color.White.copy(.04f))), CardShape), content = content)
}

@Composable fun Header(onSearch: () -> Unit, onProfile: () -> Unit, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar", tint = Color.White) }
        Row(Modifier.weight(1f).height(48.dp).clip(CircleShape)
            .background(Ink.copy(.64f)).border(.6.dp, Color.White.copy(.22f), CircleShape)
            .clickable(onClick = onSearch).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(10.dp)); Text("Pesquisar personagens", fontSize = 12.sp, color = Muted, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(48.dp).clip(CircleShape).background(Glass).border(.6.dp, Color.White.copy(.25f), CircleShape)
            .clickable(onClickLabel = "Meu Nexus", onClick = onProfile), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Person, "Meu Nexus", Modifier.size(22.dp))
            Artwork("images/avatar.png", Modifier.fillMaxSize())
        }
    }
}

@Composable fun Eyebrow(text: String, color: Color = Muted) {
    Text(text.uppercase(), color = color, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
}

@Composable fun Heading(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), fontSize = 25.sp, lineHeight = 29.sp, letterSpacing = (-.7).sp, fontWeight = FontWeight.SemiBold)
        if (action != null) TextButton(onClick = onAction) { Text(action, color = Muted, fontSize = 12.sp) }
    }
}

@Composable fun NexusButton(text: String, modifier: Modifier = Modifier, secondary: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale by animateFloatAsState(if (pressed && motion) .97f else 1f, tween(if (motion) 140 else 0), label = "Pressão do botão")
    val m = modifier.heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale }
    if (secondary) OutlinedButton(onClick, m, interactionSource = source, shape = CircleShape,
        border = BorderStroke(.7.dp, Color.White.copy(.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    } else Button(onClick, m, interactionSource = source, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink)) {
        Text(text, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable fun CharacterRow(character: Character, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.heightIn(min = 84.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(characterArt(character), Modifier.size(62.dp).clip(RoundedCornerShape(18.dp)), character.name)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(character.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(character.role, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable fun Notice(text: String, action: String? = null, onAction: () -> Unit = {}) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(text, fontSize = 14.sp, lineHeight = 22.sp, color = Muted)
            if (action != null) TextButton(onClick = onAction) { Text(action, color = Color.White) }
        }
    }
}
