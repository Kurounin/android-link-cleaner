package com.kurounin.linkcleaner.logic

import java.net.URI

object YouTubeCleaner {

    private val LONG_HOSTS = setOf("www.youtube.com", "youtube.com", "m.youtube.com")

    fun clean(input: String): CleanResult {
        val uri = runCatching { URI(input) }.getOrElse {
            return CleanResult.Error("Invalid YouTube URL")
        }

        val host = uri.host?.lowercase() ?: return CleanResult.Error("Invalid YouTube URL")
        val query = parseQuery(uri.rawQuery)

        return when {
            host == "youtu.be" -> buildWatch(uri.path.trimStart('/'), query, input)
            host in LONG_HOSTS && uri.path == "/watch" -> {
                val v = query["v"] ?: return CleanResult.Error("YouTube watch URL missing v parameter")
                buildWatch(v, query, input)
            }
            host in LONG_HOSTS && uri.path.startsWith("/shorts/") -> {
                val id = uri.path.removePrefix("/shorts/").substringBefore('/')
                CleanResult.Success(
                    cleanUrl = "https://www.youtube.com/shorts/$id",
                    platform = Platform.YOUTUBE,
                    removed = if (uri.rawQuery.isNullOrEmpty()) "nothing" else "query parameters"
                )
            }
            else -> CleanResult.Error("Unsupported YouTube URL shape")
        }
    }

    private fun buildWatch(videoId: String, query: Map<String, String>, original: String): CleanResult {
        if (videoId.isBlank()) return CleanResult.Error("YouTube URL missing video id")
        val cleanId = videoId.substringBefore('/').substringBefore('?')
        val ts = query["t"]
        val clean = buildString {
            append("https://www.youtube.com/watch?v=")
            append(cleanId)
            if (!ts.isNullOrEmpty()) {
                append("&t=")
                append(ts)
            }
        }
        val removed = describeRemoved(original, clean, query)
        return CleanResult.Success(clean, Platform.YOUTUBE, removed)
    }

    private fun describeRemoved(original: String, clean: String, query: Map<String, String>): String {
        if (original == clean) return "nothing"
        val stripped = query.keys - setOf("v", "t")
        return if (stripped.isEmpty()) "host/path normalized" else "tracking params: ${stripped.joinToString(", ")}"
    }

    private fun parseQuery(raw: String?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split('&')
            .mapNotNull {
                val eq = it.indexOf('=')
                if (eq < 0) it to "" else it.substring(0, eq) to it.substring(eq + 1)
            }
            .toMap()
    }
}
