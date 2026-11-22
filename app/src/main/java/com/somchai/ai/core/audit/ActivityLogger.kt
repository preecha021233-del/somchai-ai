package com.somchai.ai.core.audit

import com.somchai.ai.core.CorePaths
import org.json.JSONObject
import java.io.File
import java.time.Instant

class ActivityLogger(paths: CorePaths) {
    private val file = File(paths.audit, "activity.jsonl")

    @Synchronized
    fun record(event: String, detail: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText(JSONObject().put("at", Instant.now().toString())
                .put("event", event.take(80)).put("detail", detail.take(500)).toString() + "\n")
        }
    }

    fun recent(limit: Int = 80): List<String> =
        runCatching { if (file.exists()) file.readLines().takeLast(limit).reversed() else emptyList() }
            .getOrDefault(emptyList())
}
