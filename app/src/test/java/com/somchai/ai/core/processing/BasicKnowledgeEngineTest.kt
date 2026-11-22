package com.somchai.ai.core.processing

import org.junit.Assert.assertTrue
import org.junit.Test

class BasicKnowledgeEngineTest {
    private val knowledge = BasicKnowledgeEngine()

    @Test fun explainsHowThunderIsProduced() {
        val answer = knowledge.answer("ทำไมฟ้าร้อง") ?: error("missing answer")
        assertTrue(answer.contains("อากาศ"))
        assertTrue(answer.contains("คลื่นกระแทก"))
    }

    @Test fun explainsBackupAdvantagesAndRisks() {
        val answer = knowledge.answer("อธิบายข้อดีข้อเสียของการสำรองข้อมูล") ?: error("missing answer")
        assertTrue(answer.contains("ข้อดี"))
        assertTrue(answer.contains("ข้อเสีย"))
        assertTrue(answer.contains("เข้ารหัส"))
    }
}
