package com.kurounin.linkcleaner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun extracts_bare_url() {
        assertEquals("https://example.com/foo", UrlExtractor.extract("https://example.com/foo"))
    }

    @Test
    fun extracts_url_embedded_in_sentence() {
        val input = "check this out https://vm.tiktok.com/ZMabc/ cool right?"
        assertEquals("https://vm.tiktok.com/ZMabc/", UrlExtractor.extract(input))
    }

    @Test
    fun extracts_first_url_when_multiple() {
        val input = "https://first.example https://second.example"
        assertEquals("https://first.example", UrlExtractor.extract(input))
    }

    @Test
    fun accepts_http_scheme() {
        assertEquals("http://plain.example/", UrlExtractor.extract("http://plain.example/"))
    }

    @Test
    fun trims_surrounding_whitespace() {
        assertEquals("https://example.com/", UrlExtractor.extract("   https://example.com/   "))
    }

    @Test
    fun percent_decodes_wholly_encoded_input() {
        val encoded = "https%3A%2F%2Fexample.com%2Ffoo"
        assertEquals("https://example.com/foo", UrlExtractor.extract(encoded))
    }

    @Test
    fun returns_null_when_no_url_found() {
        assertNull(UrlExtractor.extract("not a url at all"))
    }

    @Test
    fun returns_null_for_empty_string() {
        assertNull(UrlExtractor.extract(""))
    }

    @Test
    fun strips_trailing_punctuation() {
        assertEquals("https://example.com/foo", UrlExtractor.extract("See https://example.com/foo."))
        assertEquals("https://example.com/foo", UrlExtractor.extract("(https://example.com/foo)"))
    }
}
