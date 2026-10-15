package com.memshot.app.ui

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memshot.app.MemShotApp
import com.memshot.app.analysis.Categories
import com.memshot.app.analysis.normalizeForSearch
import com.memshot.app.data.Backup
import com.memshot.app.data.Shot
import com.memshot.app.data.ShotStatus
import com.memshot.app.importer.ScreenshotImporter
import com.memshot.app.work.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Filtro speciale per i preferiti nella barra dei tag. */
const val FILTER_FAVORITES = "__fav"

data class TagCount(val tag: String, val count: Int)

data class HomeState(
    val loaded: Boolean = false,
    val total: Int = 0,
    val shots: List<Shot> = emptyList(),
    val tags: List<TagCount> = emptyList(),
    val favorites: Int = 0,
)

data class Settings(
    val autoImport: Boolean,
    val notify: Boolean,
    val deleteAfterImport: Boolean,
    val verticalViewer: Boolean,
)

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val memshot = app as MemShotApp
    private val dao = memshot.db.shots()
    private val prefs = memshot.prefs

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _onboarded = MutableStateFlow(prefs.onboarded)
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    val settings: StateFlow<Settings> = prefs.changes()
        .map { Settings(prefs.autoImport, prefs.notifyOnImport, prefs.deleteAfterImport, prefs.verticalViewer) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings(prefs.autoImport, prefs.notifyOnImport, prefs.deleteAfterImport, prefs.verticalViewer))

    val pendingCount: StateFlow<Int> = dao.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Testo indicizzato per ogni screenshot: OCR + nomi dei tag + app + cosa si vede nell'immagine. */
    private val indexed = dao.observeAll()
        .map { list ->
            list.map { shot ->
                val tagWords = shot.tags.joinToString(" ") { "${Categories.label(it)} $it" }
                shot to "${shot.ocrText} $tagWords ${shot.sourceApp.orEmpty()} ${shot.visualLabels.joinToString(" ")}"
                    .normalizeForSearch()
            }
        }
        .flowOn(Dispatchers.Default)

    val home: StateFlow<HomeState> = combine(indexed, _query.debounce(150), _filter) { all, q, filter ->
        val tokens = q.normalizeForSearch().split(' ').filter { it.isNotBlank() }
        val shots = all.asSequence()
            .filter { (shot, _) ->
                when (filter) {
                    null -> true
                    FILTER_FAVORITES -> shot.favorite
                    else -> filter in shot.tags
                }
            }
            .filter { (_, index) -> tokens.all { index.contains(it) } }
            .map { it.first }
            .toList()
        val counts = all.flatMap { it.first.tags }.groupingBy { it }.eachCount()
            .map { TagCount(it.key, it.value) }
            .sortedWith(compareByDescending<TagCount> { it.tag != Categories.OTHER }.thenByDescending { it.count })
        HomeState(
            loaded = true,
            total = all.size,
            shots = shots,
            tags = counts,
            favorites = all.count { it.first.favorite },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    /** Tutti gli screenshot, dal più recente: base per le schermate di categoria e il riordino. */
    val all: StateFlow<List<Shot>> = indexed.map { list -> list.map { it.first } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun shot(id: Long) = dao.observe(id)

    fun setVerticalViewer(enabled: Boolean) { prefs.verticalViewer = enabled }

    /**
     * Sposta uno screenshot da "Altro" in una categoria scelta a mano.
     * MemShot se lo ricorda: i prossimi screenshot della stessa app andranno lì da soli.
     */
    /** Annulla [assignCategory]: lo screenshot torna in [from]. */
    fun unassignCategory(id: Long, tag: String, from: String) = viewModelScope.launch {
        val shot = dao.get(id) ?: return@launch
        dao.update(
            shot.copy(
                userTags = shot.userTags - tag + (if (from in shot.autoTags) emptyList() else listOf(from)),
                removedTags = shot.removedTags - from,
            )
        )
    }

    fun assignCategory(id: Long, tag: String) = viewModelScope.launch {
        val shot = dao.get(id) ?: return@launch
        dao.update(
            shot.copy(
                userTags = (shot.userTags + tag).distinct(),
                removedTags = (shot.removedTags + Categories.OTHER).distinct(),
            )
        )
        val app = shot.sourceApp
        if (app != null && Categories.find(tag) != null) {
            prefs.learnedApps = prefs.learnedApps + (com.memshot.app.analysis.appKey(app) to tag)
        }
    }

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(tag: String?) { _filter.value = if (_filter.value == tag) null else tag }

    // ---- Selezione multipla ----
    fun toggleSelect(id: Long) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }
    fun clearSelection() { _selection.value = emptySet() }
    fun selectAllVisible() { _selection.value = home.value.shots.map { it.id }.toSet() }

    // ---- Import ----
    fun importUris(uris: List<Uri>, onDone: (Int) -> Unit) = viewModelScope.launch {
        val ids = memshot.importer.import(uris)
        if (ids.isNotEmpty()) WorkScheduler.analyze(memshot)
        onDone(ids.size)
        refreshTrashRequest()
    }

    /** Importa gli screenshot già presenti in galleria (tutti, o solo da adesso in poi). */
    fun importExisting(onDone: (Int) -> Unit) = viewModelScope.launch {
        val result = memshot.importer.scanScreenshots(0)
        prefs.lastScanSeconds = maxOf(prefs.lastScanSeconds, result.newestSeconds)
        if (result.ids.isNotEmpty()) WorkScheduler.analyze(memshot)
        onDone(result.ids.size)
        refreshTrashRequest()
    }

    fun syncNow() = viewModelScope.launch {
        if (prefs.onboarded && prefs.autoImport && ScreenshotImporter.hasMediaPermission(memshot)) {
            val result = memshot.importer.scanScreenshots(prefs.lastScanSeconds)
            prefs.lastScanSeconds = result.newestSeconds
            if (result.ids.isNotEmpty()) WorkScheduler.analyze(memshot)
        }
        refreshTrashRequest()
    }

    // ---- Aggiornamenti ----
    val updateStatus get() = memshot.updater.status
    val currentVersion get() = memshot.updater.currentVersion
    fun checkForUpdate() = viewModelScope.launch { memshot.updater.check() }
    fun downloadUpdate(update: com.memshot.app.update.Updater.Status.Available) =
        viewModelScope.launch { memshot.updater.download(update) }
    fun installUpdate(ready: com.memshot.app.update.Updater.Status.Ready) = memshot.updater.install(ready)

    // ---- Backup ----
    private val _backupProgress = MutableStateFlow<Backup.Progress?>(null)
    /** Avanzamento di un backup/ripristino in corso (null = nessuno). */
    val backupProgress: StateFlow<Backup.Progress?> = _backupProgress.asStateFlow()

    fun exportBackup(target: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        _backupProgress.value = Backup.Progress("Preparo il backup", 0, 0)
        val message = runCatching { memshot.backup.export(target) { _backupProgress.value = it } }
            .fold(
                onSuccess = { n -> if (n == 1) "Backup salvato: 1 screenshot" else "Backup salvato: $n screenshot" },
                onFailure = { "Backup non riuscito: ${it.message}" },
            )
        _backupProgress.value = null
        onDone(message)
    }

    fun restoreBackup(source: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        _backupProgress.value = Backup.Progress("Leggo il backup", 0, 0)
        val message = runCatching { memshot.backup.restore(source) { _backupProgress.value = it } }
            .fold(
                onSuccess = { r ->
                    if (r.added > 0) WorkScheduler.analyze(memshot)
                    buildString {
                        append(if (r.added == 1) "Ripristinato 1 screenshot" else "Ripristinati ${r.added} screenshot")
                        if (r.skipped > 0) append(" · ${r.skipped} già presenti")
                    }
                },
                onFailure = {
                    if (it is Backup.InvalidBackupException) it.message!! else "Ripristino non riuscito: ${it.message}"
                },
            )
        _backupProgress.value = null
        onDone(message)
    }

    // ---- Rimozione degli originali dal telefono ----
    private val _trashRequest = MutableStateFlow<IntentSender?>(null)
    /** Dialogo di sistema da mostrare per togliere dalla galleria gli screenshot già importati. */
    val trashRequest: StateFlow<IntentSender?> = _trashRequest.asStateFlow()

    private fun refreshTrashRequest() = viewModelScope.launch(Dispatchers.IO) {
        if (_trashRequest.value == null) _trashRequest.value = memshot.cleaner.pendingRequest()
    }

    fun onTrashRequestHandled() {
        memshot.cleaner.clearPending()
        _trashRequest.value = null
    }

    fun canDeleteSilently() = memshot.cleaner.canDeleteSilently()

    fun setDeleteAfterImport(enabled: Boolean) {
        prefs.deleteAfterImport = enabled
        if (!enabled) memshot.cleaner.clearPending()
    }

    fun completeOnboarding() {
        prefs.onboarded = true
        if (prefs.lastScanSeconds == 0L) prefs.lastScanSeconds = System.currentTimeMillis() / 1000
        _onboarded.value = true
        if (prefs.autoImport) WorkScheduler.watch(memshot)
    }

    fun setAutoImport(enabled: Boolean) {
        prefs.autoImport = enabled
        if (enabled) {
            prefs.lastScanSeconds = System.currentTimeMillis() / 1000
            WorkScheduler.watch(memshot)
        } else WorkScheduler.stopWatching(memshot)
    }

    fun setNotify(enabled: Boolean) { prefs.notifyOnImport = enabled }

    // ---- Modifiche ----
    private fun edit(id: Long, change: (Shot) -> Shot) = viewModelScope.launch {
        dao.get(id)?.let { dao.update(change(it)) }
    }

    fun toggleFavorite(id: Long) = edit(id) { it.copy(favorite = !it.favorite) }

    fun addTag(id: Long, raw: String) {
        val tag = Categories.all.firstOrNull { it.label.equals(raw.trim(), true) || it.id == raw }?.id
            ?: raw.trim().removePrefix("#").lowercase()
        if (tag.isBlank()) return
        edit(id) {
            if (tag in it.autoTags) it.copy(removedTags = it.removedTags - tag)
            else it.copy(userTags = (it.userTags + tag).distinct())
        }
    }

    fun removeTag(id: Long, tag: String) = edit(id) {
        it.copy(
            userTags = it.userTags - tag,
            removedTags = if (tag in it.autoTags) (it.removedTags + tag).distinct() else it.removedTags,
        )
    }

    fun reanalyze(id: Long) = viewModelScope.launch {
        dao.get(id)?.let { dao.update(it.copy(status = ShotStatus.PENDING)) }
        WorkScheduler.analyze(memshot)
    }

    fun reanalyzeAll() = viewModelScope.launch {
        dao.markAllPending()
        WorkScheduler.analyze(memshot)
    }

    fun delete(ids: Collection<Long>) = viewModelScope.launch {
        val list = ids.toList()
        withContext(Dispatchers.IO) {
            dao.getAll(list).forEach { File(it.filePath).delete() }
            dao.delete(list)
        }
        _selection.value = _selection.value - ids.toSet()
    }

    suspend fun files(ids: Collection<Long>): List<File> = dao.getAll(ids.toList()).map { File(it.filePath) }
}
