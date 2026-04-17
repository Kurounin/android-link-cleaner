package com.kurounin.linkcleaner.logic

import java.net.URI

class TikTokCleaner(private val resolver: RedirectResolver?) {

    private val shortHosts = setOf("vm.tiktok.com", "vt.tiktok.com")
    private val mainHosts = setOf("www.tiktok.com", "tiktok.com", "m.tiktok.com")
    private val canonicalRegex = Regex("""^/@[^/]+/(video|photo)/\d+/?$""")

    suspend fun clean(input: String): CleanResult {
        val uri = runCatching { URI(input) }.getOrElse {
            return CleanResult.Error("Invalid TikTok URL")
        }
        val host = uri.host?.lowercase() ?: return CleanResult.Error("Invalid TikTok URL")

        val needsResolve = host in shortHosts || (host in mainHosts && uri.path.startsWith("/t/"))
        val resolved: String = if (needsResolve) {
            if (resolver == null) return CleanResult.Error("Short link needs network resolution")
            try {
                resolver.resolve(input)
            } catch (e: Exception) {
                return CleanResult.Error("Could not resolve short link — ${e.message ?: "check connection"}")
            }
        } else {
            input
        }

        val resolvedUri = runCatching { URI(resolved) }.getOrElse {
            return CleanResult.Error("Resolved URL was invalid: $resolved")
        }
        val resolvedHost = resolvedUri.host?.lowercase()

        return when {
            resolvedHost in mainHosts && canonicalRegex.matches(resolvedUri.path) ->
                canonicalSuccess(resolvedUri)
            resolvedHost in mainHosts ->
                CleanResult.Error("Resolved URL did not match canonical TikTok shape: ${resolvedUri.path}")
            else ->
                CleanResult.Error("Not a TikTok URL after resolve")
        }
    }

    private fun canonicalSuccess(uri: URI): CleanResult.Success {
        val path = uri.path.trimEnd('/')
        val clean = "https://www.tiktok.com$path"
        val removed = if (uri.rawQuery.isNullOrEmpty()) "nothing" else "query parameters (${uri.rawQuery})"
        return CleanResult.Success(clean, Platform.TIKTOK, removed)
    }
}
