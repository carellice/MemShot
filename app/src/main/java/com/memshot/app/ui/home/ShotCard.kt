package com.memshot.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import com.memshot.app.ui.components.tagIcon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memshot.app.analysis.Categories
import com.memshot.app.data.Shot
import com.memshot.app.data.ShotStatus
import com.memshot.app.ui.components.sharedShot
import com.memshot.app.ui.components.shimmer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** L'animazione d'ingresso a cascata parte solo la prima volta che la griglia appare. */
private object Intro {
    var played = false
    val start = System.currentTimeMillis()
}

private val CardShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShotCard(
    shot: Shot,
    index: Int,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        when {
            selected -> 0.9f
            pressed -> 0.96f
            else -> 1f
        },
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "cardScale",
    )

    // Ingresso a cascata: le prime card salgono e compaiono una dopo l'altra.
    val intro = remember {
        val animate = !Intro.played && index < 14 && System.currentTimeMillis() - Intro.start < 4000
        Animatable(if (animate) 0f else 1f)
    }
    LaunchedEffect(Unit) {
        if (intro.value < 1f) {
            kotlinx.coroutines.delay(60L * index)
            intro.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 220f))
            Intro.played = true
        }
    }

    val ratio = shot.aspectRatio.coerceIn(0.46f, 1.4f)
    val pending = shot.status == ShotStatus.PENDING

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = intro.value
                translationY = (1f - intro.value) * 80f
            }
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CardShape) else Modifier
            )
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(shot.filePath))
                .memoryCacheKey(shot.filePath)
                .crossfade(250)
                .build(),
            contentDescription = shot.ocrText.take(80).ifBlank { "Screenshot" },
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .sharedShot(shot.id)
                .shimmer(pending),
        )

        // Velatura in basso con i tag
        if (shot.tags.isNotEmpty()) {
            FlowRow(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
                    .padding(start = 8.dp, end = 8.dp, top = 28.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxLines = 1,
            ) {
                shot.tags.take(2).forEach { tag -> OverlayTag(tag) }
            }
        }

        if (pending) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Analisi…", color = Color.White, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (shot.favorite && !selecting) {
            Icon(
                Icons.Rounded.Star, contentDescription = "Preferito", tint = Color(0xFFFFC53D),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(4.dp)
                    .size(16.dp),
            )
        }

        AnimatedVisibility(
            visible = selecting,
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.4f)), exit = scaleOut(tween(120))) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OverlayTag(tag: String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color(Categories.color(tag)).copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tagIcon(tag), null, tint = Color.White, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            Categories.label(tag), color = Color.White, fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall, maxLines = 1,
        )
    }
}

private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)

/** Raggruppa per data con etichette "umane": Oggi, Ieri, Questa settimana… */
fun groupByDate(shots: List<Shot>): List<Pair<String, List<Shot>>> {
    val now = Calendar.getInstance()
    fun startOfDay(daysAgo: Int) = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysAgo)
    }.timeInMillis
    val today = startOfDay(0)
    val yesterday = startOfDay(1)
    val week = startOfDay(6)
    val month = startOfDay(29)
    return shots.groupBy { s ->
        when {
            s.createdAt >= today -> "Oggi"
            s.createdAt >= yesterday -> "Ieri"
            s.createdAt >= week -> "Questa settimana"
            s.createdAt >= month -> "Questo mese"
            else -> monthFormat.format(s.createdAt).replaceFirstChar { it.titlecase(Locale.ITALIAN) }
        }
    }.toList()
}
