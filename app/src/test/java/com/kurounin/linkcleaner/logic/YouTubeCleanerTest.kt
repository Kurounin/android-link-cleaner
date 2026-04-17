package com.kurounin.linkcleaner.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeCleanerTest {

    private fun ok(input: String, expected: String) {
        val r = YouTubeCleaner.clean(input)
        assert(r is CleanResult.Success) { "expected Success for $input, got $r" }
        r as CleanResult.Success
        assertEquals(expected, r.cleanUrl)
        assertEquals(Platform.YOUTUBE, r.platform)
    }

    @Test
    fun short_youtu_be_strips_si() {
        ok("https://youtu.be/dQw4w9WgXcQ?si=trackingtoken", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    }

    @Test
    fun short_youtu_be_preserves_timestamp() {
        ok("https://youtu.be/dQw4w9WgXcQ?si=tok&t=42", "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42")
    }

    @Test
    fun long_watch_strips_si_and_pp_keeps_v_and_t() {
        ok("https://www.youtube.com/watch?v=abc&si=xyz&t=42", "https://www.youtube.com/watch?v=abc&t=42")
    }

    @Test
    fun long_watch_no_timestamp() {
        ok("https://www.youtube.com/watch?v=abc&si=xyz", "https://www.youtube.com/watch?v=abc")
    }

    @Test
    fun shorts_strips_query() {
        ok("https://www.youtube.com/shorts/abc123?feature=share", "https://www.youtube.com/shorts/abc123")
    }

    @Test
    fun already_clean_is_passthrough() {
        ok("https://www.youtube.com/watch?v=abc", "https://www.youtube.com/watch?v=abc")
    }

    @Test
    fun missing_v_parameter_returns_error() {
        val r = YouTubeCleaner.clean("https://www.youtube.com/watch?si=x")
        assert(r is CleanResult.Error) { "expected Error but got $r" }
    }
}
