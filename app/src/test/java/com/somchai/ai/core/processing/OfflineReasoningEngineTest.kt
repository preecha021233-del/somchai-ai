package com.somchai.ai.core.processing

import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineReasoningEngineTest {
    private val engine = OfflineReasoningEngine()

    @Test fun calculatesChangeFromQuantityAndUnitPriceInThai() {
        val answer = engine.solveArithmetic("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท จ่าย 100 บาท ได้ทอนเท่าไร")
            ?: error("calculation was not recognized")
        assertTrue(answer.contains("3 × 25 = 75 บาท"))
        assertTrue(answer.contains("100 - 75 = 25 บาท"))
    }

    @Test fun reportsInsufficientPaymentInsteadOfNegativeChangeAsCashBack() {
        val answer = engine.solveArithmetic("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท จ่าย 50 บาท ได้ทอนเท่าไร")
            ?: error("calculation was not recognized")
        assertTrue(answer.contains("เงินไม่พอ"))
    }

    @Test fun rejectsIncompleteChangeQuestion() {
        assertNull(engine.solveArithmetic("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท ได้ทอนเท่าไร"))
    }
}
