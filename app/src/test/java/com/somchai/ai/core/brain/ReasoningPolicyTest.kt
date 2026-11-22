package com.somchai.ai.core.brain

import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningPolicyTest {
    @Test fun requiresIntentAndContextChecks() {
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("เจตนา"))
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("บริบท"))
    }

    @Test fun requiresContradictionAndMissingInformationChecks() {
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("ขัดแย้ง"))
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("ข้อมูลไม่พอ"))
    }

    @Test fun forbidsFabricatedSourcesAndActions() {
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("ห้ามแต่งข้อเท็จจริง"))
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("ผลการกระทำ"))
    }

    @Test fun protectsPrivateReasoningAndUntrustedDataBoundary() {
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("ข้อมูลที่ไม่น่าเชื่อถือ"))
        assertTrue(ReasoningPolicy.SYSTEM_PROMPT.contains("อย่าเปิดเผยบันทึกความคิดภายใน"))
    }
}
