package com.somchai.ai.core

import android.content.Context
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.llm.LlmConnector
import com.somchai.ai.core.plugins.PluginManager
import com.somchai.ai.core.resources.ResourceLimiter
import com.somchai.ai.core.security.PermissionManager
import com.somchai.ai.core.security.SecretStore
import com.somchai.ai.extensions.BuiltinToolExecutor

class CoreRuntime(context: Context) {
    val paths = CorePaths(context.applicationContext)
    val logger = ActivityLogger(paths)
    val permissions = PermissionManager()
    val secrets = SecretStore(context.applicationContext)
    val resources = ResourceLimiter(context.applicationContext, paths)
    val plugins = PluginManager(paths, logger, permissions)
    val tools = BuiltinToolExecutor(permissions, logger, resources)
    val llm = LlmConnector(secrets, resources, logger)

    init {
        paths.ensureDirectories()
        plugins.ensureStarterCandidate()
        logger.record("CORE_START", "Protected core initialized")
    }

    fun saveProviderSettings(baseUrl: String, model: String, apiKey: String) {
        secrets.saveProvider(baseUrl, model, apiKey)
        logger.record("LLM_SETTINGS_UPDATED", "Provider settings saved")
    }
}
