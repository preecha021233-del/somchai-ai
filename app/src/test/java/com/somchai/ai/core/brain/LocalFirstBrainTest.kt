package com.somchai.ai.core.brain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFirstBrainTest {
    @Test fun usesLoadedLocalModelWithoutRemoteProvider(): Unit = runBlocking {
        val engine = object : OnDeviceInferenceEngine {
            override val engineId = "test-local-model"
            override fun isModelLoaded() = true
            override suspend fun generate(request: BrainRequest) = "คำตอบจากโมเดลในเครื่อง"
        }
        val result = LocalFirstBrain(engine) { "fallback" }.respond(BrainRequest("คำถามภาษาไทย"))
        assertEquals("คำตอบจากโมเดลในเครื่อง", result.text)
        assertEquals("test-local-model", result.engineId)
        assertFalse(result.usedFallback)
    }

    @Test fun fallsBackWhenLocalModelIsUnavailable(): Unit = runBlocking {
        val result = LocalFirstBrain(UnavailableOnDeviceInferenceEngine) { "คำตอบสำรอง" }
            .respond(BrainRequest("คำถาม"))
        assertEquals("คำตอบสำรอง", result.text)
        assertEquals("offline-fallback", result.engineId)
        assertTrue(result.usedFallback)
    }

    @Test fun fallsBackWhenLocalModelThrows(): Unit = runBlocking {
        val engine = object : OnDeviceInferenceEngine {
            override val engineId = "broken-local-model"
            override fun isModelLoaded() = true
            override suspend fun generate(request: BrainRequest): String = error("native runtime error")
        }
        val result = LocalFirstBrain(engine) { "fallback after runtime error" }
            .respond(BrainRequest("คำถาม"))
        assertEquals("fallback after runtime error", result.text)
        assertTrue(result.usedFallback)
    }

    @Test fun returnsSafeMessageWhenBothModelAndFallbackFail(): Unit = runBlocking {
        val result = LocalFirstBrain(UnavailableOnDeviceInferenceEngine) {
            error("fallback processor failed")
        }.respond(BrainRequest("คำถาม"))
        assertTrue(result.usedFallback)
        assertTrue(result.text.contains("ระบบสำรองยังไม่มีคำตอบ"))
    }

    @Test fun rejectsBlankPrompt(): Unit = runBlocking {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking { LocalFirstBrain(UnavailableOnDeviceInferenceEngine) { "fallback" }.respond(BrainRequest(" ")) }
        }
    }
}
