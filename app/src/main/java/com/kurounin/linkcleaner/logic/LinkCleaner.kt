package com.kurounin.linkcleaner.logic

import com.kurounin.linkcleaner.util.UrlExtractor
import java.net.URI

sealed class CleanResult {
    data class Success(val cleanUrl: String, val platform: Platform, val removed: String) : CleanResult()
    data class Unchanged(val url: String, val reason: String) : CleanResult()
    data class Error(val message: String) : CleanResult()
}

enum class Platform { TIKTOK, INSTAGRAM, YOUTUBE, OTHER }

/** Tracking params stripped from unknown-platform URLs (spec §5.4, §8.3). */
internal val TRACKING_PARAMS = setOf(
    "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
    "fbclid", "gclid", "mc_eid", "igshid", "si", "_r", "_t",
    "is_from_webapp", "sender_device", "sender_web_id",
    "share_app_id", "share_link_id", "share_iid", "ttclid",
    "feature", "pp"
)

class LinkCleaner(
    private val resolver: RedirectResolver = RedirectResolver()
) {
    private val tiktok = TikTokCleaner(resolver)
    private val instagram = InstagramCleaner(resolver)

    suspend fun cleanLink(input: String): CleanResult {
        val url = UrlExtractor.extract(input) ?: return CleanResult.Error("No URL found")
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull()
            ?: return CleanResult.Error("Invalid URL")

        return when {
            host == "vm.tiktok.com" || host == "vt.tiktok.com" ||
                host == "www.tiktok.com" || host == "tiktok.com" || host == "m.tiktok.com" ->
                tiktok.clean(url)
            host == "instagram.com" || host == "www.instagram.com" ->
                instagram.clean(url)
            host == "youtu.be" || host == "www.youtube.com" ||
                host == "youtube.com" || host == "m.youtube.com" ->
                YouTubeCleaner.clean(url)
            else -> stripTrackingFromUnknown(url)
        }
    }

    private fun stripTrackingFromUnknown(url: String): CleanResult {
        val uri = URI(url)
        val cleanedQuery = uri.rawQuery
            ?.split('&')
            ?.filter { pair ->
                val key = pair.substringBefore('=')
                key.isNotEmpty() && key !in TRACKING_PARAMS
            }
            ?.joinToString("&")
            ?.ifEmpty { null }

        val rebuilt = buildString {
            append(uri.scheme).append("://").append(uri.host)
            if (uri.port != -1) append(":").append(uri.port)
            append(uri.rawPath ?: "")
            if (!cleanedQuery.isNullOrEmpty()) append('?').append(cleanedQuery)
        }
        return CleanResult.Unchanged(rebuilt, "Unknown platform — no transformation applied")
    }
}

/** Top-level entry point matching the signature in spec §5. */
suspend fun cleanLink(input: String): CleanResult = DEFAULT_CLEANER.cleanLink(input)

private val DEFAULT_CLEANER by lazy { LinkCleaner() }
