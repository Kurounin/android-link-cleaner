package com.kurounin.linkcleaner.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Follows HTTP redirects manually so we can bound hops, swap HEAD→GET on rejection,
 * and respect an overall timeout. Injected client = testability with MockWebServer.
 */
open class RedirectResolver(private val client: OkHttpClient = default()) {

    open suspend fun resolve(url: String, maxHops: Int = 5): String = withContext(Dispatchers.IO) {
        var current = url
        repeat(maxHops) {
            val next = requestAndReadLocation(current) ?: return@withContext current
            current = if (next.startsWith("http", ignoreCase = true)) {
                next
            } else {
                URI(current).resolve(next).toString()
            }
        }
        current
    }

    private fun requestAndReadLocation(url: String): String? {
        val head = execute(Request.Builder().url(url).head().header("User-Agent", UA).build())
        if (head.code == 405 || head.code == 403) {
            head.close()
            val get = execute(Request.Builder().url(url).get().header("User-Agent", UA).build())
            return readLocationAndClose(get)
        }
        return readLocationAndClose(head)
    }

    private fun readLocationAndClose(resp: okhttp3.Response): String? = resp.use {
        if (it.code !in 300..399) null else it.header("Location")
    }

    private fun execute(req: Request) = client.newCall(req).execute()

    companion object {
        private const val UA = "Mozilla/5.0 (Android) LinkCleaner/1.0"
        fun default(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }
}
