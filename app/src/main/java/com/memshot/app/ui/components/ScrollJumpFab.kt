package com.memshot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Pulsante che porta in cima all'elenco; quando sei già in cima, la freccia ruota e ti porta in fondo.
 * Compare solo se l'elenco è abbastanza lungo da scorrere.
 */
@Composable
fun ScrollJumpFab(state: LazyStaggeredGridState, visible: Boolean = true, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val scrollable by remember { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    val atTop by remember { derivedStateOf { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < 80 } }
    val rotation by animateFloatAsState(
        if (atTop) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "jumpArrow",
    )
    AnimatedVisibility(
        visible = visible && scrollable,
        enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    if (atTop) state.animateScrollToItem((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                    else state.animateScrollToItem(0)
                }
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = if (atTop) "Vai in fondo" else "Torna in cima",
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}
