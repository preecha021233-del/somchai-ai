package com.somchai.ai.core.brain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real on-device GGUF inference through llama.cpp. No network or remote provider is used.
 * The model must be explicitly downloaded/installed before this engine reports ready.
 */
class NativeLlamaInferenceEngine : OnDeviceInferenceEngine, AutoCloseable {
    @Volatile private var handle: Long = 0L
    @Volatile private var contextCapacity: Int = DEFAULT_CONTEXT_SIZE
    private val lock = Any()

    override val engineId: String = "llama.cpp-android"

    fun loadModel(file: File, contextSize: Int = DEFAULT_CONTEXT_SIZE, threads: Int = 4) {
        require(GgufFileValidator.isVerifiedDefaultModel(file)) {
            "ไฟล์โมเดลไม่ผ่านการตรวจสอบ GGUF หรือ SHA-256 ของโมเดลที่อนุมัติ"
        }
        synchronized(lock) {
            unloadLocked()
            val capacity = contextSize.coerceIn(512, 4096)
            val loaded = NativeLlamaBindings.nativeLoadModel(file.absolutePath, capacity, threads)
            check(loaded != 0L) { "โหลดโมเดลภายในไม่สำเร็จ" }
            contextCapacity = capacity
            handle = loaded
        }
    }

    override fun isModelLoaded(): Boolean = handle != 0L

    override suspend fun generate(request: BrainRequest): String = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val current = handle
            check(current != 0L) { "ยังไม่ได้ติดตั้งโมเดล AI ภายใน" }

            var bounded = request.copy(prompt = request.prompt.trim().take(BrainPromptBuilder.MAX_CURRENT_CHARS))
            var prompt = BrainPromptBuilder.build(bounded)
            val maxInputTokens = (contextCapacity - MAX_GENERATION_TOKENS - CONTEXT_SAFETY_TOKENS)
                .coerceAtLeast(1)
            var tokenCount = NativeLlamaBindings.nativeCountTokens(current, prompt)
            var attempts = 0

            // Character counts do not predict Thai/BPE token counts. Ask the actual
            // model tokenizer, then remove optional context before shortening the question.
            while (tokenCount > maxInputTokens && attempts < MAX_BUDGET_RETRIES) {
                bounded = when {
                    bounded.history.size > 2 ->
                        bounded.copy(history = bounded.history.takeLast((bounded.history.size / 2).coerceAtLeast(1)))
                    bounded.history.isNotEmpty() -> bounded.copy(history = emptyList())
                    bounded.memoryContext.size > 1 -> bounded.copy(memoryContext = bounded.memoryContext.take(1))
                    bounded.memoryContext.isNotEmpty() -> bounded.copy(memoryContext = emptyList())
                    bounded.approvedSkills.size > 1 -> bounded.copy(approvedSkills = bounded.approvedSkills.take(1))
                    bounded.approvedSkills.isNotEmpty() -> bounded.copy(approvedSkills = emptyList())
                    bounded.prompt.length > MIN_CURRENT_CHARS ->
                        bounded.copy(prompt = bounded.prompt.takeLast((bounded.prompt.length * 3 / 4).coerceAtLeast(MIN_CURRENT_CHARS)))
                    else -> error("คำถามยาวเกินขีดจำกัดของโมเดล แม้ตัดบริบทเสริมแล้ว")
                }
                prompt = BrainPromptBuilder.build(bounded)
                tokenCount = NativeLlamaBindings.nativeCountTokens(current, prompt)
                attempts++
            }

            check(tokenCount <= maxInputTokens) {
                "คำถามยาวเกินขีดจำกัดโทเค็นของโมเดล กรุณาย่อคำถาม"
            }
            String(NativeLlamaBindings.nativeGenerate(current, prompt, MAX_GENERATION_TOKENS), Charsets.UTF_8).trim()
        }
    }

    override fun close() {
        synchronized(lock) { unloadLocked() }
    }

    private fun unloadLocked() {
        val previous = handle
        handle = 0L
        if (previous != 0L) NativeLlamaBindings.nativeUnloadModel(previous)
    }

    companion object {
        private const val DEFAULT_CONTEXT_SIZE = 2048
        private const val MAX_GENERATION_TOKENS = 384
        private const val CONTEXT_SAFETY_TOKENS = 16
        private const val MIN_CURRENT_CHARS = 200
        private const val MAX_BUDGET_RETRIES = 12
    }
}
