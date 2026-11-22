package com.somchai.ai.core

import android.content.Context
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.brain.LocalFirstBrain
import com.somchai.ai.core.brain.LocalModelStore
import com.somchai.ai.core.brain.NativeLlamaInferenceEngine
import com.somchai.ai.core.llm.LlmConnector
import com.somchai.ai.core.memory.AndroidMemoryCipher
import com.somchai.ai.core.memory.LocalMemoryStore
import com.somchai.ai.core.plugins.PluginManager
import com.somchai.ai.core.processing.InternalProcessor
import com.somchai.ai.core.processing.TaskPlanner
import com.somchai.ai.core.resources.ResourceLimiter
import com.somchai.ai.core.security.PermissionManager
import com.somchai.ai.core.security.SecretStore
import com.somchai.ai.extensions.BuiltinToolExecutor
import java.io.File

class CoreRuntime(context: Context) {
    val paths = CorePaths(context.applicationContext)
    val logger = ActivityLogger(paths)
    val permissions = PermissionManager()
    val secrets = SecretStore(context.applicationContext)
    val resources = ResourceLimiter(context.applicationContext, paths)
    val memory = LocalMemoryStore(paths.memory, AndroidMemoryCipher()) { event, detail -> logger.record(event, detail) }
    val planner = TaskPlanner()
    val tasks = com.somchai.ai.core.processing.TaskExecutionStore(paths.core)
    val processor = InternalProcessor(memory, planner)
    val modelStore = LocalModelStore(paths.models)
    val localInference = NativeLlamaInferenceEngine()
    // Ordinary chat uses the on-device llama.cpp engine when a verified GGUF model is installed.
    // Otherwise it reports the deterministic internal fallback; it never silently calls a remote provider.
    val brain = LocalFirstBrain(localInference) { request -> processor.process(request.prompt).answer }
    val plugins = PluginManager(paths, logger, permissions)
    val tools = BuiltinToolExecutor(permissions, logger, resources)
    val llm = LlmConnector(secrets, resources, logger)

    init {
        paths.ensureDirectories()
        plugins.ensureStarterCandidate()
        val installedModel = modelStore.modelFile()
        if (installedModel.isFile) {
            runCatching { localInference.loadModel(installedModel) }
                .onSuccess { logger.record("LOCAL_MODEL_READY", installedModel.name) }
                .onFailure { logger.record("LOCAL_MODEL_LOAD_FAILED", it.message ?: "unknown") }
        }
        logger.record("CORE_START", "Protected core initialized; local model=" + localInference.isModelLoaded())
    }

    suspend fun downloadAndLoadLocalModel(onProgress: (Int) -> Unit = {}): File {
        val file = modelStore.downloadDefaultModel(onProgress)
        localInference.loadModel(file)
        logger.record("LOCAL_MODEL_READY", "engine=llama.cpp file=" + file.name + " bytes=" + file.length())
        return file
    }

    fun saveProviderSettings(baseUrl: String, model: String, apiKey: String) {
        secrets.saveProvider(baseUrl, model, apiKey)
        logger.record("LLM_SETTINGS_UPDATED", "Optional provider settings saved")
    }
}
