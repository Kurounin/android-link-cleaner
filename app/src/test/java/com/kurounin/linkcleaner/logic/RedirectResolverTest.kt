package com.kurounin.linkcleaner.logic

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RedirectResolverTest {

    private lateinit var server: MockWebServer
    private lateinit var resolver: RedirectResolver

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.SECONDS)
            .build()
        resolver = RedirectResolver(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun follows_single_redirect() = runTest {
        val target = server.url("/final").toString()
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", target))
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/start").toString())

        assertEquals(target, result)
    }

    @Test
    fun follows_chain_of_redirects() = runTest {
        val hop1 = server.url("/hop1").toString()
        val final = server.url("/final").toString()
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", hop1))
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", final))
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/start").toString())

        assertEquals(final, result)
    }

    @Test
    fun relative_location_is_resolved_against_current_url() = runTest {
        val expected = server.url("/final").toString()
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "/final"))
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/start").toString())

        assertEquals(expected, result)
    }

    @Test
    fun stops_when_non_redirect_status() = runTest {
        val start = server.url("/start").toString()
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Location", "/ignored"))

        val result = resolver.resolve(start)

        assertEquals(start, result)
    }

    @Test
    fun falls_back_to_GET_when_HEAD_returns_405() = runTest {
        val final = server.url("/final").toString()
        server.enqueue(MockResponse().setResponseCode(405))
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", final))
        server.enqueue(MockResponse().setResponseCode(200))

        val result = resolver.resolve(server.url("/start").toString())

        assertEquals(final, result)
    }

    @Test
    fun stops_at_max_hops() = runTest {
        repeat(10) {
            val next = server.url("/hop$it").toString()
            server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", next))
        }
        val result = resolver.resolve(server.url("/start").toString(), maxHops = 3)
        assert(result.contains("/hop")) { "expected intermediate URL, got $result" }
    }
}
