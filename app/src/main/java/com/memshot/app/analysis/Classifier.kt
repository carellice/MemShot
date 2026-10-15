package com.memshot.app.analysis

import java.text.Normalizer
import kotlin.math.min

data class VisualLabel(val text: String, val confidence: Float)

/**
 * Classificatore a punteggio: ogni categoria accumula punti da parole chiave nel testo OCR,
 * dall'app di provenienza, da etichette visive e da euristiche specifiche (prezzi, orari, codice…).
 * Gira tutto sul telefono, è istantaneo e non manda niente in rete.
 */
class Classifier {

    private class Compiled(val category: Category, val keywords: List<Pair<Regex, Float>>, val apps: List<Regex>)

    private val compiled = Categories.all.map { c ->
        Compiled(
            category = c,
            keywords = c.keywords.map { (k, w) -> pattern(k) to w },
            apps = c.apps.map { pattern(it) },
        )
    }

    fun classify(
        ocrText: String,
        labels: List<VisualLabel>,
        sourceApp: String?,
        learnedTag: String? = null,
    ): List<String> {
        val rawText = stripStatusBar(ocrText)
        val text = rawText.normalizeForSearch()
        val app = sourceApp?.normalizeForSearch().orEmpty()
        val wordCount = WORD.findAll(text).count()
        val scores = HashMap<String, Float>()
        fun add(id: String, v: Float) { scores[id] = (scores[id] ?: 0f) + v }

        for (c in compiled) {
            var s = 0f
            for ((re, w) in c.keywords) {
                val n = re.findAll(text).take(3).count()
                if (n > 0) s += w * (1f + 0.25f * (n - 1))
            }
            if (app.isNotEmpty() && c.apps.any { it.containsMatchIn(app) }) s += 5f
            for (l in labels) {
                c.category.imageLabels[l.text.lowercase()]?.let { s += it * l.confidence }
            }
            if (s > 0f) add(c.category.id, s)
        }

        // Categoria che hai scelto tu per altri screenshot della stessa app
        if (learnedTag != null) add(learnedTag, 5f)

        // ---- Euristiche specifiche ----
        val placeholders = PLACEHOLDER.findAll(rawText).take(4).count()
        if (placeholders > 0) add("prompt", 1.5f * placeholders)
        val firstWord = WORD.find(text)?.value
        if (firstWord != null && firstWord in IMPERATIVES && wordCount > 12) add("prompt", 4f)
        if (LENGTH_LIMIT.containsMatchIn(text)) add("prompt", 2.5f)
        if (wordCount > 40 && SECOND_PERSON.findAll(text).count() >= 3 && IMPERATIVE_ANY.findAll(text).count() >= 2) add("prompt", 2f)

        val symbols = rawText.count { it in "{}();=<>[]" }
        if (rawText.length > 40 && symbols.toFloat() / rawText.length > 0.04f) add("code", 3.5f)
        if (CODE_LINE.findAll(rawText).count() >= 2) add("code", 2f)

        if (DURATION.containsMatchIn(text)) add("film", 2f)
        if (SEASON_EP.containsMatchIn(text)) add("film", 3f)

        val times = TIME.findAll(text).count()
        if (times >= 4) add("chat", min(times, 10) * 0.6f)

        val prices = PRICE.findAll(rawText).take(6).count()
        if (prices > 0) { add("shopping", prices * 0.8f); add("finance", prices * 0.5f) }

        val quantities = QUANTITY.findAll(text).take(6).count()
        if (quantities >= 2) add("recipe", quantities * 0.8f)

        if (wordCount in 4..60 && QUOTES.containsMatchIn(rawText)) add("quotes", 2.5f)
        if (wordCount in 4..80 && ATTRIBUTION.containsMatchIn(rawText)) add("quotes", 2.5f)

        if (wordCount < 4) add(Categories.PHOTO, 3f + labels.size * 0.3f)

        // ---- Scelta dei tag ----
        val ranked = scores.entries.sortedByDescending { it.value }
        val best = ranked.firstOrNull()?.value ?: 0f
        val tags = ranked
            .filter { it.value >= THRESHOLD && it.value >= best * 0.5f }
            .take(3)
            .map { it.key }
        // Poche parole = foto/immagine; testo che non so collocare = "Altro", da riordinare a mano
        return tags.ifEmpty { listOf(if (wordCount < 4) Categories.PHOTO else Categories.OTHER) }
    }

    /** Toglie le righe iniziali della barra di stato (ora, batteria, rete) che l'OCR legge sempre. */
    private fun stripStatusBar(text: String): String {
        val lines = text.lines()
        val skip = lines.takeWhile { line ->
            val l = line.trim()
            l.isEmpty() || STATUS_LINE.matches(l) || (l.length <= 4 && l.none { it.isLetter() })
        }.size
        return lines.drop(skip).joinToString("\n")
    }

    private fun pattern(keyword: String): Regex {
        val prefix = keyword.endsWith("*")
        val k = Normalizer.normalize(keyword.removeSuffix("*").lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        val body = Regex.escape(k) + if (prefix) "[\\p{L}]*" else ""
        return Regex("(?<![\\p{L}\\p{N}])$body(?![\\p{L}\\p{N}])")
    }

    private companion object {
        const val THRESHOLD = 3f
        val WORD = Regex("[\\p{L}\\p{N}]{2,}")
        val PLACEHOLDER = Regex("\\[[^\\]\\n]{3,40}\\]|\\{[^\\}\\n]{3,40}\\}|<[a-zA-Z][^>\\n]{2,30}>")
        val IMPERATIVES = setOf(
            "scrivi", "crea", "genera", "agisci", "immagina", "spiega", "riassumi", "traduci", "analizza", "elenca",
            "rispondi", "fornisci", "descrivi", "aiutami", "comportati", "voglio", "write", "create", "generate", "act",
            "imagine", "explain", "summarize", "translate", "analyze", "list", "provide", "describe", "help", "you", "sei",
            "pretend", "rewrite", "riscrivi", "migliora", "improve", "design", "progetta",
        )
        val SECOND_PERSON = Regex("(?<![\\p{L}])(you|your|tu|tuo|tua|devi|dovrai|sei)(?![\\p{L}])")
        val IMPERATIVE_ANY = Regex("(?<![\\p{L}])(scrivi|crea|genera|usa|evita|includi|use|avoid|include|make sure|assicurati|non usare|do not)(?![\\p{L}])")
        val LENGTH_LIMIT = Regex("(massimo|max|al massimo|in|under|maximum|at most)\\s+\\d+\\s+(parole|words|caratteri|characters|righe|frasi|sentences|paragrafi|paragraphs)")
        val CODE_LINE = Regex("(?m)^\\s*(fun|def|class|import|return|if|for|val|var|const|let|public|private)\\b.*[({:;=]\\s*$")
        val DURATION = Regex("(?<![\\p{N}])\\d\\s?h\\s?\\d{1,2}\\s?m(in)?(?![\\p{L}])")
        val SEASON_EP = Regex("(?<![\\p{L}])s\\d{1,2}\\s?[:.]?\\s?e\\d{1,2}(?![\\p{N}])|stagione \\d|season \\d")
        val TIME = Regex("(?<![\\p{N}:])\\d{1,2}:\\d{2}(?![\\p{N}:])")
        val PRICE = Regex("[€$£]\\s?\\d+([.,]\\d{2})?|\\d+[.,]\\d{2}\\s?[€$£]|\\d+\\s?(euro|eur)\\b")
        val QUANTITY = Regex("(?<![\\p{L}\\p{N}])\\d+\\s?(g|gr|kg|ml|cl|l|cucchiai|cucchiaini|tazze|cups|tbsp|tsp)(?![\\p{L}])")
        val QUOTES = Regex("[“”«»„]|^\\s*\"|\"\\s*[-–—]")
        val ATTRIBUTION = Regex("(?m)^\\s*[-–—]\\s*\\p{Lu}[\\p{L}.']+(\\s+\\p{Lu}[\\p{L}.']+){0,3}\\s*$")
        val STATUS_LINE = Regex("\\d{1,2}[:.]\\d{2}(\\s*(am|pm|AM|PM))?(\\s+.{0,12})?")
    }
}
