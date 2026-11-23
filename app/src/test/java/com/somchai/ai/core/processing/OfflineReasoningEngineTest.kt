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


    @Test fun calculatesChangeWhenPaymentUsesThaiWordChำระ() {
        val answer = engine.solveArithmetic("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท ชำระ 100 บาท ได้ทอนเท่าไร")
            ?: error("calculation was not recognized")
        assertTrue(answer.contains("3 × 25 = 75 บาท"))
        assertTrue(answer.contains("100 - 75 = 25 บาท"))
    }

    @Test fun rejectsIncompleteChangeQuestion() {
        assertNull(engine.solveArithmetic("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท ได้ทอนเท่าไร"))
    }
    @Test fun questionAndNegatedStatementAreNotMisclassifiedAsCommands() {
        val question = engine.classifyExamples(
            "จำแนกข้อความต่อไปนี้:\n1. การบันทึกข้อมูลมีข้อดีอย่างไร"
        ) ?: error("question intent was not recognized")
        assertTrue(question.contains("ประเภท: คำถาม"))

        assertNull(
            engine.classifyExamples(
                "จำแนกข้อความต่อไปนี้:\n1. ฉันไม่ต้องการลบข้อมูล"
            )
        )
    }

    @Test fun recognizesAdditionalThaiQuestionFormsBeforeActionKeywords() {
        val examples = engine.classifyExamples(
            "จำแนกข้อความต่อไปนี้:\n1. วิธีคำนวณภาษีทำอย่างไร\n2. การสร้างบัญชีต้องใช้อะไร"
        ) ?: error("Thai question forms were not recognized")
        assertTrue(examples.contains("ประเภท: คำถาม"))
        assertTrue(examples.contains("วิธีคำนวณภาษีทำอย่างไร"))
        assertTrue(examples.contains("การสร้างบัญชีต้องใช้อะไร"))
    }

    @Test fun identifiesExactlyWhichInputsAreMissingForSpeedCalculation() {
        val missingBoth = engine.missingInformation("รถคันนี้มีความเร็วเท่าไร")
            ?: error("missing speed inputs were not detected")
        assertTrue(missingBoth.contains("ระยะทาง"))
        assertTrue(missingBoth.contains("เวลาที่ใช้"))

        val missingTime = engine.missingInformation("รถวิ่งระยะทาง 120 กิโลเมตร มีความเร็วเท่าไร")
            ?: error("missing time was not detected")
        assertTrue(missingTime.contains("เวลาที่ใช้"))
        assertTrue(!missingTime.contains("ระยะทาง และ"))
    }

    @Test fun identifiesMissingCashChangeInputsIndividually() {
        val missingBoth = engine.missingInformation("เงินทอนเท่าไร")
            ?: error("missing cash-change inputs were not detected")
        assertTrue(missingBoth.contains("ราคารวมของสินค้า"))
        assertTrue(missingBoth.contains("จำนวนเงินที่จ่าย"))

        val missingPayment = engine.missingInformation("สินค้าราคา 80 บาท เงินทอนเท่าไร")
            ?: error("missing payment amount was not detected")
        assertTrue(missingPayment.contains("จำนวนเงินที่จ่าย"))
        val missingFields = missingPayment.substringAfter("ตอนนี้ยังขาด:").substringBefore("กรุณาระบุ")
        assertTrue(!missingFields.contains("ราคารวมของสินค้า"))
    }

    @Test fun rejectsOversizedNumericLiteralsAndExcessiveNumericTokens() {
        assertNull(engine.solveArithmetic("9".repeat(65)))
        assertNull(engine.solveArithmetic("1+".repeat(128) + "1"))
    }
}