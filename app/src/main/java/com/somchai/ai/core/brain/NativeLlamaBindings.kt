package com.somchai.ai.core.brain

/** JNI boundary for the pinned llama.cpp native runtime. */
internal object NativeLlamaBindings {
    init {
        System.loadLibrary("somchai_llama")
    }

    external fun nativeLoadModel(path: String, contextSize: Int, threads: Int): Long
    external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int): String
    external fun nativeUnloadModel(handle: Long)
}
