package com.kurounin.linkcleaner.logic

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkCleanerTest {

    private class Resolver(private val target: String) : RedirectResolver() {
        override suspend fun resolve(url: String, maxHops: Int): String = target
    }

    private fun cleaner(resolved: String = ""): LinkCleaner = LinkCleaner(Resolver(resolved))

    @Test
    fun tiktok_short_resolves_to_canonical() = runTest {
        val c = cleaner("https://www.tiktok.com/@user/video/1234567890?_r=1&_t=xyz")
        val r = c.cleanLink("https://vm.tiktok.com/ZMabc123/")
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/video/1234567890", r.cleanUrl)
        assertEquals(Platform.TIKTOK, r.platform)
    }

    @Test
    fun tiktok_canonical_strips_query() = runTest {
        val r = cleaner().cleanLink("https://www.tiktok.com/@user/video/12345?_r=1&_t=xyz")
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/video/12345", r.cleanUrl)
    }

    @Test
    fun instagram_reel_strips_igsh() = runTest {
        val r = cleaner().cleanLink("https://www.instagram.com/reel/Cabc_xyz/?igsh=foo")
        r as CleanResult.Success
        assertEquals("https://www.instagram.com/reel/Cabc_xyz/", r.cleanUrl)
    }

    @Test
    fun youtube_short_strips_si() = runTest {
        val r = cleaner().cleanLink("https://youtu.be/dQw4w9WgXcQ?si=trackingtoken")
        r as CleanResult.Success
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", r.cleanUrl)
    }

    @Test
    fun youtube_long_keeps_v_and_t() = runTest {
        val r = cleaner().cleanLink("https://www.youtube.com/watch?v=abc&si=xyz&t=42")
        r as CleanResult.Success
        assertEquals("https://www.youtube.com/watch?v=abc&t=42", r.cleanUrl)
    }

    @Test
    fun unknown_host_strips_tracking_returns_unchanged_wrapper() = runTest {
        val r = cleaner().cleanLink("https://example.com/foo?utm_source=x")
        r as CleanResult.Unchanged
        assertEquals("https://example.com/foo", r.url)
    }

    @Test
    fun garbage_input_returns_error() = runTest {
        val r = cleaner().cleanLink("not a url at all")
        r as CleanResult.Error
        assertEquals("No URL found", r.message)
    }

    @Test
    fun free_text_with_embedded_url_works() = runTest {
        val r = cleaner().cleanLink("check this https://youtu.be/dQw4w9WgXcQ?si=abc cool")
        r as CleanResult.Success
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", r.cleanUrl)
    }
}
