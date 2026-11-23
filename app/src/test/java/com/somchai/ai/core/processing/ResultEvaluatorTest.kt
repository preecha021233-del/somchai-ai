package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultEvaluatorTest {
    private val evaluator = ResultEvaluator()

    @Test fun acceptsCorrectMultiStepArithmeticWithIntermediateCalculations() {
        val answer = """
            วิธีคำนวณ:
            เงินเริ่มต้น = 500 บาท
            รายการที่ 1: 2 × 30 = 60 บาท
            รายการที่ 2: 3 × 10 = 30 บาท
            ยอดใช้รวม = 60 + 30 = 90 บาท
            เงินคงเหลือ = 500 - 90 = 410 บาท
        """.trimIndent()
        val checked = evaluator.evaluate(ProcessingResult(answer, "reasoning.arithmetic"))
        assertTrue(checked.safe)
        assertEquals(answer, checked.text)
    }

    @Test fun rejectsArithmeticAnswerWhenDisplayedResultIsWrong() {
        val checked = evaluator.evaluate(
            ProcessingResult("วิธีคำนวณ:\\n2 + 3 * 4 = 15", "reasoning.arithmetic")
        )
        assertFalse(checked.safe)
        assertTrue(checked.notes.contains("arithmetic-verification-failed"))
        assertTrue(checked.text.contains("ยังยืนยันไม่ได้"))
    }

    @Test fun rejectsArithmeticIntentWithoutAVerifiableEquation() {
        val checked = evaluator.evaluate(
            ProcessingResult("คำตอบคือ 14", "reasoning.arithmetic")
        )
        assertFalse(checked.safe)
    }

    @Test fun keepsCorrectCashChangeCalculationSafe() {
        val checked = evaluator.evaluate(
            ProcessingResult(
                "วิธีคำนวณ:\\nราคารวม = 3 × 25 = 75 บาท\\nเงินทอน = เงินที่จ่าย - ราคารวม\\nเงินทอน = 100 - 75 = 25 บาท",
                "reasoning.arithmetic"
            )
        )
        assertTrue(checked.safe)
    }
}