package com.memshot.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.remember
import com.memshot.app.ui.categories.CategoriesScreen
import com.memshot.app.ui.categories.CategoryScreen
import com.memshot.app.ui.categories.shotsIn
import com.memshot.app.ui.triage.TriageScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memshot.app.ui.MainViewModel
import com.memshot.app.ui.components.LocalNavScope
import com.memshot.app.ui.components.LocalSharedScope
import com.memshot.app.ui.detail.DetailScreen
import com.memshot.app.ui.home.HomeScreen
import com.memshot.app.ui.onboarding.OnboardingScreen
import com.memshot.app.ui.theme.MemShotTheme
import com.memshot.app.work.Notifications
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val openShot = MutableStateFlow<Long?>(null)
    private var trashDialogShown = false
    private val trashLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        trashDialogShown = false
        vm.onTrashRequestHandled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        // Quando serve, chiede ad Android di togliere dalla galleria gli screenshot già importati
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.trashRequest.collect { sender ->
                    if (sender != null && !trashDialogShown) {
                        trashDialogShown = true
                        trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                }
            }
        }
        setContent {
            MemShotTheme {
                val onboarded by vm.onboarded.collectAsStateWithLifecycle()
                AnimatedContent(
                    targetState = onboarded,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                    label = "root",
                ) { done ->
                    if (done) Library(vm, openShot) else OnboardingScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recupera eventuali screenshot fatti mentre l'app era chiusa
        vm.syncNow()
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(Notifications.EXTRA_SHOT_ID, -1L) ?: -1L
        if (id > 0) openShot.value = id
    }
}

/**
 * Navigazione a pila, salvata come testo (sopravvive alla rotazione):
 * home · cats · cat|<tag> · triage|<tag> · detail|<id>|<origine>
 */
@Composable
private fun Library(vm: MainViewModel, openShot: MutableStateFlow<Long?>) {
    var stack by rememberSaveable { mutableStateOf("home") }
    val routes = stack.split('\n')
    fun push(route: String) { stack = "$stack\n$route" }
    fun pop() { if (routes.size > 1) stack = stack.substringBeforeLast('\n') }

    val settings by vm.settings.collectAsStateWithLifecycle()
    // Stato delle griglie tenuto qui, così tornando indietro ritrovi lo stesso punto
    val gridStates = remember { mutableMapOf<String, LazyStaggeredGridState>() }
    fun grid(key: String) = gridStates.getOrPut(key) { LazyStaggeredGridState() }
    var focusId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        openShot.collect { id ->
            if (id != null) {
                push("detail|$id|single")
                openShot.value = null
            }
        }
    }

    BackHandler(routes.size > 1) { pop() }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedScope provides this) {
            AnimatedContent(
                targetState = routes,
                contentKey = { it.last() },
                transitionSpec = {
                    val forward = targetState.size > initialState.size
                    val involvesDetail = targetState.last().startsWith("detail") || initialState.last().startsWith("detail")
                    if (involvesDetail) {
                        fadeIn(tween(320)) togetherWith fadeOut(tween(320))
                    } else {
                        (slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(220)))
                    }
                },
                label = "nav",
            ) { stackNow ->
                val route = stackNow.last()
                CompositionLocalProvider(LocalNavScope provides this) {
                    when {
                        route == "home" -> HomeScreen(
                            vm = vm,
                            gridState = grid("home"),
                            focusId = focusId,
                            onOpen = { push("detail|$it|home") },
                            onOpenCategories = { push("cats") },
                            onTriage = { push("triage|$it") },
                        )
                        route == "cats" -> CategoriesScreen(vm, onBack = ::pop, onOpen = { push("cat|$it") })
                        route.startsWith("cat|") -> {
                            val tag = route.substringAfter('|')
                            CategoryScreen(
                                vm = vm,
                                tag = tag,
                                gridState = grid(route),
                                focusId = focusId,
                                onBack = ::pop,
                                onOpen = { push("detail|$it|cat=$tag") },
                                onTriage = { push("triage|$tag") },
                            )
                        }
                        route.startsWith("triage|") -> TriageScreen(vm, route.substringAfter('|'), onClose = ::pop)
                        route.startsWith("detail|") -> {
                            val parts = route.split('|', limit = 3)
                            val id = parts[1].toLong()
                            val origin = parts.getOrElse(2) { "single" }
                            val ids = remember(route) {
                                when {
                                    origin == "home" -> vm.home.value.shots.map { it.id }
                                    origin.startsWith("cat=") -> shotsIn(vm.all.value, origin.removePrefix("cat=")).map { it.id }
                                    else -> listOf(id)
                                }
                            }
                            DetailScreen(
                                vm = vm,
                                ids = ids,
                                startId = id,
                                initial = { wanted -> vm.all.value.firstOrNull { it.id == wanted } },
                                vertical = settings.verticalViewer,
                                onBack = { lastSeen ->
                                    focusId = lastSeen
                                    pop()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
