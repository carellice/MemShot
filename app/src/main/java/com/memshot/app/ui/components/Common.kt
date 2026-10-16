package com.memshot.app.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memshot.app.analysis.Categories

val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Collega lo stesso screenshot tra griglia e dettaglio: l'immagine "vola" da una schermata all'altra. */
@Composable
fun Modifier.sharedShot(
    id: Long,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val shared = LocalSharedScope.current ?: return this
    val nav = LocalNavScope.current ?: return this
    return with(shared) {
        this@sharedShot.sharedElement(
            sharedContentState = rememberSharedContentState("shot-$id"),
            animatedVisibilityScope = nav,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.85f, stiffness = 380f) },
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

/** Vero mentre l'immagine sta "volando" tra griglia e dettaglio. */
@Composable
fun isSharedTransitionRunning(): Boolean = LocalSharedScope.current?.isTransitionActive == true

/** Effetto "pressione" morbido su qualsiasi elemento cliccabile. */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Riflesso luminoso che scorre: usato mentre uno screenshot è in analisi. */
fun Modifier.shimmer(active: Boolean = true): Modifier = composed {
    if (!active) return@composed this
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x",
    )
    drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = Offset(w * x - w * 0.5f, 0f),
                end = Offset(w * x + w * 0.5f, size.height),
            )
        )
    }
}

@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    count: Int? = null,
    compact: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector = tagIcon(tag),
    label: String = tagLabel(tag),
    color: Color = tagColor(tag),
    onClick: (() -> Unit)? = null,
) {
    val tagColor = color
    val bg by animateColorAsState(
        if (selected) tagColor else tagColor.copy(alpha = 0.14f), tween(250), label = "chipBg",
    )
    val fg by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurface, tween(250), label = "chipFg",
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.bouncyClick(onClick = onClick) else Modifier)
            .background(bg)
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.material3.Icon(
            icon, null,
            tint = if (selected) Color.White else tagColor,
            modifier = androidx.compose.ui.Modifier.size(if (compact) 14.dp else 18.dp),
        )
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.6f),
            )
        }
    }
}
