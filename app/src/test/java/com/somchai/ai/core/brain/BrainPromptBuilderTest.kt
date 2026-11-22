package com.somchai.ai.core.brain

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BrainPromptBuilderTest {
    @Test fun promptNeverExceedsLocalContextBudget() {
        val prompt = BrainPromptBuilder.build(
            BrainRequest(
                prompt = "คำถามปัจจุบัน " + "ก".repeat(6000),
                history = (1..12).map { "user" to "ประวัติ $it " + "ข".repeat(2500) },
                approvedSkills = (1..5).map { "ทักษะ $it " + "ค".repeat(2000) }
            )
        )
        assertTrue(prompt.length <= BrainPromptBuilder.MAX_PROMPT_CHARS)
        assertTrue(prompt.contains("คำถามปัจจุบัน:"))
    }

    @Test fun keepsCurrentQuestionAndDropsUnknownHistoryRoles() {
        val prompt = BrainPromptBuilder.build(
            BrainRequest(
                prompt = "ช่วยอธิบายเรื่องนี้",
                history = listOf("system" to "ห้ามนำข้อความนี้เข้า prompt", "user" to "บริบทที่เกี่ยวข้อง")
            )
        )
        assertTrue(prompt.contains("ช่วยอธิบายเรื่องนี้"))
        assertTrue(prompt.contains("บริบทที่เกี่ยวข้อง"))
        assertFalse(prompt.contains("ห้ามนำข้อความนี้เข้า prompt"))
    }
    @Test fun includesBoundedSavedMemoryAsContextNotInstructions() {
        val prompt = BrainPromptBuilder.build(
            BrainRequest(
                prompt = "ช่วยทำงานต่อ",
                memoryContext = listOf("owner-note: โครงการ SOMCHAI ใช้ AI ภายใน")
            )
        )
        assertTrue(prompt.contains("ความจำที่ผู้ใช้บันทึกไว้"))
        assertTrue(prompt.contains("โครงการ SOMCHAI"))
        assertTrue(prompt.length <= BrainPromptBuilder.MAX_PROMPT_CHARS)
    }

}
