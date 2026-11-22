package com.somchai.ai.core.resources

import android.content.Context
import com.somchai.ai.core.CorePaths
import java.time.LocalDate

class ResourceLimiter(context: Context, paths: CorePaths) {
    private val prefs = context.getSharedPreferences("resource_budget", Context.MODE_PRIVATE)
    private val root = paths.active.parentFile?.parentFile
    private val modelDirectory = paths.models

    @Synchronized
    fun consumeRequest() {
        val today = LocalDate.now().toString()
        if (prefs.getString("day", null) != today) {
            prefs.edit().putString("day", today).putInt("count", 0).apply()
        }
        val count = prefs.getInt("count", 0)
        check(count < 40) { "Daily API request limit reached (40)" }
        prefs.edit().putInt("count", count + 1).apply()
    }

    /**
     * The model weights are a separately managed asset (about 491 MB).
     * Exclude that directory from the 50 MB operational-data budget so
     * installing the local brain does not disable web tools or plugins.
     */
    fun checkStorageBudget() {
        val modelRoot = runCatching { modelDirectory.canonicalFile.toPath() }.getOrNull()
        val bytes = root?.walkTopDown()
            ?.filter { file ->
                file.isFile && (modelRoot == null ||
                    !runCatching { file.canonicalFile.toPath().startsWith(modelRoot) }.getOrDefault(true))
            }
            ?.sumOf { it.length() } ?: 0L
        check(bytes < MAX_OPERATIONAL_STORAGE_BYTES) {
            "Somchai operational storage budget reached (50 MB; model files are excluded)"
        }
    }

    companion object {
        private const val MAX_OPERATIONAL_STORAGE_BYTES = 50L * 1024L * 1024L
    }
}
