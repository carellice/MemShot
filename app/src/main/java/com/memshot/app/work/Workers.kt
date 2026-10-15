package com.memshot.app.work

import android.content.Context
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memshot.app.MemShotApp
import com.memshot.app.data.ShotStatus
import java.io.File
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WATCH = "watch-screenshots"
    private const val ANALYZE = "analyze-shots"

    /**
     * Lavoro "dormiente" che Android risveglia appena compare una nuova immagine nella galleria.
     * Funziona anche con MemShot chiusa, senza servizi sempre attivi.
     */
    fun watch(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(1, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(4, TimeUnit.SECONDS)
            .build()
        val request = OneTimeWorkRequestBuilder<ScreenshotWatchWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WATCH, policy, request)
    }

    fun stopWatching(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WATCH)

    fun analyze(context: Context, notifyIds: List<Long> = emptyList()) {
        val request = OneTimeWorkRequestBuilder<AnalyzeWorker>()
            .setInputData(workDataOf(AnalyzeWorker.KEY_NOTIFY to notifyIds.toLongArray()))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ANALYZE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

class ScreenshotWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MemShotApp
        if (!app.prefs.autoImport) return Result.success()
        try {
            val result = app.importer.scanScreenshots(app.prefs.lastScanSeconds)
            app.prefs.lastScanSeconds = result.newestSeconds
            if (result.ids.isNotEmpty()) WorkScheduler.analyze(app, if (app.prefs.notifyOnImport) result.ids else emptyList())
        } finally {
            // si rimette in ascolto per il prossimo screenshot
            WorkScheduler.watch(app, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        return Result.success()
    }
}

class AnalyzeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MemShotApp
        val dao = app.db.shots()
        while (!isStopped) {
            val batch = dao.pending(8)
            if (batch.isEmpty()) break
            for (shot in batch) {
                if (isStopped) return Result.retry()
                val updated = runCatching {
                    val a = app.analyzer.analyze(File(shot.filePath), shot.sourceApp)
                    shot.copy(
                        ocrText = a.text,
                        visualLabels = a.labels.map { it.text },
                        autoTags = a.tags,
                        status = ShotStatus.DONE,
                    )
                }.getOrElse { shot.copy(status = ShotStatus.FAILED) }
                dao.update(updated)
            }
        }
        val notifyIds = inputData.getLongArray(KEY_NOTIFY)?.toList().orEmpty()
        if (notifyIds.isNotEmpty()) Notifications.imported(app, dao.getAll(notifyIds))
        return Result.success()
    }

    companion object {
        const val KEY_NOTIFY = "notify"
    }
}
