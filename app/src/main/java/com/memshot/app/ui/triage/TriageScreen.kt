package com.memshot.app.ui.triage

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memshot.app.analysis.Categories
import com.memshot.app.ui.MainViewModel
import com.memshot.app.ui.components.TagChip
import com.memshot.app.ui.components.FloatingIcon
import com.memshot.app.ui.components.TAG_NEW
import androidx.compose.material.icons.rounded.Celebration
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private data class Decision(val id: Long, val tag: String?)

/**
 * Riordino: mostra uno alla volta gli screenshot di una categoria (di solito "Altro")
 * e con un tocco li sposti nella categoria giusta. Scorri a sinistra per lasciarlo dov'è.
 */
@Composable
fun TriageScreen(vm: MainViewModel, tag: String, onClose: () -> Unit) {
    val all by vm.all.collectAsStateWithLifecycle()
    // La coda è fissata all'apertura: gli screenshot spostati non spariscono da sotto le dita
    val queue = remember { all.filter { tag in it.tags }.map { it.id } }
    var index by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Decision>() }
    var newTag by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    BackHandler { onClose() }

    val currentId = queue.getOrNull(index)
    val current = all.firstOrNull { it.id == currentId }

    fun decide(choice: String?) {
        val id = currentId ?: return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (choice != null) vm.assignCategory(id, choice)
        history += Decision(id, choice)
        index++
    }

    fun undo() {
        val last = history.removeLastOrNull() ?: return
        if (last.tag != null) vm.unassignCategory(last.id, last.tag, from = tag)
        index = (index - 1).coerceAtLeast(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Chiudi") }
            Column(Modifier.weight(1f)) {
                Text("Riordina ${Categories.label(tag)}", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (currentId != null) "${index + 1} di ${queue.size}" else "Finito",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = ::undo, enabled = history.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Rounded.Undo, "Annulla l'ultimo")
            }
        }
        val progress by animateFloatAsState(
            if (queue.isEmpty()) 1f else index.toFloat() / queue.size, spring(stiffness = 300f), label = "progress",
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(6.dp).clip(CircleShape),
            drawStopIndicator = {},
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            gapSize = 0.dp,
        )

        if (current == null) {
            Done(sorted = history.count { it.tag != null }, onClose = onClose)
            return@Column
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = current.id,
                transitionSpec = {
                    (scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.7f)) + fadeIn(tween(220))) togetherWith
                        fadeOut(tween(160))
                },
                label = "card",
            ) { id ->
                val shot = all.firstOrNull { it.id == id } ?: return@AnimatedContent
                SwipeCard(onSwipedAway = { decide(null) }) {
                    Column(
                        Modifier
                            .shadow(18.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(shot.filePath)).memoryCacheKey(shot.filePath + "#full").build(),
                            contentDescription = "Screenshot da riordinare",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.04f)),
                        )
                        val snippet = shot.ocrText.lines().filter { it.isNotBlank() }.drop(1).joinToString(" ")
                        if (snippet.isNotBlank()) {
                            Text(
                                snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Dove lo metto?",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        FlowRow(
            Modifier
                .heightIn(max = 210.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Categories.all.filter { it.id != tag && it.id != Categories.OTHER }.forEach { c ->
                TagChip(tag = c.id, compact = true, onClick = { decide(c.id) })
            }
            TagChip(
                tag = TAG_NEW, label = "Nuovo tag", color = MaterialTheme.colorScheme.primary,
                compact = true, onClick = { newTag = true },
            )
        }
        OutlinedButton(
            onClick = { decide(null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(50.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Lascia in ${Categories.label(tag)}  ·  o scorri a sinistra") }
    }

    if (newTag) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newTag = false },
            title = { Text("Nuovo tag") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    placeholder = { Text("es. idee regalo") }, shape = RoundedCornerShape(16.dp),
                )
            },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = {
                    newTag = false
                    decide(text.trim().removePrefix("#").lowercase())
                }) { Text("Sposta qui") }
            },
            dismissButton = { TextButton(onClick = { newTag = false }) { Text("Annulla") } },
        )
    }
}

/** Card trascinabile: oltre una certa distanza vola via e passa alla prossima. */
@Composable
private fun SwipeCard(onSwipedAway: () -> Unit, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .graphicsLayer {
                translationX = offset.value
                rotationZ = offset.value / 40f
                alpha = 1f - (abs(offset.value) / (size.width * 1.5f)).coerceIn(0f, 0.6f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > size.width * 0.3f) {
                                offset.animateTo(if (offset.value > 0) size.width * 1.4f else -size.width * 1.4f, tween(200))
                                onSwipedAway()
                            } else {
                                offset.animateTo(0f, spring(dampingRatio = 0.6f))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                    onHorizontalDrag = { _, delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                )
            }
    ) { content() }
}

@Composable
private fun Done(sorted: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FloatingIcon(Icons.Rounded.Celebration)
        Spacer(Modifier.height(16.dp))
        Text("Tutto in ordine!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            when (sorted) {
                0 -> "Non hai spostato niente: restano dove sono."
                1 -> "Hai sistemato 1 screenshot. Quando so da che app arriva, me lo ricorderò anche per i prossimi."
                else -> "Hai sistemato $sorted screenshot. Quando so da che app arrivano, me lo ricorderò anche per i prossimi."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose, shape = RoundedCornerShape(16.dp), modifier = Modifier.width(200.dp).height(50.dp)) {
            Text("Fine")
        }
    }
}

