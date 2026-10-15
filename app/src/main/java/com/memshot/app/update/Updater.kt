package com.memshot.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aggiornamenti dalle Releases di GitHub: l'app va in rete solo quando apri le impostazioni.
 * Lo stato vive qui (non nella schermata), così il download continua anche se chiudi il pannello.
 */
class Updater(private val context: Context) {
    sealed interface Status {
        data object Idle : Status
        data object Checking : Status
        data object UpToDate : Status
        data class Available(val version: String, val url: String, val sizeBytes: Long) : Status
        data class Downloading(val version: String, val progress: Float) : Status
        data class Ready(val version: String, val file: File) : Status
        data class Failed(val message: String) : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "MemShot")
    }

    /** Controlla se c'è una versione nuova (non ricontrolla mentre scarica o se è già pronta). */
    suspend fun check() {
        when (_status.value) {
            is Status.Checking, is Status.Downloading, is Status.Ready -> return
            else -> Unit
        }
        _status.value = Status.Checking
        _status.value = runCatching {
            withContext(Dispatchers.IO) {
                val connection = open(LATEST_RELEASE)
                try {
                    if (connection.responseCode == 404) return@withContext Status.UpToDate // nessuna release ancora
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val version = json.getString("tag_name").removePrefix("v")
                    val assets = json.getJSONArray("assets")
                    val apk = (0 until assets.length()).map(assets::getJSONObject)
                        .firstOrNull { it.getString("name").endsWith(".apk") }
                    val url = apk?.getString("browser_download_url")
                    // Si scarica solo dalla repository ufficiale.
                    if (url != null && url.startsWith(DOWNLOAD_PREFIX) && isNewer(version, currentVersion)) {
                        Status.Available(version, url, apk.optLong("size"))
                    } else {
                        Status.UpToDate
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }.getOrElse { Status.Failed("Controllo non riuscito: verifica la connessione") }
    }

    /** Scarica l'APK mostrando l'avanzamento, poi apre subito l'installazione. */
    suspend fun download(update: Status.Available) {
        _status.value = Status.Downloading(update.version, 0f)
        _status.value = runCatching {
            withContext(Dispatchers.IO) {
                val folder = File(context.cacheDir, "updates").apply {
                    deleteRecursively()
                    mkdirs()
                }
                val file = File(folder, "MemShot-${update.version}.apk")
                val connection = open(update.url)
                try {
                    val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            var lastReported = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) {
                                    val percent = (done * 100 / total).toInt()
                                    if (percent != lastReported) {
                                        lastReported = percent
                                        _status.value = Status.Downloading(update.version, done / total.toFloat())
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                Status.Ready(update.version, file)
            }
        }.getOrElse { Status.Failed("Download non riuscito: riprova") }
        (_status.value as? Status.Ready)?.let(::install)
    }

    /**
     * Passa l'APK all'installatore di Android, che chiede conferma e verifica la firma.
     * La prima volta Android chiede anche di consentire le installazioni da MemShot.
     */
    fun install(ready: Status.Ready) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", ready.file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    companion object {
        private const val REPO = "carellice/MemShot"
        private const val LATEST_RELEASE = "https://api.github.com/repos/$REPO/releases/latest"
        private const val DOWNLOAD_PREFIX = "https://github.com/$REPO/releases/download/"

        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
            val b = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff > 0
            }
            return false
        }
    }
}
