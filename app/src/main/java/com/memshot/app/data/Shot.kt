package com.memshot.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ShotStatus { PENDING, DONE, FAILED }

@Entity(
    tableName = "shots",
    indices = [Index(value = ["sourceKey"], unique = true), Index("createdAt")],
)
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Chiave per evitare doppioni (nome file + dimensione). */
    val sourceKey: String,
    /** Copia privata dell'immagine dentro l'app: sopravvive anche se cancelli lo screenshot dalla galleria. */
    val filePath: String,
    val width: Int = 0,
    val height: Int = 0,
    /** Quando è stato fatto lo screenshot. */
    val createdAt: Long,
    val importedAt: Long = System.currentTimeMillis(),
    /** App da cui è stato fatto lo screenshot, se ricavabile dal nome file. */
    val sourceApp: String? = null,
    val ocrText: String = "",
    /** Cosa ML Kit "vede" nell'immagine (es. Food, Sky): aiuta la ricerca e la classificazione. */
    val visualLabels: List<String> = emptyList(),
    /** Tag assegnati automaticamente (id categoria). */
    val autoTags: List<String> = emptyList(),
    /** Tag aggiunti a mano dall'utente. */
    val userTags: List<String> = emptyList(),
    /** Tag automatici rimossi a mano: non vengono riassegnati alla rianalisi. */
    val removedTags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val status: ShotStatus = ShotStatus.PENDING,
) {
    val tags: List<String> get() = (autoTags.filterNot { it in removedTags } + userTags).distinct()
    val aspectRatio: Float get() = if (width > 0 && height > 0) width.toFloat() / height else 9f / 19.5f
}
