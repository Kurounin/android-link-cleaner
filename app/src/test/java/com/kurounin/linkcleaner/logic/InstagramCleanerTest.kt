package com.kurounin.linkcleaner.logic

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InstagramCleanerTest {

    private val cleaner = InstagramCleaner(resolver = null)

    private suspend fun ok(input: String, expected: String) {
        val r = cleaner.clean(input)
        assert(r is CleanResult.Success) { "expected Success for $input, got $r" }
        r as CleanResult.Success
        assertEquals(expected, r.cleanUrl)
        assertEquals(Platform.INSTAGRAM, r.platform)
    }

    @Test
    fun reel_strips_igsh() = runTest {
        ok("https://www.instagram.com/reel/Cabc_xyz/?igsh=foo",
           "https://www.instagram.com/reel/Cabc_xyz/")
    }

    @Test
    fun post_strips_utm() = runTest {
        ok("https://www.instagram.com/p/Cabc123/?utm_source=ig_web_copy_link",
           "https://www.instagram.com/p/Cabc123/")
    }

    @Test
    fun reels_alias_path() = runTest {
        ok("https://instagram.com/reels/Cxy/?x=1",
           "https://www.instagram.com/reels/Cxy/")
    }

    @Test
    fun tv_path() = runTest {
        ok("https://www.instagram.com/tv/Ctv_id/",
           "https://www.instagram.com/tv/Ctv_id/")
    }

    @Test
    fun non_instagram_host_returns_error() = runTest {
        val r = cleaner.clean("https://example.com/p/foo/")
        assert(r is CleanResult.Error) { "expected Error, got $r" }
    }

    @Test
    fun share_path_resolves_to_reel() = runTest {
        val resolver = FixedResolver("https://www.instagram.com/reel/Cabc_xyz/?igsh=foo")
        val c = InstagramCleaner(resolver)
        val r = c.clean("https://www.instagram.com/share/ABC/")
        r as CleanResult.Success
        assertEquals("https://www.instagram.com/reel/Cabc_xyz/", r.cleanUrl)
    }
}

internal class FixedResolver(private val target: String) : RedirectResolver() {
    override suspend fun resolve(url: String, maxHops: Int): String = target
}
