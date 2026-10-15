package com.memshot.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("memshot", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean(KEY_ONBOARDED, false)
        set(v) = sp.edit { putBoolean(KEY_ONBOARDED, v) }

    var autoImport: Boolean
        get() = sp.getBoolean(KEY_AUTO, true)
        set(v) = sp.edit { putBoolean(KEY_AUTO, v) }

    var notifyOnImport: Boolean
        get() = sp.getBoolean(KEY_NOTIFY, true)
        set(v) = sp.edit { putBoolean(KEY_NOTIFY, v) }

    /** Dopo l'import, toglie lo screenshot dal telefono (resta solo in MemShot). */
    var deleteAfterImport: Boolean
        get() = sp.getBoolean(KEY_DELETE, true)
        set(v) = sp.edit { putBoolean(KEY_DELETE, v) }

    /** Originali ancora da togliere dalla galleria, in attesa della conferma dell'utente. */
    var pendingDeletes: Set<String>
        get() = sp.getStringSet(KEY_PENDING_DELETES, emptySet()).orEmpty().toSet()
        set(v) = sp.edit { putStringSet(KEY_PENDING_DELETES, v) }

    /** Nel dettaglio scorri in verticale come TikTok invece che in orizzontale come una galleria. */
    var verticalViewer: Boolean
        get() = sp.getBoolean(KEY_VERTICAL, false)
        set(v) = sp.edit { putBoolean(KEY_VERTICAL, v) }

    /** Cosa hai insegnato a MemShot riordinando "Altro": app di provenienza → categoria. */
    var learnedApps: Map<String, String>
        get() = sp.getStringSet(KEY_LEARNED_APPS, emptySet()).orEmpty()
            .mapNotNull { e -> e.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
            .toMap()
        set(v) = sp.edit { putStringSet(KEY_LEARNED_APPS, v.map { "${it.key}=${it.value}" }.toSet()) }

    /** Data (secondi, come MediaStore.DATE_ADDED) dell'ultimo screenshot già visto. */
    var lastScanSeconds: Long
        get() = sp.getLong(KEY_LAST_SCAN, 0L)
        set(v) = sp.edit { putLong(KEY_LAST_SCAN, v) }

    fun changes(): Flow<String> = callbackFlow {
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> trySend(key ?: "") }
        sp.registerOnSharedPreferenceChangeListener(l)
        trySend("")
        awaitClose { sp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_AUTO = "auto_import"
        const val KEY_NOTIFY = "notify_import"
        const val KEY_LAST_SCAN = "last_scan"
        const val KEY_DELETE = "delete_after_import"
        const val KEY_VERTICAL = "vertical_viewer"
        const val KEY_LEARNED_APPS = "learned_apps"
        const val KEY_PENDING_DELETES = "pending_deletes"
    }
}
