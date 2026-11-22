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
    private val lock = Any()

    override val engineId: String = "llama.cpp-android"

    fun loadModel(file: File, contextSize: Int = 2048, threads: Int = 4) {
        require(file.isFile && file.length() > 10_000_000L) { "ไม่พบไฟล์โมเดล GGUF ที่สมบูรณ์" }
        synchronized(lock) {
            unloadLocked()
            val loaded = NativeLlamaBindings.nativeLoadModel(file.absolutePath, contextSize, threads)
            check(loaded != 0L) { "โหลดโมเดลภายในไม่สำเร็จ" }
            handle = loaded
        }
    }

    override fun isModelLoaded(): Boolean = handle != 0L

    override suspend fun generate(request: BrainRequest): String = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val current = handle
            check(current != 0L) { "ยังไม่ได้ติดตั้งโมเดล AI ภายใน" }
            val prompt = buildString {
                append(ReasoningPolicy.SYSTEM_PROMPT).append('\n')
                request.history.takeLast(8).forEach { (role, content) ->
                    if (role == "user" || role == "assistant") {
                        append(if (role == "user") "ผู้ใช้: " else "SOMCHAI: ")
                        append(content.take(2500)).append('\n')
                    }
                }
                append("คำถามปัจจุบัน: ").append(request.prompt)
                append("\nตอบเป็นภาษาไทยให้ตรงคำถาม")
            }
            NativeLlamaBindings.nativeGenerate(current, prompt, 384).trim()
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
}
