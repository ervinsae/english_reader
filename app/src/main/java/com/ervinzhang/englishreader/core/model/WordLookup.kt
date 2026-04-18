package com.ervinzhang.englishreader.core.model

import java.util.Locale

const val UNKNOWN_MEANING_PLACEHOLDER = "暂无释义"

fun String.normalizeWord(): String {
    val trimmed = trim()
    val normalized = trimmed
        .lowercase(Locale.ROOT)
        .replace(EDGE_PUNCTUATION_REGEX, "")
    return normalized.ifBlank { trimmed.lowercase(Locale.ROOT) }
}

fun String?.hasUsableMeaning(): Boolean {
    return !isNullOrBlank() && this != UNKNOWN_MEANING_PLACEHOLDER
}

fun Word.hasUsableMeaning(): Boolean = meaningZh.hasUsableMeaning()

fun Word.needsLookupFallback(): Boolean {
    return !hasUsableMeaning() || audioUri.isNullOrBlank()
}

private val EDGE_PUNCTUATION_REGEX = Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$")
