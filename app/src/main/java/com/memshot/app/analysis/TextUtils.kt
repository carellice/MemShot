package com.memshot.app.analysis

import java.text.Normalizer

private val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
private val SPACES = "\\s+".toRegex()

/** Minuscolo, senza accenti, spazi compattati: così "Città" trova anche "citta". */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(SPACES, " ")
        .trim()
