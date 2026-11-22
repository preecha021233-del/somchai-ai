package com.somchai.ai.core.skills

import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.brain.OnDeviceInferenceEngine
import com.somchai.ai.core.memory.PlaintextMemoryCipher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SkillStoreTest {
    private class FakeEngine(private val answer: (String) -> String) : OnDeviceInferenceEngine {
        override val engineId = "test-engine"
        override fun isModelLoaded() = true
        override suspend fun generate(request: BrainRequest): String = answer(request.prompt)
    }

    private fun newStore(dir: File) = SkillStore(dir, PlaintextMemoryCipher)

    @Test fun proposedSkillIsNotActiveUntilTestedAndApproved() {
        val dir = createTempDir()
        try {
            val store = newStore(dir)
            val skill = store.propose(
                "สรุปรายงาน",
                listOf("สรุปรายงาน", "รายงาน"),
                "อ่านข้อมูล แยกหัวข้อ และสรุปประเด็น",
                listOf(
                    SkillTestCase("สรุปยอดขาย", "ยอดขาย"),
                    SkillTestCase("สรุปค่าใช้จ่าย", "ค่าใช้จ่าย")
                )
            )
            assertFalse(skill.approved)
            assertTrue(store.relevantFor("ช่วยสรุปรายงาน").isEmpty())
            runBlocking {
                store.evaluate(skill.id, FakeEngine { prompt ->
                    if (prompt.contains("สรุปยอดขาย")) "สรุปยอดขาย" else "สรุปค่าใช้จ่าย"
                })
            }
            val approved = store.approve(skill.id)
            assertTrue(approved.approved)
            assertEquals(1, store.relevantFor("ช่วยสรุปรายงาน").size)
        } finally { dir.deleteRecursively() }
    }

    @Test fun learnsDraftProcedureFromExperienceButDoesNotAutoApprove() {
        val dir = createTempDir()
        try {
            val store = newStore(dir)
            val draft = runBlocking {
                store.learnFromExperience(
                    "สรุปเอกสาร",
                    listOf("สรุปเอกสาร"),
                    "อ่านเอกสาร แยกหัวข้อหลัก ตรวจตัวเลข แล้วสรุปเป็นข้อ",
                    listOf(SkillTestCase("สรุปเอกสารรายงาน", "หัวข้อ"), SkillTestCase("สรุปเอกสารบัญชี", "ตัวเลข")),
                    FakeEngine { "1. อ่านเอกสาร 2. แยกหัวข้อ 3. ตรวจตัวเลข 4. สรุป" }
                )
            }
            assertFalse(draft.approved)
            assertTrue(draft.procedure.contains("แยกหัวข้อ"))
            assertTrue(store.relevantFor("ช่วยสรุปเอกสาร").isEmpty())
        } finally { dir.deleteRecursively() }
    }

    @Test fun approvalIsBlockedWhenAnyTestFails() {
        val dir = createTempDir()
        try {
            val store = newStore(dir)
            val skill = store.propose(
                "คำนวณ",
                listOf("คำนวณ"),
                "คำนวณตามข้อมูล",
                listOf(SkillTestCase("2+2", "4"), SkillTestCase("3+3", "6"))
            )
            runBlocking { store.evaluate(skill.id, FakeEngine { "ผลลัพธ์ 4" }) }
            assertFalse(store.all().single().approved)
            try {
                store.approve(skill.id)
                throw AssertionError("Expected approval rejection")
            } catch (_: IllegalArgumentException) { }
        } finally { dir.deleteRecursively() }
    }

    @Test fun approvedSkillPersistsAcrossStoreInstances() {
        val dir = createTempDir()
        try {
            val first = newStore(dir)
            val skill = first.propose(
                "แปลภาษา",
                listOf("แปล"),
                "รักษาความหมายและแปลตามภาษาเป้าหมาย",
                listOf(SkillTestCase("hello", "สวัสดี"), SkillTestCase("thanks", "ขอบคุณ"))
            )
            runBlocking {
                first.evaluate(skill.id, FakeEngine { prompt ->
                    if (prompt.contains("hello")) "สวัสดี" else "ขอบคุณ"
                })
            }
            first.approve(skill.id)
            val second = newStore(dir)
            assertTrue(second.all().single().approved)
        } finally { dir.deleteRecursively() }
    }
}
