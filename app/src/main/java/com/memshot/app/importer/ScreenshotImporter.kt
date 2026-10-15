package com.memshot.app.importer

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.memshot.app.data.Shot
import com.memshot.app.data.ShotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Copia gli screenshot dentro MemShot (galleria, condivisione o selezione manuale) evitando doppioni. */
class ScreenshotImporter(
    private val context: Context,
    private val dao: ShotDao,
    private val cleaner: PhoneCleaner,
) {

    private val dir = File(context.filesDir, "shots").apply { mkdirs() }

    data class ScanResult(val ids: List<Long>, val newestSeconds: Long)

    /** Esito di un import: [stored] = la copia è al sicuro in MemShot (nuova o già presente). */
    private data class Outcome(val id: Long?, val stored: Boolean, val original: Uri?)

    /**
     * Importa le immagini e poi, se l'opzione è attiva, toglie gli originali dal telefono.
     * Restituisce gli id dei nuovi screenshot.
     */
    suspend fun import(uris: List<Uri>): List<Long> {
        val outcomes = uris.map { importOne(it) }
        cleaner.removeOriginals(outcomes.filter { it.stored }.mapNotNull { it.original })
        return outcomes.mapNotNull { it.id }
    }

    private suspend fun importOne(
        uri: Uri,
        takenAtMillis: Long? = null,
        addedAtMillis: Long? = null,
        mediaUri: Uri? = null,
    ): Outcome = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0)
                    size = if (c.isNull(1)) -1L else c.getLong(1)
                }
            }
        }
        val original = mediaUri ?: cleaner.findInGallery(uri, name, size)
        // Il selettore foto può restituire nomi "finti": quando troviamo l'originale usiamo i suoi dati veri
        val info = original?.let { cleaner.describe(it) }
        val displayName = info?.name ?: name ?: uri.lastPathSegment ?: UUID.randomUUID().toString()
        if (info != null && info.size > 0) size = info.size
        val takenAt = takenAtMillis ?: info?.takenAt
        val key = "$displayName|$size"
        if (dao.exists(key)) return@withContext Outcome(null, stored = true, original = original)

        val ext = displayName.substringAfterLast('.', "png").lowercase().take(5)
        val target = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!copied || target.length() == 0L) {
            target.delete()
            return@withContext Outcome(null, stored = false, original = null)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.path, bounds)
        if (bounds.outWidth <= 0) {
            target.delete()
            return@withContext Outcome(null, stored = false, original = null)
        }

        val id = dao.insert(
            Shot(
                sourceKey = key,
                filePath = target.path,
                width = bounds.outWidth,
                height = bounds.outHeight,
                createdAt = takenAt ?: parseDate(displayName) ?: addedAtMillis ?: System.currentTimeMillis(),
                sourceApp = parseSourceApp(displayName),
            )
        )
        if (id <= 0) {
            target.delete()
            Outcome(null, stored = dao.exists(key), original = original)
        } else Outcome(id, stored = true, original = original)
    }

    /** Cerca nella galleria gli screenshot aggiunti dopo [sinceSeconds] e li importa. */
    suspend fun scanScreenshots(sinceSeconds: Long): ScanResult = withContext(Dispatchers.IO) {
        if (!hasMediaPermission(context)) return@withContext ScanResult(emptyList(), sinceSeconds)
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ? AND (" +
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR " +
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ? OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?)"
        val args = arrayOf(sinceSeconds.toString(), "%Screenshot%", "%Screenshot%", "Screenshot%")
        val found = mutableListOf<Triple<Uri, Long, Long?>>()
        context.contentResolver.query(
            collection, projection, selection, args, "${MediaStore.Images.Media.DATE_ADDED} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, c.getLong(0))
                val taken = if (c.isNull(2)) null else c.getLong(2).takeIf { it > 0 }
                found += Triple(uri, c.getLong(1), taken)
            }
        }
        var newest = sinceSeconds
        val outcomes = found.map { (uri, added, taken) ->
            newest = maxOf(newest, added)
            importOne(uri, taken, added * 1000, mediaUri = uri)
        }
        cleaner.removeOriginals(outcomes.filter { it.stored }.mapNotNull { it.original })
        ScanResult(outcomes.mapNotNull { it.id }, newest)
    }

    /** Quanti screenshot ci sono già in galleria (per proporre l'import iniziale). */
    suspend fun countScreenshots(): Int = withContext(Dispatchers.IO) {
        if (!hasMediaPermission(context)) return@withContext 0
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR " +
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), selection,
            arrayOf("%Screenshot%", "%Screenshot%", "Screenshot%"), null,
        )?.use { it.count } ?: 0
    }

    companion object {
        fun mediaPermission(): String =
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE

        fun hasMediaPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, mediaPermission()) == PackageManager.PERMISSION_GRANTED

        private val DATE = Regex("(20\\d{2})[-_]?(\\d{2})[-_]?(\\d{2})[-_ ]?(\\d{2})[-_.]?(\\d{2})[-_.]?(\\d{2})")

        fun parseDate(name: String): Long? {
            val m = DATE.find(name) ?: return null
            val (y, mo, d, h, mi, s) = m.destructured
            return runCatching {
                java.util.Calendar.getInstance().apply {
                    set(y.toInt(), mo.toInt() - 1, d.toInt(), h.toInt(), mi.toInt(), s.toInt())
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.getOrNull()
        }

        /**
         * Molti telefoni mettono l'app nel nome del file:
         * Samsung "Screenshot_20241012_101010_Netflix.jpg", Xiaomi "Screenshot_2024-10-12-10-10-10-123_com.netflix.mediaclient.jpg".
         */
        fun parseSourceApp(name: String): String? {
            val base = name.substringBeforeLast('.')
            val tail = base.substringAfterLast('_', "")
            if (tail.isBlank() || tail.none { it.isLetter() } || tail.equals(base, true)) return null
            if (tail.startsWith("Screenshot", true)) return null
            return if (tail.count { it == '.' } >= 1) {
                // nome pacchetto: com.netflix.mediaclient → netflix
                val parts = tail.split('.').filter { it !in setOf("com", "org", "net", "it", "android", "app", "apps", "google") }
                parts.firstOrNull()?.let { "$it ${tail}" }
            } else tail
        }
    }
}
