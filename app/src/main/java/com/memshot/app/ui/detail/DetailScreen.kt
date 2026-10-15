package com.memshot.app.ui.detail

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memshot.app.analysis.Categories
import com.memshot.app.analysis.Entities
import com.memshot.app.analysis.Entity
import com.memshot.app.analysis.EntityType
import com.memshot.app.analysis.normalizeForSearch
import com.memshot.app.data.Shot
import com.memshot.app.data.ShotStatus
import com.memshot.app.ui.MainViewModel
import com.memshot.app.ui.components.LocalNavScope
import com.memshot.app.ui.components.TagChip
import com.memshot.app.ui.components.bouncyClick
import com.memshot.app.ui.components.tagIcon
import com.memshot.app.ui.components.sharedShot
import com.memshot.app.ui.components.shareFiles
import com.memshot.app.ui.components.shimmer
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMMM yyyy · HH:mm", Locale.ITALIAN)

/**
 * Dettaglio a pagine: scorri tra gli screenshot della lista da cui sei arrivato,
 * in orizzontale come una galleria oppure in verticale come TikTok.
 */
@Composable
fun DetailScreen(
    vm: MainViewModel,
    ids: List<Long>,
    startId: Long,
    initial: (Long) -> Shot?,
    vertical: Boolean,
    onBack: (Long) -> Unit,
) {
    val pageIds = remember { ids.ifEmpty { listOf(startId) }.toMutableStateList() }
    val pager = rememberPagerState(initialPage = pageIds.indexOf(startId).coerceAtLeast(0)) { pageIds.size }
    val currentId = pageIds.getOrNull(pager.currentPage) ?: startId
    val shot by remember(currentId) { vm.shot(currentId) }.collectAsStateWithLifecycle(initial(currentId))
    val query by vm.query.collectAsStateWithLifecycle()
    var viewer by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addTag by remember { mutableStateOf(false) }
    var infoSheet by remember { mutableStateOf(false) }

    BackHandler(!viewer) { onBack(currentId) }
    BackHandler(viewer) { viewer = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (vertical) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (vertical) {
            LightStatusBarIcons()
            VerticalPager(
                state = pager,
                key = { pageIds.getOrElse(it) { -it.toLong() } },
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val id = pageIds.getOrNull(page) ?: return@VerticalPager
                TikTokPage(
                    vm = vm,
                    id = id,
                    initial = initial(id),
                    onAddTag = { addTag = true },
                    onInfo = { infoSheet = true },
                    onDelete = { confirmDelete = true },
                )
            }
        } else {
            HorizontalPager(
                state = pager,
                key = { pageIds.getOrElse(it) { -it.toLong() } },
                beyondViewportPageCount = 1,
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val id = pageIds.getOrNull(page) ?: return@HorizontalPager
                GalleryPage(
                    vm = vm,
                    id = id,
                    initial = initial(id),
                    query = query,
                    onAddTag = { addTag = true },
                    onOpenViewer = { viewer = true },
                )
            }
        }

        shot?.let { s ->
            TopBar(
                shot = s,
                position = if (pageIds.size > 1) "${pager.currentPage + 1} / ${pageIds.size}" else null,
                onDark = vertical,
                onBack = { onBack(currentId) },
                onFavorite = { vm.toggleFavorite(s.id) },
                onReanalyze = { vm.reanalyze(s.id) },
                onDelete = { confirmDelete = true },
            )
        }

        AnimatedVisibility(viewer, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            shot?.let { FullscreenViewer(it, onClose = { viewer = false }) }
        }
    }

    val s = shot ?: return

    if (infoSheet) {
        ModalBottomSheet(
            onDismissRequest = { infoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ShotInfo(s, query, onRemoveTag = { vm.removeTag(s.id, it) }, onAddTag = { addTag = true })
            }
        }
    }

    if (addTag) AddTagDialog(
        current = s.tags,
        onDismiss = { addTag = false },
        onAdd = { vm.addTag(s.id, it); addTag = false },
    )

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Eliminare lo screenshot?") },
        text = { Text("Verrà eliminato da MemShot.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                vm.delete(listOf(s.id))
                if (pageIds.size <= 1) {
                    onBack(s.id)
                } else {
                    pageIds.remove(s.id)
                }
            }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
    )
}

/** Data, tag, azioni rapide e testo riconosciuto: usato sia nella pagina galleria sia nel pannello TikTok. */
@Composable
private fun ShotInfo(s: Shot, query: String, onRemoveTag: (String) -> Unit, onAddTag: () -> Unit) {
    Meta(s)
    TagsSection(shot = s, onRemove = onRemoveTag, onAdd = onAddTag)
    if (s.status == ShotStatus.PENDING) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .shimmer()
                .padding(18.dp)
        ) {
            Text(
                "Sto leggendo lo screenshot e scegliendo i tag…",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        val entities = remember(s.ocrText) { Entities.extract(s.ocrText) }
        if (entities.isNotEmpty()) QuickActions(entities)
        RecognizedText(s.ocrText, query)
    }
}

@Composable
private fun GalleryPage(
    vm: MainViewModel,
    id: Long,
    initial: Shot?,
    query: String,
    onAddTag: () -> Unit,
    onOpenViewer: () -> Unit,
) {
    val shot by remember(id) { vm.shot(id) }.collectAsStateWithLifecycle(initial)
    val s = shot ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 64.dp)
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(s.filePath))
                    .placeholderMemoryCacheKey(s.filePath)
                    .memoryCacheKey(s.filePath + "#full")
                    .build(),
                contentDescription = "Screenshot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .aspectRatio(s.aspectRatio, matchHeightConstraintsFirst = true)
                    .sharedShot(s.id, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenViewer),
            )
        }
        Column(
            Modifier
                .enterFromBelow()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ShotInfo(s, query, onRemoveTag = { vm.removeTag(s.id, it) }, onAddTag = onAddTag)
        }
    }
}

/** Pagina a tutto schermo in stile TikTok: immagine intera, info in basso, azioni a destra. */
@Composable
private fun TikTokPage(
    vm: MainViewModel,
    id: Long,
    initial: Shot?,
    onAddTag: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    val shot by remember(id) { vm.shot(id) }.collectAsStateWithLifecycle(initial)
    val s = shot ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(0f) }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(s.filePath))
                .placeholderMemoryCacheKey(s.filePath)
                .memoryCacheKey(s.filePath + "#full")
                .build(),
            contentDescription = "Screenshot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp, bottom = 150.dp)
                .padding(horizontal = 12.dp)
                .sharedShot(s.id, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(s.id) {
                    detectTapGestures(
                        onTap = { onInfo() },
                        // doppio tocco = preferito, come il "mi piace" di TikTok
                        onDoubleTap = {
                            if (!s.favorite) vm.toggleFavorite(s.id)
                            scope.launch {
                                burst.snapTo(0.01f)
                                burst.animateTo(1f, tween(650))
                                burst.snapTo(0f)
                            }
                        },
                    )
                },
        )

        if (burst.value > 0f) {
            Icon(
                Icons.Rounded.Star, null, tint = Color(0xFFFFC53D),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(140.dp)
                    .graphicsLayer {
                        val v = burst.value
                        val sc = if (v < 0.3f) v / 0.3f * 1.2f else 1.2f - (v - 0.3f) * 0.3f
                        scaleX = sc; scaleY = sc
                        alpha = if (v < 0.7f) 1f else (1f - v) / 0.3f
                    },
            )
        }

        // Info in basso a sinistra
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 88.dp, top = 40.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(dateFormat.format(s.createdAt), color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (s.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.tags.forEach { tag ->
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(Color(Categories.color(tag)).copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(tagIcon(tag), null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(Categories.label(tag), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            val excerpt = s.ocrText.lines().filter { it.isNotBlank() }.drop(1).joinToString(" ")
            if (excerpt.isNotBlank()) {
                Text(
                    excerpt,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onInfo),
                )
            }
        }

        // Azioni a destra
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SideAction(
                if (s.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                "Preferito",
                tint = if (s.favorite) Color(0xFFFFC53D) else Color.White,
            ) { vm.toggleFavorite(s.id) }
            SideAction(Icons.Rounded.Add, "Tag") { onAddTag() }
            SideAction(Icons.AutoMirrored.Rounded.Notes, "Testo") { onInfo() }
            SideAction(Icons.Rounded.Share, "Condividi") { shareFiles(context, listOf(File(s.filePath))) }
            SideAction(Icons.Rounded.DeleteOutline, "Elimina") { onDelete() }
        }
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bouncyClick(pressedScale = 0.8f, onClick = onClick)) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 8f),
            ),
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** Icone della barra di stato chiare finché siamo sullo sfondo nero della modalità TikTok. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        val beforeNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = before
            controller.isAppearanceLightNavigationBars = beforeNav
        }
    }
}

/** Il contenuto sale dal basso mentre l'immagine "vola" in posizione. */
@Composable
private fun Modifier.enterFromBelow(): Modifier {
    val scope: AnimatedVisibilityScope = LocalNavScope.current ?: return this
    val progress by scope.transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 300f) }, label = "enter",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 120f * density
    }
}

@Composable
private fun TopBar(
    shot: Shot,
    position: String?,
    onDark: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onReanalyze: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val starScale = remember { Animatable(1f) }
    LaunchedEffect(shot.favorite) {
        starScale.snapTo(0.6f)
        starScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", onBack, onDark = onDark)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (position != null) {
                AnimatedContent(position, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "pos") { p ->
                    Text(
                        p,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (onDark) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (!onDark) {
            RoundIcon(
                if (shot.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                "Preferito",
                onFavorite,
                tint = if (shot.favorite) Color(0xFFFFB224) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { scaleX = starScale.value; scaleY = starScale.value },
            )
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Share, "Condividi", { shareFiles(context, listOf(File(shot.filePath))) })
            Spacer(Modifier.width(8.dp))
        }
        Box {
            RoundIcon(Icons.Rounded.MoreVert, "Altro", { menu = true }, onDark = onDark)
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem(
                    text = { Text("Rianalizza") },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                    onClick = { menu = false; onReanalyze() },
                )
                DropdownMenuItem(
                    text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun RoundIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    tint: Color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (onDark) Color.White.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
            )
            .bouncyClick(pressedScale = 0.85f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint) }
}

@Composable
private fun Meta(shot: Shot) {
    Column {
        Text(dateFormat.format(shot.createdAt), style = MaterialTheme.typography.titleLarge)
        val app = shot.sourceApp?.substringBefore(' ')?.replaceFirstChar { it.uppercase() }
        val words = shot.ocrText.split(Regex("\\s+")).count { it.isNotBlank() }
        Text(
            listOfNotNull(app?.let { "da $it" }, if (words > 0) "$words parole riconosciute" else null)
                .joinToString(" · ")
                .ifEmpty { "Screenshot" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun TagsSection(shot: Shot, onRemove: (String) -> Unit, onAdd: () -> Unit) {
    Column {
        SectionTitle("Tag")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shot.tags.forEach { tag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(tag = tag, selected = true, onClick = { onRemove(tag) })
                }
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .bouncyClick(onClick = onAdd)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Aggiungi", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (shot.tags.isNotEmpty()) {
            Text(
                "Tocca un tag per toglierlo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun QuickActions(entities: List<Entity>) {
    val context = LocalContext.current
    Column {
        SectionTitle("Azioni rapide")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entities.forEach { e ->
                val (icon, intent) = when (e.type) {
                    EntityType.LINK -> Icons.Rounded.Link to Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(if (e.value.startsWith("http", true)) e.value else "https://${e.value}"),
                    )
                    EntityType.EMAIL -> Icons.Rounded.Email to Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${e.value}"))
                    EntityType.PHONE -> Icons.Rounded.Call to Intent(Intent.ACTION_DIAL, Uri.parse("tel:${e.value.filter { it.isDigit() || it == '+' }}"))
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .bouncyClick {
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "Nessuna app per aprirlo", Toast.LENGTH_SHORT).show() }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        e.value.removePrefix("https://").removePrefix("http://").take(32),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizedText(text: String, query: String) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column {
        SectionTitle("Testo riconosciuto") {
            if (text.isNotBlank()) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .bouncyClick {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("MemShot", text)))
                                Toast.makeText(context, "Testo copiato", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Copia tutto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(18.dp)
        ) {
            if (text.isBlank()) {
                Text(
                    "Nessun testo in questa immagine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val highlight = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                val annotated = remember(text, query) { highlight(text, query, highlight) }
                SelectionContainer {
                    Text(annotated, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Evidenzia nel testo le parole cercate (ignorando maiuscole e accenti). */
private fun highlight(text: String, query: String, color: Color): AnnotatedString {
    val tokens = query.normalizeForSearch().split(' ').filter { it.length >= 2 }
    if (tokens.isEmpty()) return AnnotatedString(text)
    // la normalizzazione toglie solo gli accenti, quindi le posizioni restano allineate carattere per carattere
    val plain = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    if (plain.length != text.length) return AnnotatedString(text)
    val ranges = tokens.flatMap { t ->
        generateSequence(plain.indexOf(t)) { i -> plain.indexOf(t, i + 1).takeIf { it >= 0 } }
            .takeWhile { it >= 0 }
            .map { it until it + t.length }
            .toList()
    }.sortedBy { it.first }
    return buildAnnotatedString {
        var pos = 0
        for (r in ranges) {
            if (r.first < pos) continue
            append(text.substring(pos, r.first))
            withStyle(SpanStyle(background = color, fontWeight = FontWeight.Bold)) { append(text.substring(r.first, r.last + 1)) }
            pos = r.last + 1
        }
        append(text.substring(pos))
    }
}

@Composable
private fun AddTagDialog(current: List<String>, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi un tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("es. vacanze, idee regalo…") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Oppure scegli una categoria", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Categories.all.filter { it.id !in current && it.id != Categories.OTHER }.forEach { c ->
                        TagChip(tag = c.id, compact = true, onClick = { onAdd(c.id) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Visualizzatore a schermo intero: pizzica per zoomare, doppio tocco per ingrandire. */
@Composable
private fun FullscreenViewer(shot: Shot, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (scale.value <= 1.01f) onClose() },
                    onDoubleTap = { tap ->
                        scope.launch {
                            if (scale.value > 1.01f) {
                                launch { scale.animateTo(1f, spring()) }
                                offset.animateTo(Offset.Zero, spring())
                            } else {
                                val target = 2.5f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                launch { scale.animateTo(target, spring()) }
                                offset.animateTo((center - tap) * (target - 1f), spring())
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scope.launch {
                        val s = (scale.value * zoom).coerceIn(1f, 6f)
                        scale.snapTo(s)
                        val maxX = size.width * (s - 1f) / 2f
                        val maxY = size.height * (s - 1f) / 2f
                        val o = offset.value + pan
                        offset.snapTo(Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY)))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(shot.filePath))
                .placeholderMemoryCacheKey(shot.filePath + "#full")
                .build(),
            contentDescription = "Screenshot a schermo intero",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y
                },
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .bouncyClick(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, "Chiudi", tint = Color.White) }
    }
}
