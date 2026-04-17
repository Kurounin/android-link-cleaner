package com.kurounin.linkcleaner.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object UrlExtractor {

    private val URL_REGEX = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
    private val FULLY_ENCODED = Regex("""^https?%3[aA]%2[fF]%2[fF].*$""")
    private val TRAILING_TRIM = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'')

    /** Extracts the first http(s) URL from free text. Returns null when none found. */
    fun extract(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val candidate = if (FULLY_ENCODED.matches(trimmed)) {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8)
        } else {
            trimmed
        }

        val match = URL_REGEX.find(candidate) ?: return null
        return match.value.trimEnd(*TRAILING_TRIM)
    }
}
