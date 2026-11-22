package com.somchai.ai.core.brain

data class BrainRequest(
    val prompt: String,
    val history: List<Pair<String, String>> = emptyList(),
    val approvedSkills: List<String> = emptyList(),
    val memoryContext: List<String> = emptyList()
)

data class BrainReply(
    val text: String,
    val engineId: String,
    val usedFallback: Boolean
)

/** All language generation used by the app must pass through this local-first boundary. */
interface Brain {
    suspend fun respond(request: BrainRequest): BrainReply
}

/** Adapter contract implemented by the selected on-device inference runtime. */
interface OnDeviceInferenceEngine {
    val engineId: String
    fun isModelLoaded(): Boolean
    suspend fun generate(request: BrainRequest): String
}

/**
 * Local-first router. It never calls a remote provider. If the local model is unavailable
 * or fails, it returns the caller-supplied deterministic offline result.
 */
class LocalFirstBrain(
    private val engine: OnDeviceInferenceEngine,
    private val fallback: (BrainRequest) -> String
) : Brain {
    override suspend fun respond(request: BrainRequest): BrainReply {
        require(request.prompt.isNotBlank()) { "Prompt cannot be blank" }
        require(request.prompt.length <= 6000) { "Prompt exceeds the local context input limit" }
        if (engine.isModelLoaded()) {
            val generated = runCatching { engine.generate(request).trim() }.getOrNull()
            if (!generated.isNullOrBlank()) {
                return BrainReply(generated, engine.engineId, usedFallback = false)
            }
        }
        val safeFallback = fallback(request).ifBlank {
            "โมเดลภายในยังไม่พร้อมใช้งาน และระบบสำรองยังไม่มีคำตอบสำหรับคำขอนี้"
        }
        return BrainReply(safeFallback, "offline-fallback", usedFallback = true)
    }
}

/** Explicit unavailable adapter; it prevents accidental claims that a model is installed. */
object UnavailableOnDeviceInferenceEngine : OnDeviceInferenceEngine {
    override val engineId: String = "llama.cpp-not-installed"
    override fun isModelLoaded(): Boolean = false
    override suspend fun generate(request: BrainRequest): String =
        error("ยังไม่ได้ติดตั้งโมเดลภาษาในเครื่อง")
}
