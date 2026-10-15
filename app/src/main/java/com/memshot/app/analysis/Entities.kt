package com.memshot.app.analysis

enum class EntityType { LINK, EMAIL, PHONE }

data class Entity(val type: EntityType, val value: String)

/** Trova link, email e numeri di telefono nel testo riconosciuto, per offrire azioni rapide. */
object Entities {
    private val URL = Regex("(?i)\\b((https?://)?(www\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)*\\.(com|it|org|net|io|ai|dev|app|co|eu|me|ly|gl|tv|be|info|edu|gov|uk|de|fr|es)(/[^\\s]*)?)")
    private val EMAIL = Regex("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b")
    private val PHONE = Regex("(?<![\\d])(\\+\\d{2}\\s?)?(3\\d{2}|0\\d{1,3})[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}(?![\\d])")

    fun extract(text: String): List<Entity> {
        val out = LinkedHashMap<String, Entity>()
        val emails = EMAIL.findAll(text).map { it.value }.toSet()
        emails.forEach { out[it.lowercase()] = Entity(EntityType.EMAIL, it) }
        URL.findAll(text).map { it.value.trimEnd('.', ',', ')', ';') }
            .filter { url -> emails.none { it.contains(url, ignoreCase = true) } }
            .forEach { out.putIfAbsent(it.lowercase(), Entity(EntityType.LINK, it)) }
        PHONE.findAll(text).map { it.value.trim() }
            .filter { it.count(Char::isDigit) in 9..13 }
            .forEach { out.putIfAbsent(it, Entity(EntityType.PHONE, it)) }
        return out.values.take(8)
    }
}
