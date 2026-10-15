package com.memshot.app.ui.categories

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memshot.app.analysis.Categories
import com.memshot.app.data.Shot
import com.memshot.app.ui.FILTER_FAVORITES
import com.memshot.app.ui.MainViewModel
import com.memshot.app.ui.home.ShotGrid
import com.memshot.app.ui.home.TriageBanner
import com.memshot.app.ui.components.bouncyClick
import com.memshot.app.ui.components.FloatingIcon
import com.memshot.app.ui.components.TagBadge
import com.memshot.app.ui.components.ScrollJumpFab
import androidx.compose.foundation.layout.navigationBarsPadding
import com.memshot.app.ui.components.tagLabel
import androidx.compose.material.icons.rounded.Celebration
import com.memshot.app.ui.theme.Amber
import kotlinx.coroutines.delay
import java.io.File

/** Gli screenshot di una categoria (o dei preferiti), dal più recente. */
fun shotsIn(all: List<Shot>, tag: String): List<Shot> =
    if (tag == FILTER_FAVORITES) all.filter { it.favorite } else all.filter { tag in it.tags }


@Composable
private fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit, tag: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro") }
        Spacer(Modifier.width(4.dp))
        if (tag != null) {
            TagBadge(tag, 40.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Elenco di tutte le categorie, con quanti screenshot contengono e un'anteprima. */
@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state by vm.home.collectAsStateWithLifecycle()
    val all by vm.all.collectAsStateWithLifecycle()
    val entries = buildList {
        if (state.favorites > 0) add(FILTER_FAVORITES to state.favorites)
        state.tags.forEach { add(it.tag to it.count) }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Categorie", "${state.tags.size} categorie · ${state.total} screenshot", onBack)
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries, key = { it.first }) { (tag, count) ->
                val index = entries.indexOfFirst { it.first == tag }
                CategoryRow(
                    tag = tag,
                    count = count,
                    previews = remember(all, tag) { shotsIn(all, tag).take(3) },
                    index = index,
                    onClick = { onOpen(tag) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    tag: String,
    count: Int,
    previews: List<Shot>,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(35L * index.coerceAtMost(12))
        intro.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 260f))
    }
    Row(
        modifier
            .graphicsLayer {
                alpha = intro.value
                translationY = (1f - intro.value) * 40f * density
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .bouncyClick(pressedScale = 0.97f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagBadge(tag, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tagLabel(tag), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                if (tag == Categories.OTHER) "$count da sistemare" else if (count == 1) "1 screenshot" else "$count screenshot",
                style = MaterialTheme.typography.bodyMedium,
                color = if (tag == Categories.OTHER) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PreviewStack(previews)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tre miniature sovrapposte a ventaglio. */
@Composable
private fun PreviewStack(shots: List<Shot>) {
    val context = LocalContext.current
    Box(Modifier.size(width = 84.dp, height = 60.dp), contentAlignment = Alignment.CenterEnd) {
        shots.reversed().forEachIndexed { i, shot ->
            val pos = shots.size - 1 - i
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(shot.filePath)).memoryCacheKey(shot.filePath).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .offset(x = (-pos * 18).dp)
                    .graphicsLayer { rotationZ = (pos - 1) * 6f }
                    .size(width = 36.dp, height = 56.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

/** Una categoria: tutti i suoi screenshot in ordine, e per "Altro" la scorciatoia per riordinarli. */
@Composable
fun CategoryScreen(
    vm: MainViewModel,
    tag: String,
    gridState: LazyStaggeredGridState,
    focusId: Long?,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onTriage: () -> Unit,
) {
    val all by vm.all.collectAsStateWithLifecycle()
    val shots = remember(all, tag) { shotsIn(all, tag) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(
            title = tagLabel(tag),
            subtitle = if (shots.size == 1) "1 screenshot" else "${shots.size} screenshot",
            onBack = onBack,
            tag = tag,
        )
        if (tag == Categories.OTHER && shots.isNotEmpty()) {
            TriageBanner(shots.size, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = onTriage)
        }
        if (shots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingIcon(Icons.Rounded.Celebration, 72.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Qui non c'è più niente", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                ShotGrid(
                    shots = shots,
                    gridState = gridState,
                    focusId = focusId,
                    selection = emptySet(),
                    onClick = { onOpen(it.id) },
                    onLongClick = { onOpen(it.id) },
                )
                ScrollJumpFab(
                    gridState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}
