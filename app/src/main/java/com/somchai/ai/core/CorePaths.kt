package com.somchai.ai.core

import android.content.Context
import java.io.File

class CorePaths(context: Context) {
    private val root = File(context.filesDir, "somchai")
    val core = File(root, "core")
    val staging = File(root, "plugins/staging")
    val active = File(root, "plugins/active")
    val backups = File(root, "plugins/backups")
    val pluginData = File(root, "plugin-data")
    val memory = File(root, "memory")
    val audit = File(root, "audit")
    val models = File(root, "models")

    fun ensureDirectories() {
        listOf(root, core, staging, active, backups, pluginData, memory, audit, models)
            .forEach { check(it.exists() || it.mkdirs()) { "Cannot create app storage directory" } }
    }

    fun contained(parent: File, child: File): Boolean =
        child.canonicalFile.toPath().startsWith(parent.canonicalFile.toPath())
}
