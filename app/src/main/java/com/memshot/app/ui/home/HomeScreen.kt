package com.memshot.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memshot.app.ui.FILTER_FAVORITES
import com.memshot.app.ui.HomeState
import com.memshot.app.ui.MainViewModel
import com.memshot.app.analysis.Categories
import com.memshot.app.ui.components.TagChip
import com.memshot.app.ui.components.ScrollJumpFab
import com.memshot.app.ui.components.TAG_ALL
import com.memshot.app.ui.components.TAG_FAVORITES
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.SearchOff
import com.memshot.app.ui.components.bouncyClick
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import com.memshot.app.ui.components.shareFiles
import com.memshot.app.ui.components.shimmer
import com.memshot.app.ui.theme.Amber
import com.memshot.app.ui.theme.BrandGradient
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    vm: MainViewModel,
    gridState: LazyStaggeredGridState,
    focusId: Long?,
    onOpen: (Long) -> Unit,
    onOpenCategories: () -> Unit,
    onTriage: (String) -> Unit,
) {
    val state by vm.home.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val pending by vm.pendingCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selecting = selection.isNotEmpty()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) vm.importUris(uris) { n ->
            scope.launch {
                snackbar.showSnackbar(
                    when (n) {
                        0 -> "Erano già tutti in MemShot"
                        1 -> "1 screenshot importato"
                        else -> "$n screenshot importati"
                    }
                )
            }
        }
    }
    val pickImages = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val importExisting = {
        vm.importExisting { n ->
            scope.launch { snackbar.showSnackbar(if (n == 0) "Nessun nuovo screenshot trovato" else "Importo $n screenshot…") }
        }
        Unit
    }

    BackHandler(selecting) { vm.clearSelection() }
    BackHandler(!selecting && (filter != null || query.isNotEmpty())) {
        vm.setQuery("")
        if (filter != null) vm.setFilter(filter)
    }

    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 24 } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        floatingActionButton = {
          Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.navigationBarsPadding(),
          ) {
            ScrollJumpFab(gridState, visible = !selecting && state.shots.isNotEmpty())
            AnimatedVisibility(
                visible = !selecting && state.total > 0,
                enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = pickImages,
                    expanded = !scrolled,
                    icon = { Icon(Icons.Rounded.AddPhotoAlternate, null) },
                    text = { Text("Importa") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                )
            }
          }
        },
    ) { _ ->
        Column(Modifier.fillMaxSize()) {
            val elevation by animateDpAsState(if (scrolled) 10.dp else 0.dp, label = "topElevation")
            Column(
                Modifier
                    .shadow(elevation, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp), clip = false)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                AnimatedContent(
                    targetState = selecting,
                    transitionSpec = {
                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "topBar",
                ) { sel ->
                    if (sel) SelectionBar(
                        count = selection.size,
                        onClose = vm::clearSelection,
                        onSelectAll = vm::selectAllVisible,
                        onShare = { scope.launch { shareFiles(context, vm.files(selection)) } },
                        onDelete = { confirmDelete = true },
                    ) else TitleBar(
                        state = state,
                        collapsed = scrolled,
                        onSettings = { showSettings = true },
                        onCategories = onOpenCategories,
                    )
                }
                if (state.total > 0) {
                    SearchField(query, vm::setQuery, Modifier.padding(horizontal = 16.dp))
                    PendingBanner(pending)
                    FilterRow(state, filter, vm::setFilter)
                    val otherCount = state.tags.firstOrNull { it.tag == Categories.OTHER }?.count ?: 0
                    AnimatedVisibility(
                        filter == Categories.OTHER && otherCount > 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        TriageBanner(otherCount, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                            onTriage(Categories.OTHER)
                        }
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                val mode = when {
                    !state.loaded -> 0
                    state.total == 0 -> 1
                    state.shots.isEmpty() -> 2
                    else -> 3
                }
                AnimatedContent(mode, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "content") { m ->
                    when (m) {
                        1 -> EmptyLibrary(onPick = pickImages, onImportExisting = importExisting)
                        2 -> NoResults(query)
                        3 -> ShotGrid(
                            shots = state.shots,
                            gridState = gridState,
                            focusId = focusId,
                            selection = selection,
                            onClick = { shot -> if (selecting) vm.toggleSelect(shot.id) else onOpen(shot.id) },
                            onLongClick = { shot -> vm.toggleSelect(shot.id) },
                        )
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    // I selettori file vivono qui (non nel pannello impostazioni, che si chiude prima del risultato)
    val backupDone = { msg: String -> scope.launch { snackbar.showSnackbar(msg) }; Unit }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportBackup(uri, backupDone)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restoreBackup(uri, backupDone)
    }

    if (showSettings) SettingsSheet(
        vm = vm,
        onDismiss = { showSettings = false },
        onImportExisting = importExisting,
        onExportBackup = {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALY).format(java.util.Date())
            exportLauncher.launch("MemShot-backup-$date.zip")
        },
        onRestoreBackup = {
            restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        },
    )

    val backup by vm.backupProgress.collectAsStateWithLifecycle()
    backup?.let { p ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(p.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (p.total > 0) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { p.done.toFloat() / p.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${p.done} di ${p.total}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (p.done > 0) Text("${p.done} immagini lette", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        )
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(if (selection.size == 1) "Eliminare lo screenshot?" else "Eliminare ${selection.size} screenshot?") },
        text = { Text("Verranno eliminati da MemShot.") },
        confirmButton = {
            TextButton(onClick = { vm.delete(selection); confirmDelete = false }) {
                Text("Elimina", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
    )
}

@Composable
private fun TitleBar(state: HomeState, collapsed: Boolean, onSettings: () -> Unit, onCategories: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)
            .animateContentSize(spring(stiffness = 500f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "MemShot",
                style = (if (collapsed) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge)
                    .copy(brush = BrandGradient),
                modifier = Modifier.weight(1f),
            )
            if (state.total > 0) IconButton(onClick = onCategories) {
                Icon(Icons.Rounded.GridView, contentDescription = "Categorie")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Tune, contentDescription = "Impostazioni")
            }
        }
        AnimatedVisibility(!collapsed && state.total > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val cats = state.tags.size
            Text(
                "${state.total} screenshot · $cats ${if (cats == 1) "categoria" else "categorie"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Annulla selezione") }
        AnimatedContent(count, transitionSpec = {
            (slideInVertically { if (targetState > initialState) it else -it } + fadeIn()) togetherWith
                (slideOutVertically { if (targetState > initialState) -it else it } + fadeOut())
        }, label = "count", modifier = Modifier.weight(1f)) { n ->
            Text("$n selezionat${if (n == 1) "o" else "i"}", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onSelectAll) { Icon(Icons.Rounded.SelectAll, "Seleziona tutti") }
        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Condividi") }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Elimina", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Cerca parole, film, prompt…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            IconButton(onClick = { onChange(""); focus.clearFocus() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, "Cancella ricerca", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PendingBanner(pending: Int) {
    AnimatedVisibility(pending > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .shimmer()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (pending == 1) "Sto leggendo 1 screenshot…" else "Sto leggendo $pending screenshot…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun FilterRow(state: HomeState, filter: String?, onFilter: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp),
    ) {
        item(key = "all") {
            TagChip(
                tag = TAG_ALL, color = MaterialTheme.colorScheme.primary,
                count = state.total, selected = filter == null, onClick = { onFilter(null) },
                modifier = Modifier.animateItem(),
            )
        }
        if (state.favorites > 0) item(key = FILTER_FAVORITES) {
            TagChip(
                tag = TAG_FAVORITES,
                count = state.favorites, selected = filter == FILTER_FAVORITES, onClick = { onFilter(FILTER_FAVORITES) },
                modifier = Modifier.animateItem(),
            )
        }
        items(state.tags, key = { it.tag }) { t ->
            TagChip(
                tag = t.tag, count = t.count, selected = filter == t.tag, onClick = { onFilter(t.tag) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        com.memshot.app.ui.components.FloatingIcon(Icons.Rounded.SearchOff)
        Spacer(Modifier.height(16.dp))
        Text("Niente da qui", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            if (query.isNotBlank()) "Nessuno screenshot contiene “$query”. Prova con un'altra parola o togli il filtro."
            else "Nessuno screenshot in questa categoria.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
internal fun gridBottomPadding(): PaddingValues {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = nav + 110.dp)
}

/** Invito a sistemare a mano gli screenshot che MemShot non ha saputo classificare. */
@Composable
fun TriageBanner(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandGradient)
            .bouncyClick(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Checklist, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Riordina uno alla volta", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(
                if (count == 1) "1 screenshot da sistemare" else "$count screenshot da sistemare",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White)
    }
}

@Composable
internal fun ShotGrid(
    shots: List<com.memshot.app.data.Shot>,
    gridState: LazyStaggeredGridState,
    focusId: Long?,
    selection: Set<Long>,
    onClick: (com.memshot.app.data.Shot) -> Unit,
    onLongClick: (com.memshot.app.data.Shot) -> Unit,
) {
    val sections = remember(shots) {
        // Se eri in cima, resta in cima quando arrivano screenshot nuovi (invece di restare ancorato ai vecchi)
        val atTop = androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
            gridState.firstVisibleItemIndex <= 1 && gridState.firstVisibleItemScrollOffset < 200
        }
        if (atTop) gridState.requestScrollToItem(0)
        groupByDate(shots)
    }
    // Tornando dal dettaglio dopo aver sfogliato, la griglia si porta sull'ultimo screenshot visto
    androidx.compose.runtime.LaunchedEffect(focusId) {
        if (focusId == null) return@LaunchedEffect
        var index = 0
        var target = -1
        for ((_, items) in sections) {
            index++ // intestazione
            val i = items.indexOfFirst { it.id == focusId }
            if (i >= 0) { target = index + i; break }
            index += items.size
        }
        if (target < 0) return@LaunchedEffect
        val visible = gridState.layoutInfo.visibleItemsInfo.map { it.index }
        if (target !in visible) gridState.scrollToItem((target - 1).coerceAtLeast(0))
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        state = gridState,
        contentPadding = gridBottomPadding(),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        var index = 0
        sections.forEach { (label, items) ->
            item(key = "h-$label", span = StaggeredGridItemSpan.FullLine, contentType = "header") {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .animateItem()
                        .padding(start = 4.dp, top = 8.dp),
                )
            }
            items.forEach { shot ->
                val i = index++
                item(key = shot.id, contentType = "shot") {
                    ShotCard(
                        shot = shot,
                        index = i,
                        selected = shot.id in selection,
                        selecting = selection.isNotEmpty(),
                        onClick = { onClick(shot) },
                        onLongClick = { onLongClick(shot) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
