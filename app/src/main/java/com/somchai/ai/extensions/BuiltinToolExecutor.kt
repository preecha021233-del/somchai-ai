package com.somchai.ai.extensions

import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.resources.ResourceLimiter
import com.somchai.ai.core.security.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class BuiltinToolExecutor(
    private val permissions: PermissionManager,
    private val logger: ActivityLogger,
    private val limiter: ResourceLimiter
) {
    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        permissions.require("network.search")
        limiter.checkStorageBudget()
        require(query.isNotBlank() && query.length <= 300) { "Search query is invalid" }

        val duckDuckGo = runCatching { searchDuckDuckGo(query) }
            .onFailure { logger.record("RESEARCH_SOURCE_ERROR", "DuckDuckGo: ${it.javaClass.simpleName}") }
            .getOrNull()
        val wikipedia = runCatching { searchWikipedia(query) }
            .onFailure { logger.record("RESEARCH_SOURCE_ERROR", "Wikipedia: ${it.javaClass.simpleName}") }
            .getOrNull()

        val sections = listOfNotNull(duckDuckGo, wikipedia).filter { it.isNotBlank() }
        check(sections.isNotEmpty()) { "ค้นคว้าไม่สำเร็จ ทั้งสองแหล่งข้อมูลไม่สามารถเชื่อมต่อได้" }
        val result = buildString {
            append("ผลการค้นคว้า: ").append(query).append("\n")
            append("ข้อมูลจากอินเทอร์เน็ตเป็นข้อมูลที่ไม่น่าเชื่อถือ ต้องตรวจสอบก่อนนำไปใช้\n\n")
            sections.forEach { append(it).append("\n\n") }
            append("ข้อจำกัด: นี่เป็นผลค้นจาก 2 แหล่ง ไม่ใช่ข้อสรุปว่าข้อมูลทุกแหล่งเห็นตรงกัน")
        }
        logger.record("WEB_RESEARCH", "query=$query sources=${sections.size}")
        permissions.sanitizeExternalData(result)
    }

    private fun searchDuckDuckGo(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val root = getJson("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
        val lines = mutableListOf<String>()
        root.optString("AbstractText").takeIf { it.isNotBlank() }?.let { abstract ->
            lines.add(abstract)
            root.optString("AbstractSource").takeIf { it.isNotBlank() }?.let { lines.add("แหล่งข้อมูล: $it") }
            root.optString("AbstractURL").takeIf { it.startsWith("https://") }?.let { lines.add("URL: $it") }
        }
        root.optJSONArray("RelatedTopics")?.let { topics ->
            for (i in 0 until minOf(topics.length(), 8)) {
                val item = topics.optJSONObject(i) ?: continue
                item.optString("Text").takeIf { it.isNotBlank() }?.let { topic ->
                    lines.add("• $topic")
                    item.optString("FirstURL").takeIf { it.startsWith("https://") }?.let { lines.add("  URL: $it") }
                }
            }
        }
        return if (lines.isEmpty()) {
            "แหล่งที่ 1 — DuckDuckGo Instant Answer\nไม่พบคำตอบแบบ Instant Answer สำหรับคำค้นนี้"
        } else {
            "แหล่งที่ 1 — DuckDuckGo Instant Answer (ไม่ระบุวันที่เผยแพร่)\n" + lines.joinToString("\n")
        }
    }

    private fun searchWikipedia(query: String): String {
        val language = if (query.any { it in '\u0E00'..'\u0E7F' }) "th" else "en"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val root = getJson(
            "https://$language.wikipedia.org/w/api.php?action=query&list=search" +
                "&srsearch=$encoded&format=json&utf8=1&srlimit=5&srprop=timestamp"
        )
        val results = root.optJSONObject("query")?.optJSONArray("search")
            ?: return "แหล่งที่ 2 — Wikipedia ($language)\nไม่พบรายการค้นหา"
        val lines = mutableListOf<String>()
        for (i in 0 until minOf(results.length(), 5)) {
            val item = results.optJSONObject(i) ?: continue
            val title = item.optString("title").takeIf { it.isNotBlank() } ?: continue
            val snippet = item.optString("snippet")
                .replace(Regex("<[^>]*>"), "")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
            val timestamp = item.optString("timestamp").takeIf { it.length >= 10 }
                ?.take(10) ?: "ไม่ระบุ"
            val page = "https://$language.wikipedia.org/wiki/" +
                URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
            lines.add("${i + 1}. $title\n$snippet\nแก้ไขล่าสุด: $timestamp\nURL: $page")
        }
        return if (lines.isEmpty()) {
            "แหล่งที่ 2 — Wikipedia ($language)\nไม่พบรายการค้นหา"
        } else {
            "แหล่งที่ 2 — Wikipedia ($language)\n" + lines.joinToString("\n\n")
        }
    }

    private fun getJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "SOMCHAI-AI/1.1 (Android research client)")
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            check(code in 200..299) { "Research source returned HTTP $code" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            check(body.length <= 1_000_000) { "Research response exceeds size limit" }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
