package com.somchai.ai.core.resources

import android.content.Context
import com.somchai.ai.core.CorePaths
import java.time.LocalDate

class ResourceLimiter(context: Context, paths: CorePaths) {
    private val prefs = context.getSharedPreferences("resource_budget", Context.MODE_PRIVATE)
    private val root = paths.active.parentFile?.parentFile

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

    fun checkStorageBudget() {
        val bytes = root?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        check(bytes < 50L * 1024L * 1024L) { "Somchai storage budget reached (50 MB)" }
    }
}
