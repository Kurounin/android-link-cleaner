package com.kurounin.linkcleaner.logic

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TikTokCleanerTest {

    private val cleaner = TikTokCleaner(resolver = null)

    @Test
    fun canonical_video_strips_query() = runTest {
        val r = cleaner.clean("https://www.tiktok.com/@user/video/12345?_r=1&_t=xyz")
        assert(r is CleanResult.Success) { "got $r" }
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/video/12345", r.cleanUrl)
        assertEquals(Platform.TIKTOK, r.platform)
    }

    @Test
    fun canonical_photo_strips_query() = runTest {
        val r = cleaner.clean("https://www.tiktok.com/@user/photo/99?_r=2")
        assert(r is CleanResult.Success) { "got $r" }
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/photo/99", r.cleanUrl)
    }

    @Test
    fun already_clean_passthrough() = runTest {
        val r = cleaner.clean("https://www.tiktok.com/@user/video/12345")
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/video/12345", r.cleanUrl)
        assertEquals("nothing", r.removed)
    }

    @Test
    fun short_link_without_resolver_returns_error() = runTest {
        val r = cleaner.clean("https://vm.tiktok.com/ZMabc123/")
        assert(r is CleanResult.Error) { "got $r" }
    }

    @Test
    fun non_tiktok_host_returns_error() = runTest {
        val r = cleaner.clean("https://example.com/@x/video/1")
        assert(r is CleanResult.Error) { "got $r" }
    }

    @Test
    fun vm_tiktok_short_resolves_to_canonical() = runTest {
        val resolver = FixedResolver("https://www.tiktok.com/@creator/video/7234567890?_r=1")
        val c = TikTokCleaner(resolver)
        val r = c.clean("https://vm.tiktok.com/ZMabc/")
        assert(r is CleanResult.Success) { "got $r" }
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@creator/video/7234567890", r.cleanUrl)
    }

    @Test
    fun t_path_resolves_to_canonical() = runTest {
        val resolver = FixedResolver("https://www.tiktok.com/@user/photo/42")
        val c = TikTokCleaner(resolver)
        val r = c.clean("https://www.tiktok.com/t/ZZZ/")
        r as CleanResult.Success
        assertEquals("https://www.tiktok.com/@user/photo/42", r.cleanUrl)
    }

    @Test
    fun short_link_resolving_to_non_canonical_returns_error() = runTest {
        val resolver = FixedResolver("https://www.tiktok.com/nonsense")
        val c = TikTokCleaner(resolver)
        val r = c.clean("https://vm.tiktok.com/ZMabc/")
        assert(r is CleanResult.Error) { "got $r" }
    }
}
