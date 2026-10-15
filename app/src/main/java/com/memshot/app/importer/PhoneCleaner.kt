package com.memshot.app.importer

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.memshot.app.data.Prefs

/**
 * Toglie dal telefono gli screenshot già copiati al sicuro in MemShot.
 *
 * - Con "Accesso a tutti i file" li sposta nel cestino del telefono in silenzio, anche in background.
 * - Senza, li mette in coda e Android chiede una conferma (un solo dialogo per tutti) appena apri MemShot.
 *
 * Finiscono nel cestino di sistema, dove restano recuperabili per 30 giorni.
 */
class PhoneCleaner(private val context: Context, private val prefs: Prefs) {

    private val resolver get() = context.contentResolver

    fun canDeleteSilently(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    suspend fun removeOriginals(uris: List<Uri>) {
        if (!prefs.deleteAfterImport || uris.isEmpty()) return
        val failed = if (canDeleteSilently()) uris.filterNot { trash(it) } else uris
        if (failed.isNotEmpty()) prefs.pendingDeletes = prefs.pendingDeletes + failed.map { it.toString() }
    }

    private fun trash(uri: Uri): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) }
            resolver.update(uri, values, null, null) > 0 || resolver.delete(uri, null, null) > 0
        } else {
            resolver.delete(uri, null, null) > 0
        }
    }.getOrDefault(false)

    /**
     * Dialogo di sistema per spostare nel cestino gli screenshot in coda.
     * Restituisce null se non c'è nulla da chiedere.
     */
    fun pendingRequest(): IntentSender? {
        if (!prefs.deleteAfterImport || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = prefs.pendingDeletes.map(Uri::parse).filter(::stillInGallery)
        if (uris.isEmpty()) {
            prefs.pendingDeletes = emptySet()
            return null
        }
        if (canDeleteSilently()) {
            val left = uris.filterNot { trash(it) }
            prefs.pendingDeletes = left.map { it.toString() }.toSet()
            if (left.isEmpty()) return null
            return MediaStore.createTrashRequest(resolver, left, true).intentSender
        }
        return MediaStore.createTrashRequest(resolver, uris, true).intentSender
    }

    fun clearPending() { prefs.pendingDeletes = emptySet() }

    private fun stillInGallery(uri: Uri): Boolean = runCatching {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { it.count > 0 } ?: false
    }.getOrDefault(false)

    data class MediaInfo(val name: String?, val size: Long, val takenAt: Long?)

    fun describe(uri: Uri): MediaInfo? = runCatching {
        resolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATE_TAKEN),
            null, null, null,
        )?.use { c ->
            if (!c.moveToFirst()) null
            else MediaInfo(c.getString(0), if (c.isNull(1)) -1 else c.getLong(1), if (c.isNull(2)) null else c.getLong(2).takeIf { it > 0 })
        }
    }.getOrNull()

    /**
     * Trova l'originale nella galleria anche quando arriva da "Condividi" o dal selettore foto
     * (che danno URI diversi da quelli della galleria).
     */
    fun findInGallery(uri: Uri, displayName: String?, size: Long): Uri? {
        if (!ScreenshotImporter.hasMediaPermission(context)) return null
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val segments = uri.pathSegments
        if (uri.authority == MediaStore.AUTHORITY) {
            if (segments.firstOrNull()?.startsWith("picker") == true) {
                // Selettore foto: per le foto locali l'ultimo pezzo dell'URI è l'id nella galleria
                segments.lastOrNull()?.toLongOrNull()?.let { id ->
                    val candidate = ContentUris.withAppendedId(collection, id)
                    val info = describe(candidate)
                    if (info != null && (size <= 0 || info.size == size)) return candidate
                }
            } else if ("images" in segments || "file" in segments) {
                return uri
            }
        }
        if (displayName == null || size <= 0) return null
        return runCatching {
            resolver.query(
                collection,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.SIZE} = ?",
                arrayOf(displayName, size.toString()),
                null,
            )?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null }
        }.getOrNull()
    }
}
