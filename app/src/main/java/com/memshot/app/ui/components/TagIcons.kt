package com.memshot.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.memshot.app.analysis.Categories
import com.memshot.app.ui.theme.BrandGradient

const val TAG_ALL = "__all"
const val TAG_FAVORITES = "__fav"
const val TAG_NEW = "__new"

/** Icona di ogni categoria (e dei filtri speciali). I tag personalizzati usano il cancelletto. */
fun tagIcon(tag: String): ImageVector = when (tag) {
    "film" -> Icons.Rounded.Movie
    "prompt" -> Icons.Rounded.AutoAwesome
    "code" -> Icons.Rounded.Code
    "recipe" -> Icons.Rounded.RestaurantMenu
    "shopping" -> Icons.Rounded.ShoppingBag
    "chat" -> Icons.Rounded.Forum
    "music" -> Icons.Rounded.MusicNote
    "books" -> Icons.AutoMirrored.Rounded.MenuBook
    "travel" -> Icons.Rounded.Flight
    "events" -> Icons.Rounded.ConfirmationNumber
    "finance" -> Icons.Rounded.AccountBalanceWallet
    "places" -> Icons.Rounded.Place
    "social" -> Icons.Rounded.Groups
    "news" -> Icons.Rounded.Newspaper
    "quotes" -> Icons.Rounded.FormatQuote
    "study" -> Icons.Rounded.School
    "work" -> Icons.Rounded.Work
    "health" -> Icons.Rounded.FitnessCenter
    "sport" -> Icons.Rounded.SportsSoccer
    "games" -> Icons.Rounded.SportsEsports
    Categories.PHOTO -> Icons.Rounded.Image
    Categories.OTHER -> Icons.Rounded.Category
    TAG_ALL -> Icons.Rounded.PhotoLibrary
    TAG_FAVORITES -> Icons.Rounded.Star
    TAG_NEW -> Icons.Rounded.Add
    else -> Icons.Rounded.Tag
}

fun tagColor(tag: String): Color = when (tag) {
    TAG_FAVORITES -> Color(0xFFFFB224)
    else -> Color(Categories.color(tag))
}

fun tagLabel(tag: String): String = when (tag) {
    TAG_FAVORITES -> "Preferiti"
    TAG_ALL -> "Tutti"
    else -> Categories.label(tag)
}

/** Riquadro colorato con l'icona della categoria. */
@Composable
fun TagBadge(tag: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val color = tagColor(tag)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) { Icon(tagIcon(tag), null, tint = color, modifier = Modifier.size(size * 0.55f)) }
}

/** Icona in un cerchio col gradiente del brand che fluttua piano: per stati vuoti e momenti "wow". */
@Composable
fun FloatingIcon(icon: ImageVector, size: Dp = 88.dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    val r by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    Box(
        modifier
            .graphicsLayer { translationY = y * density; rotationZ = r }
            .size(size)
            .clip(CircleShape)
            .background(BrandGradient),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.5f)) }
}
