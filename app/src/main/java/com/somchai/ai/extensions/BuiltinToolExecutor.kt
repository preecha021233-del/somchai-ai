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
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "SOMCHAI-AI/1.0 Android")
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val lines = mutableListOf<String>()
            root.optString("AbstractText").takeIf { it.isNotBlank() }?.let { lines.add(it) }
            root.optJSONArray("RelatedTopics")?.let { topics ->
                for (i in 0 until minOf(topics.length(), 8)) {
                    val item = topics.optJSONObject(i) ?: continue
                    item.optString("Text").takeIf { it.isNotBlank() }?.let { lines.add(it) }
                }
            }
            val result = lines.joinToString("\n").ifBlank {
                "ไม่พบคำตอบแบบ Instant Answer สำหรับคำค้นนี้ ลองระบุคำค้นให้เฉพาะเจาะจงขึ้น"
            }
            logger.record("WEB_SEARCH", "query=$query")
            permissions.sanitizeExternalData(result)
        } finally { connection.disconnect() }
    }
}
