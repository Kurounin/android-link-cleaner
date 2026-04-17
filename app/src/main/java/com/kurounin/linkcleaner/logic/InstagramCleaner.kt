package com.kurounin.linkcleaner.logic

import java.net.URI

class InstagramCleaner(private val resolver: RedirectResolver?) {

    private val canonicalPrefixes = listOf("/p/", "/reel/", "/reels/", "/tv/")

    suspend fun clean(input: String): CleanResult {
        val uri = runCatching { URI(input) }.getOrElse {
            return CleanResult.Error("Invalid Instagram URL")
        }
        val host = uri.host?.lowercase()
        if (host != "instagram.com" && host != "www.instagram.com") {
            return CleanResult.Error("Not an Instagram URL")
        }

        val resolvedInput = if (uri.path.startsWith("/share/")) {
            if (resolver == null) return CleanResult.Error("/share/ needs network resolution")
            try {
                resolver.resolve(input)
            } catch (e: Exception) {
                return CleanResult.Error("Could not resolve /share/ link — ${e.message ?: "check connection"}")
            }
        } else {
            input
        }

        val resolvedUri = runCatching { URI(resolvedInput) }.getOrElse {
            return CleanResult.Error("Resolved URL was invalid")
        }
        val path = resolvedUri.path

        if (canonicalPrefixes.none { path.startsWith(it) }) {
            return CleanResult.Error("Unsupported Instagram path: $path")
        }

        val normalizedPath = if (path.endsWith("/")) path else "$path/"
        val clean = "https://www.instagram.com$normalizedPath"
        val removed = if (resolvedUri.rawQuery.isNullOrEmpty() && input == resolvedInput) "nothing"
                      else "query parameters / redirect trail"
        return CleanResult.Success(clean, Platform.INSTAGRAM, removed)
    }
}
