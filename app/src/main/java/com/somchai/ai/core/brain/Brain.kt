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
    val usedFallback: Boolean,
    val failureReason: String? = null
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
        var failureReason: String? = null
        val loaded = try {
            engine.isModelLoaded()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            failureReason = "${e.javaClass.simpleName}: ${e.message.orEmpty()}".take(240)
            false
        }
        if (loaded) {
            try {
                val generated = engine.generate(request).trim()
                if (generated.isNotBlank()) return BrainReply(generated, engine.engineId, usedFallback = false)
                failureReason = "โมเดลส่งคำตอบว่างเปล่า"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                failureReason = "${e.javaClass.simpleName}: ${e.message.orEmpty()}".take(240)
            }
        } else if (failureReason == null) {
            failureReason = "ยังไม่ได้โหลดโมเดลภายใน"
        }
        val safeFallback = try {
            fallback(request).trim()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            failureReason = (failureReason.orEmpty() + "; fallback " + e.javaClass.simpleName + ": " + e.message.orEmpty()).take(480)
            ""
        }.ifBlank {
            "โมเดลภายในยังไม่พร้อมใช้งาน และระบบสำรองยังไม่มีคำตอบสำหรับคำขอนี้"
        }
        return BrainReply(safeFallback, "offline-fallback", usedFallback = true, failureReason = failureReason)
    }
}

/** Explicit unavailable adapter; it prevents accidental claims that a model is installed. */
object UnavailableOnDeviceInferenceEngine : OnDeviceInferenceEngine {
    override val engineId: String = "llama.cpp-not-installed"
    override fun isModelLoaded(): Boolean = false
    override suspend fun generate(request: BrainRequest): String =
        error("ยังไม่ได้ติดตั้งโมเดลภาษาในเครื่อง")
}
