package com.somchai.ai.extensions

import java.net.URI
import java.net.URLDecoder

data class WebSearchHit(val title: String, val url: String)

/** Extracts public HTTPS result links from DuckDuckGo's HTML search page. */
internal object WebSearchResultParser {
    fun parse(html: String, pageReader: WebPageReader, limit: Int = 5): List<WebSearchHit> {
        if (html.isBlank() || limit <= 0) return emptyList()
        val anchors = Regex("""(?is)<a\b[^>]*class=["'][^"']*result__a[^"']*["'][^>]*>.*?</a>""")
            .findAll(html)
        val hits = linkedMapOf<String, WebSearchHit>()
        anchors.forEach { match ->
            val anchor = match.value
            val rawHref = Regex("""(?is)\bhref=["']([^"']+)["']""")
                .find(anchor)?.groupValues?.getOrNull(1) ?: return@forEach
            val decodedHref = rawHref.replace("&amp;", "&")
            val destination = unwrapDuckDuckGoRedirect(decodedHref) ?: return@forEach
            val safeUrl = runCatching { pageReader.validateUrl(destination) }.getOrNull() ?: return@forEach
            val title = HtmlTextExtractor.extract(anchor, 180).trim()
            if (title.isNotBlank() && safeUrl !in hits) {
                hits[safeUrl] = WebSearchHit(title, safeUrl)
            }
            if (hits.size >= limit) return hits.values.toList()
        }
        return hits.values.toList()
    }

    private fun unwrapDuckDuckGoRedirect(address: String): String? = runCatching {
        val uri = URI(address)
        if (uri.host?.endsWith("duckduckgo.com", ignoreCase = true) == true) {
            val target = uri.rawQuery.orEmpty().split("&").firstOrNull { it.startsWith("uddg=") }
                ?.substringAfter("uddg=") ?: return null
            URLDecoder.decode(target, "UTF-8")
        } else {
            address
        }
    }.getOrNull()
}
