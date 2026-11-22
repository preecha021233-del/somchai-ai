package com.somchai.ai.extensions

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ResearchCandidate(
    val source: String,
    val title: String,
    val url: String,
    val snippet: String = ""
)

data class ResearchDiscoveryResult(
    val candidates: List<ResearchCandidate>,
    val sourceErrors: List<String>
)

interface ResearchDiscoveryProvider {
    fun discover(query: String): ResearchDiscoveryResult
}

interface ResearchPageReader {
    fun read(address: String): WebPageContent
}

/** Real multi-source discovery. Search snippets are leads; page reads are the evidence. */
class HttpResearchDiscoveryProvider(
    private val pageReader: WebPageReader = WebPageReader()
) : ResearchDiscoveryProvider {
    override fun discover(query: String): ResearchDiscoveryResult {
        val candidates = mutableListOf<ResearchCandidate>()
        val errors = mutableListOf<String>()

        runCatching { searchInstant(query) }
            .onFailure { errors.add("DuckDuckGo Instant Answer: ${it.javaClass.simpleName}") }
            .getOrNull()?.let { candidates.addAll(it) }

        runCatching { searchWikipedia(query) }
            .onFailure { errors.add("Wikipedia: ${it.javaClass.simpleName}") }
            .getOrNull()?.let { candidates.addAll(it) }

        runCatching { searchWeb(query) }
            .onFailure { errors.add("DuckDuckGo Web: ${it.javaClass.simpleName}") }
            .getOrNull()?.let { candidates.addAll(it) }

        val unique = linkedMapOf<String, ResearchCandidate>()
        candidates.forEach { candidate ->
            val safe = runCatching { pageReader.validateUrl(candidate.url) }.getOrNull() ?: return@forEach
            if (candidate.title.isNotBlank()) unique.putIfAbsent(safe, candidate.copy(url = safe))
        }
        return ResearchDiscoveryResult(unique.values.take(10), errors)
    }

    private fun searchInstant(query: String): List<ResearchCandidate> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val root = getJson("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
        val result = mutableListOf<ResearchCandidate>()
        root.optString("AbstractURL").takeIf { it.startsWith("https://") }?.let { address ->
            result.add(ResearchCandidate(
                "DuckDuckGo Instant Answer",
                root.optString("Heading").ifBlank { root.optString("AbstractSource").ifBlank { "Instant Answer" } },
                address,
                root.optString("AbstractText")
            ))
        }
        root.optJSONArray("RelatedTopics")?.let { topics ->
            for (i in 0 until minOf(topics.length(), 8)) {
                val item = topics.optJSONObject(i) ?: continue
                val address = item.optString("FirstURL").takeIf { it.startsWith("https://") } ?: continue
                result.add(ResearchCandidate(
                    "DuckDuckGo Related Topic",
                    item.optString("Text").take(180).ifBlank { "Related topic" },
                    address,
                    item.optString("Text")
                ))
            }
        }
        return result
    }

    private fun searchWikipedia(query: String): List<ResearchCandidate> {
        val language = if (query.any { it in '\u0E00'..'\u0E7F' }) "th" else "en"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val root = getJson(
            "https://$language.wikipedia.org/w/api.php?action=query&list=search" +
                "&srsearch=$encoded&format=json&utf8=1&srlimit=5&srprop=timestamp"
        )
        val results = root.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return (0 until minOf(results.length(), 5)).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val snippet = item.optString("snippet")
                .replace(Regex("<[^>]*>"), " ")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            val address = "https://$language.wikipedia.org/wiki/" +
                URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
            ResearchCandidate("Wikipedia ($language)", title, address, snippet)
        }
    }

    private fun searchWeb(query: String): List<ResearchCandidate> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection = URL("https://html.duckduckgo.com/html/?q=$encoded")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "text/html")
            val code = connection.responseCode
            check(code in 200..299) { "DuckDuckGo Web returned HTTP $code" }
            val html = readBounded(connection.inputStream)

            return WebSearchResultParser.parse(html, pageReader, limit = 8).map {
                ResearchCandidate("DuckDuckGo Web Search", it.title, it.url)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            check(code in 200..299) { "Research source returned HTTP $code" }
            val body = readBounded(connection.inputStream)
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        input.use { stream ->
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_SEARCH_RESPONSE_CHARS) {
                    "Research response exceeds size limit"
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_SEARCH_RESPONSE_CHARS = 1_000_000
        private const val USER_AGENT = "SOMCHAI-AI/2.0 (multi-source research)"
    }
}
