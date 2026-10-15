package com.memshot.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup in un unico file .zip: dentro ci sono le immagini (`shots/…`) e un `memshot.json`
 * con testo riconosciuto, tag, preferiti e ciò che MemShot ha imparato dai tuoi riordini.
 * Il ripristino unisce il backup alla libreria attuale, saltando gli screenshot già presenti.
 */
class Backup(private val context: Context, private val dao: ShotDao, private val prefs: Prefs) {

    data class Progress(val title: String, val done: Int, val total: Int)
    data class RestoreResult(val added: Int, val skipped: Int)

    class InvalidBackupException : Exception("Questo file non è un backup di MemShot")

    private val dir = File(context.filesDir, "shots").apply { mkdirs() }

    suspend fun export(target: Uri, onProgress: (Progress) -> Unit): Int = withContext(Dispatchers.IO) {
        val shots = dao.getAllOnce()
        val out = context.contentResolver.openOutputStream(target, "wt") ?: error("Impossibile scrivere il file")
        ZipOutputStream(out.buffered()).use { zip ->
            // Le immagini sono già compresse: salvarle così come sono è molto più veloce
            zip.setLevel(Deflater.NO_COMPRESSION)
            val items = JSONArray()
            shots.forEachIndexed { i, shot ->
                onProgress(Progress("Esporto gli screenshot", i, shots.size))
                val file = File(shot.filePath)
                if (!file.exists()) return@forEachIndexed
                val entry = "shots/${shot.id}.${file.extension.ifEmpty { "png" }}"
                zip.putNextEntry(ZipEntry(entry))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                items.put(shot.toJson(entry))
            }
            val root = JSONObject()
                .put("app", "MemShot")
                .put("version", VERSION)
                .put("exportedAt", System.currentTimeMillis())
                .put("learnedApps", JSONObject(prefs.learnedApps as Map<*, *>))
                .put("shots", items)
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(root.toString().toByteArray())
            zip.closeEntry()
            onProgress(Progress("Esporto gli screenshot", shots.size, shots.size))
            items.length()
        }
    }

    suspend fun restore(source: Uri, onProgress: (Progress) -> Unit): RestoreResult = withContext(Dispatchers.IO) {
        val extracted = HashMap<String, File>()
        var manifest: JSONObject? = null
        try {
            val input = context.contentResolver.openInputStream(source) ?: throw InvalidBackupException()
            ZipInputStream(input.buffered()).use { zip ->
                var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == MANIFEST -> manifest = JSONObject(zip.readBytes().decodeToString())
                        entry.name.startsWith("shots/") -> {
                            // Il nome nello zip serve solo come chiave: il file finisce sempre nella nostra cartella
                            val ext = entry.name.substringAfterLast('.', "png").filter { it.isLetterOrDigit() }.take(5)
                            val file = File(dir, "${UUID.randomUUID()}.$ext")
                            file.outputStream().use { zip.copyTo(it) }
                            extracted[entry.name] = file
                            onProgress(Progress("Leggo il backup", ++count, 0))
                        }
                    }
                    zip.closeEntry()
                }
            }
            val root = manifest?.takeIf { it.optString("app") == "MemShot" } ?: throw InvalidBackupException()
            val items = root.getJSONArray("shots")
            var added = 0
            var skipped = 0
            for (i in 0 until items.length()) {
                onProgress(Progress("Ripristino gli screenshot", i, items.length()))
                val json = items.getJSONObject(i)
                val file = extracted.remove(json.getString("file"))
                val key = json.getString("sourceKey")
                if (file == null || dao.exists(key)) {
                    file?.delete()
                    skipped++
                    continue
                }
                val id = dao.insert(json.toShot(file.path))
                if (id > 0) added++ else { file.delete(); skipped++ }
            }
            root.optJSONObject("learnedApps")?.let { learned ->
                prefs.learnedApps = prefs.learnedApps + learned.keys().asSequence().associateWith { learned.getString(it) }
            }
            RestoreResult(added, skipped)
        } finally {
            // immagini nello zip senza una voce corrispondente nel manifest
            extracted.values.forEach { it.delete() }
        }
    }

    private fun Shot.toJson(entry: String) = JSONObject()
        .put("file", entry)
        .put("sourceKey", sourceKey)
        .put("width", width)
        .put("height", height)
        .put("createdAt", createdAt)
        .put("importedAt", importedAt)
        .put("sourceApp", sourceApp ?: JSONObject.NULL)
        .put("ocrText", ocrText)
        .put("visualLabels", JSONArray(visualLabels))
        .put("autoTags", JSONArray(autoTags))
        .put("userTags", JSONArray(userTags))
        .put("removedTags", JSONArray(removedTags))
        .put("favorite", favorite)
        .put("status", status.name)

    private fun JSONObject.toShot(path: String): Shot {
        fun list(name: String): List<String> =
            optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        val status = runCatching { ShotStatus.valueOf(optString("status")) }.getOrDefault(ShotStatus.PENDING)
        return Shot(
            sourceKey = getString("sourceKey"),
            filePath = path,
            width = optInt("width"),
            height = optInt("height"),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            importedAt = optLong("importedAt", System.currentTimeMillis()),
            sourceApp = if (isNull("sourceApp")) null else optString("sourceApp"),
            ocrText = optString("ocrText"),
            visualLabels = list("visualLabels"),
            autoTags = list("autoTags"),
            userTags = list("userTags"),
            removedTags = list("removedTags"),
            favorite = optBoolean("favorite"),
            status = if (status == ShotStatus.DONE) ShotStatus.DONE else ShotStatus.PENDING,
        )
    }

    private companion object {
        const val VERSION = 1
        const val MANIFEST = "memshot.json"
    }
}
