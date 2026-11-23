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
            val line = JSONObject().put("at", Instant.now().toString())
                .put("event", event.take(80)).put("detail", detail.take(500)).toString() + "\n"
            if (file.isFile && file.length() + line.toByteArray(Charsets.UTF_8).size > MAX_FILE_BYTES) {
                val retained = file.readLines().takeLast(MAX_RETAINED_LINES)
                val temporary = File(file.parentFile, "activity.jsonl.tmp")
                temporary.writeText(retained.joinToString("\n", postfix = if (retained.isEmpty()) "" else "\n"))
                if (!temporary.renameTo(file)) {
                    temporary.copyTo(file, overwrite = true)
                    temporary.delete()
                }
            }
            file.appendText(line)
        }
    }

    @Synchronized
    fun recent(limit: Int = 80): List<String> =
        runCatching {
            if (file.exists()) file.readLines()
                .takeLast(limit.coerceIn(1, MAX_RETAINED_LINES)).reversed()
            else emptyList()
        }.getOrDefault(emptyList())

    companion object {
        private const val MAX_FILE_BYTES = 1_048_576L
        private const val MAX_RETAINED_LINES = 1_500
    }
}
