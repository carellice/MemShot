package com.memshot.app.ui.home

import android.Manifest
import android.widget.Toast
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.memshot.app.ui.theme.Amber
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import com.memshot.app.ui.components.tagIcon
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memshot.app.analysis.Categories
import com.memshot.app.importer.ScreenshotImporter
import com.memshot.app.ui.MainViewModel

/** Pila di "screenshot" fluttuanti con i tag: l'illustrazione della libreria vuota e dell'onboarding. */
@Composable
fun FloatingStack(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "stack")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val cards = listOf(
        Triple("film", -14f, -70),
        Triple("prompt", 10f, 70),
        Triple("recipe", -2f, 0),
    )
    Box(modifier.size(260.dp, 230.dp), contentAlignment = Alignment.Center) {
        cards.forEachIndexed { i, (tag, rot, dx) ->
            val wobble = (phase - 0.5f) * (if (i % 2 == 0) 6f else -6f)
            Box(
                Modifier
                    .offset(x = dx.dp)
                    .graphicsLayer {
                        rotationZ = rot + wobble
                        translationY = (phase - 0.5f) * (12f + i * 6f) * density
                    }
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .size(110.dp, 190.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(Categories.color(tag)), Color(Categories.color(tag)).copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(tagIcon(tag), null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                    repeat(3) { line ->
                        Box(
                            Modifier
                                .fillMaxWidth(if (line == 2) 0.55f else 0.9f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(Categories.color(tag)).copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            Categories.label(tag), fontSize = 10.sp, maxLines = 1,
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibrary(onPick: () -> Unit, onImportExisting: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FloatingStack()
        Spacer(Modifier.height(28.dp))
        Text("Ancora nessuno screenshot", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Fai uno screenshot come sempre: MemShot lo prende da solo, legge il testo e lo mette nella categoria giusta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (ScreenshotImporter.hasMediaPermission(context)) {
            Button(onClick = onImportExisting, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Sync, null)
                Spacer(Modifier.width(8.dp))
                Text("Importa quelli che ho già")
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Rounded.PhotoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Scegli dalla galleria")
        }
    }
}

@Composable
fun SettingsSheet(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onImportExisting: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasMedia by remember { mutableStateOf(ScreenshotImporter.hasMediaPermission(context)) }
    var silent by remember { mutableStateOf(vm.canDeleteSilently()) }
    LifecycleResumeEffect(Unit) {
        silent = vm.canDeleteSilently()
        hasMedia = ScreenshotImporter.hasMediaPermission(context)
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasMedia = ScreenshotImporter.hasMediaPermission(context)
        if (hasMedia) vm.setAutoImport(true)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (!hasMedia) {
                SettingRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = "Consenti accesso agli screenshot",
                    subtitle = "Serve per importarli in automatico appena li fai",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        permission.launch(
                            buildList {
                                add(ScreenshotImporter.mediaPermission())
                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                            }.toTypedArray()
                        )
                    },
                )
            }
            SettingRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Import automatico",
                subtitle = "Ogni nuovo screenshot entra in MemShot da solo",
                checked = settings.autoImport && hasMedia,
                onClick = { if (hasMedia) vm.setAutoImport(!settings.autoImport) },
            )
            SettingRow(
                icon = Icons.Rounded.DeleteSweep,
                title = "Togli dal telefono dopo l'import",
                subtitle = "Lo screenshot resta solo in MemShot (l'originale va nel cestino del telefono per 30 giorni)",
                checked = settings.deleteAfterImport,
                onClick = { vm.setDeleteAfterImport(!settings.deleteAfterImport) },
            )
            AnimatedVisibility(settings.deleteAfterImport && !silent) {
                SettingRow(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Elimina senza chiedere conferma",
                    subtitle = "Consenti \"Accesso a tutti i file\": così funziona anche in background, senza dialoghi",
                    tint = Amber,
                    onClick = { openAllFilesAccess(context) },
                )
            }
            SettingRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "Avvisami quando salvo",
                subtitle = "Una notifica discreta con i tag trovati",
                checked = settings.notify,
                onClick = { vm.setNotify(!settings.notify) },
            )
            ViewerModePicker(vertical = settings.verticalViewer, onChange = vm::setVerticalViewer)
            SettingRow(
                icon = Icons.Rounded.CollectionsBookmark,
                title = "Importa screenshot già presenti",
                subtitle = "Cerca nella galleria quelli fatti prima di installare MemShot",
                onClick = { onImportExisting(); onDismiss() },
            )
            Text(
                "Backup",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
            )
            SettingRow(
                icon = Icons.Rounded.CloudUpload,
                title = "Esporta backup",
                subtitle = "Un file .zip con screenshot, testo e tag: salvalo in Download, su Google Drive o dove vuoi",
                onClick = { onExportBackup(); onDismiss() },
            )
            SettingRow(
                icon = Icons.Rounded.CloudDownload,
                title = "Ripristina da backup",
                subtitle = "Aggiunge gli screenshot del backup a quelli che hai già, senza doppioni",
                onClick = { onRestoreBackup(); onDismiss() },
            )
            Text(
                "Manutenzione",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
            )
            SettingRow(
                icon = Icons.Rounded.Sync,
                title = "Rianalizza tutto",
                subtitle = "Rilegge il testo e ricalcola i tag automatici",
                onClick = { vm.reanalyzeAll(); onDismiss() },
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
            ) {
                Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Da qualsiasi app puoi anche usare Condividi → “Salva in MemShot”. Tutta l'analisi avviene sul telefono: niente viene caricato online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Scelta di come sfogliare gli screenshot nel dettaglio. */
@Composable
private fun ViewerModePicker(vertical: Boolean, onChange: (Boolean) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
        Text("Come sfogliare gli screenshot", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard(Icons.Rounded.SwapHoriz, "Galleria", "Scorri in orizzontale", selected = !vertical, Modifier.weight(1f)) { onChange(false) }
            ModeCard(Icons.Rounded.SwapVert, "Stile TikTok", "Scorri in verticale, a tutto schermo", selected = vertical, Modifier.weight(1f)) { onChange(true) }
        }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "modeBorder",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, border, RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Apre la schermata di sistema "Accesso a tutti i file" per MemShot. */
fun openAllFilesAccess(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val specific = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(specific) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (checked != null) {
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = { onClick() })
        }
    }
}
