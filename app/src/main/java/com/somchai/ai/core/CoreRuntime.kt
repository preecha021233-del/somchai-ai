package com.somchai.ai.core

import android.content.Context
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.brain.LocalFirstBrain
import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.brain.LocalModelStore
import com.somchai.ai.core.brain.NativeLlamaInferenceEngine
import com.somchai.ai.core.llm.LlmConnector
import com.somchai.ai.core.memory.AndroidMemoryCipher
import com.somchai.ai.core.memory.LocalMemoryStore
import com.somchai.ai.core.plugins.PluginManager
import com.somchai.ai.core.processing.InternalProcessor
import com.somchai.ai.core.processing.ResultEvaluator
import com.somchai.ai.core.orchestrator.ChatOrchestrator
import com.somchai.ai.core.processing.TaskPlanner
import com.somchai.ai.core.resources.ResourceLimiter
import com.somchai.ai.core.security.PermissionManager
import com.somchai.ai.core.security.SecretStore
import com.somchai.ai.core.skills.SkillStore
import com.somchai.ai.extensions.BuiltinToolExecutor
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocalModelStatus { NOT_INSTALLED, DOWNLOADING, LOADING, READY, FAILED }

class CoreRuntime(context: Context) {
    val paths = CorePaths(context.applicationContext)
    val logger = ActivityLogger(paths)
    val permissions = PermissionManager()
    val secrets = SecretStore(context.applicationContext)
    val resources = ResourceLimiter(context.applicationContext, paths)
    val memory = LocalMemoryStore(paths.memory, AndroidMemoryCipher()) { event, detail -> logger.record(event, detail) }
    val skills = SkillStore(paths.skills, AndroidMemoryCipher()) { event, detail -> logger.record(event, detail) }
    val planner = TaskPlanner()
    val tasks = com.somchai.ai.core.processing.TaskExecutionStore(paths.core) { event, detail ->
        logger.record(event, detail)
    }
    val processor = InternalProcessor(memory, planner, tasks = tasks)
    val modelStore = LocalModelStore(paths.models)
    val localInference = NativeLlamaInferenceEngine()
    private val mutableLocalModelStatus = MutableStateFlow(
        if (modelStore.modelFile().isFile) LocalModelStatus.LOADING else LocalModelStatus.NOT_INSTALLED
    )
    val localModelStatus: StateFlow<LocalModelStatus> = mutableLocalModelStatus.asStateFlow()
    // Ordinary chat uses the on-device llama.cpp engine when a verified GGUF model is installed.
    // Otherwise it reports the deterministic internal fallback; it never silently calls a remote provider.
    val brain = LocalFirstBrain(localInference) { request -> processor.process(request.prompt).answer }
    val plugins = PluginManager(paths, logger, permissions)
    val tools = BuiltinToolExecutor(
        permissions, logger, resources::checkStorageBudget,
        isWebSearchApproved = {
            plugins.activePlugins().any { it.id == "web-search" && it.operation == "web.search" }
        },
        summarizeLocally = { prompt ->
            if (localInference.isModelLoaded()) localInference.generate(BrainRequest(prompt)) else ""
        }
    )
    val llm = LlmConnector(secrets, resources, logger)
    val orchestrator = ChatOrchestrator(
        brain = brain,
        processor = processor,
        searchApproved = { plugins.activePlugins().any { it.id == "web-search" && it.operation == "web.search" } },
        search = { query -> tools.search(query) },
        remoteConfigured = { secrets.hasKey() },
        remoteAsk = { history, prompt -> llm.ask(history, prompt) },
        tasks = tasks,
        planner = planner,
        audit = { event, detail -> logger.record(event, detail) },
        evaluator = ResultEvaluator(),
        retrieveMemory = { query ->
            memory.relevantFor(query, 3).map { "[${it.id}] ${it.content}" }
        }
    )

    private val modelLoader = Executors.newSingleThreadExecutor { task -> Thread(task, "somchai-model-loader").apply { isDaemon = true } }

    init {
        paths.ensureDirectories()
        plugins.ensureStarterCandidate()
        val installedModel = modelStore.modelFile()
        if (installedModel.isFile) {
            // Never load a 491 MB model on Application.onCreate's main thread.
            modelLoader.execute {
                if (!modelStore.isVerifiedModel(installedModel)) {
                    mutableLocalModelStatus.value = LocalModelStatus.FAILED
                    logger.record("LOCAL_MODEL_REJECTED", "Installed model failed SHA-256/GGUF validation")
                } else {
                    runCatching { localInference.loadModel(installedModel) }
                        .onSuccess {
                            mutableLocalModelStatus.value = LocalModelStatus.READY
                            logger.record("LOCAL_MODEL_READY", installedModel.name)
                        }
                        .onFailure {
                            mutableLocalModelStatus.value = LocalModelStatus.FAILED
                            logger.record("LOCAL_MODEL_LOAD_FAILED", it.message ?: "unknown")
                        }
                }
            }
        }
        logger.record("CORE_START", "Protected core initialized; local model=" + localInference.isModelLoaded())
    }

    suspend fun downloadAndLoadLocalModel(onProgress: (Int) -> Unit = {}): File = withContext(Dispatchers.IO) {
        mutableLocalModelStatus.value = LocalModelStatus.DOWNLOADING
        try {
            val file = modelStore.downloadDefaultModel(onProgress)
            mutableLocalModelStatus.value = LocalModelStatus.LOADING
            localInference.loadModel(file)
            mutableLocalModelStatus.value = LocalModelStatus.READY
            logger.record("LOCAL_MODEL_READY", "engine=llama.cpp file=" + file.name + " bytes=" + file.length())
            file
        } catch (failure: Exception) {
            mutableLocalModelStatus.value = LocalModelStatus.FAILED
            logger.record("LOCAL_MODEL_SETUP_FAILED", failure.message ?: "unknown")
            throw failure
        }
    }

    suspend fun loadInstalledLocalModel(): File = withContext(Dispatchers.IO) {
        mutableLocalModelStatus.value = LocalModelStatus.LOADING
        try {
            val file = modelStore.modelFile()
            require(modelStore.isVerifiedModel(file)) { "ไฟล์โมเดลไม่ผ่านการตรวจสอบ SHA-256/GGUF กรุณาดาวน์โหลดใหม่" }
            if (!localInference.isModelLoaded()) localInference.loadModel(file)
            mutableLocalModelStatus.value = LocalModelStatus.READY
            logger.record("LOCAL_MODEL_READY", "engine=llama.cpp file=" + file.name + " bytes=" + file.length())
            file
        } catch (failure: Exception) {
            mutableLocalModelStatus.value = LocalModelStatus.FAILED
            logger.record("LOCAL_MODEL_LOAD_FAILED", failure.message ?: "unknown")
            throw failure
        }
    }

    fun saveProviderSettings(baseUrl: String, model: String, apiKey: String) {
        secrets.saveProvider(baseUrl, model, apiKey)
        logger.record("LLM_SETTINGS_UPDATED", "Optional provider settings saved")
    }
}
