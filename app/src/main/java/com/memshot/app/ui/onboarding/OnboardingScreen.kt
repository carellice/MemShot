package com.memshot.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memshot.app.MemShotApp
import com.memshot.app.importer.ScreenshotImporter
import com.memshot.app.ui.MainViewModel
import com.memshot.app.ui.home.FloatingStack
import com.memshot.app.ui.components.FloatingIcon
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.PhotoLibrary
import com.memshot.app.ui.home.openAllFilesAccess
import com.memshot.app.ui.theme.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var existing by remember { mutableIntStateOf(0) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (ScreenshotImporter.hasMediaPermission(context)) {
            scope.launch {
                existing = (context.applicationContext as MemShotApp).importer.countScreenshots()
                step = 1
            }
        } else {
            vm.setAutoImport(false)
            vm.completeOnboarding()
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
        },
        label = "onboarding",
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { s ->
        when (s) {
            0 -> Welcome(onStart = {
                permissions.launch(
                    buildList {
                        add(ScreenshotImporter.mediaPermission())
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.toTypedArray()
                )
            })
            1 -> CleanGallery(
                onAllow = {
                    openAllFilesAccess(context)
                    if (existing > 0) step = 2 else vm.completeOnboarding()
                },
                onConfirmEachTime = { if (existing > 0) step = 2 else vm.completeOnboarding() },
                onKeep = {
                    vm.setDeleteAfterImport(false)
                    if (existing > 0) step = 2 else vm.completeOnboarding()
                },
            )
            else -> ImportExisting(
                count = existing,
                onImportAll = {
                    vm.completeOnboarding()
                    vm.importExisting { }
                },
                onSkip = { vm.completeOnboarding() },
            )
        }
    }
}

@Composable
private fun Welcome(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        FloatingStack()
        Spacer(Modifier.height(28.dp))
        Text("MemShot", style = MaterialTheme.typography.displaySmall.copy(brush = BrandGradient))
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu fai gli screenshot.\nMemShot se li ricorda per te.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        val features = listOf(
            Triple(Icons.Rounded.Bolt, "Import automatico", "Ogni screenshot che fai entra subito in MemShot"),
            Triple(Icons.Rounded.AutoAwesome, "Tag intelligenti", "Legge il testo e capisce se è un film, un prompt, una ricetta…"),
            Triple(Icons.Rounded.Search, "Ritrovi tutto", "Cerca qualsiasi parola scritta dentro lo screenshot"),
            Triple(Icons.Rounded.Lock, "Privato", "L'analisi avviene sul telefono, niente va online"),
        )
        features.forEachIndexed { i, (icon, title, body) -> Feature(icon, title, body, i) }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Iniziamo", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(10.dp))
        Text(
            "Ti chiederò l'accesso alle immagini per trovare i nuovi screenshot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String, index: Int) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250L + index * 110L)
        anim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 200f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer {
                alpha = anim.value
                translationX = (1f - anim.value) * 60f * density
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleanGallery(onAllow: () -> Unit, onConfirmEachTime: () -> Unit, onKeep: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FloatingIcon(Icons.Rounded.CleaningServices)
        Spacer(Modifier.height(16.dp))
        Text("Galleria sempre pulita", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Dopo averli importati, MemShot toglie gli screenshot dal telefono: restano solo qui, già catalogati. " +
                "Gli originali finiscono nel cestino del telefono e si possono recuperare per 30 giorni.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Per farlo in automatico, senza chiederti conferma ogni volta, attiva \"Consenti l'accesso per gestire tutti i file\" nella schermata che si apre.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Attiva eliminazione automatica", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onConfirmEachTime) { Text("Chiedimi conferma ogni volta") }
        TextButton(onClick = onKeep) { Text("No, lasciali anche nella galleria") }
    }
}

@Composable
private fun ImportExisting(count: Int, onImportAll: () -> Unit, onSkip: () -> Unit) {
    var working by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FloatingIcon(Icons.Rounded.PhotoLibrary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Ho trovato $count screenshot",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Vuoi che li importi e li cataloghi subito? Ci vuole qualche istante, e puoi già usare l'app mentre lavoro.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { working = true; onImportAll() },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            if (working) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Sì, importali tutti", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, enabled = !working) { Text("No, solo quelli nuovi da adesso") }
    }
}
