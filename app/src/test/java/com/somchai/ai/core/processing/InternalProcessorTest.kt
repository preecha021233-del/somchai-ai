package com.somchai.ai.core.processing

import com.somchai.ai.core.memory.LocalMemoryStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InternalProcessorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private fun processor(): InternalProcessor = InternalProcessor(LocalMemoryStore(temporaryFolder.newFolder("memory")))

    @Test fun answersGreetingWithoutExternalProvider() {
        val result = processor().process("สวัสดี SOMCHAI")
        assertTrue(result.intent == "greeting")
        assertTrue(result.answer.contains("ระบบประมวลผลภายใน"))
    }
    @Test fun explainsCurrentBoundedCapabilities() {
        val result = processor().process("คุณทำอะไรได้บ้าง")
        assertTrue(result.intent == "capabilities")
        assertTrue(result.answer.contains("ออฟไลน์"))
    }
    @Test fun answersSimpleThaiFactQuestionOffline() {
        val result = processor().process("แมวมีกี่ขา")
        assertTrue(result.intent == "knowledge.fact")
        assertTrue(result.answer.contains("4 ขา"))
    }
    @Test fun leavesUnknownQuestionsHonest() {
        val result = processor().process("ช่วยอธิบายทฤษฎีควอนตัม")
        assertTrue(result.intent == "unsupported")
        assertTrue(result.answer.contains("ยังไม่มีกฎภายใน"))
    }
    @Test fun createsTaskPlanWithoutExecutingIt() {
        val result = processor().process("/plan พัฒนาแอป")
        assertTrue(result.intent == "task.plan")
        assertTrue(result.plan.isNotEmpty())
        assertTrue(result.answer.contains("ยังไม่ได้ลงมือทำ"))
    }
    @Test fun storesAndRetrievesLocalMemory() {
        val processor = processor()
        assertTrue(processor.process("/remember ขั้นตอนสำรองข้อมูล").answer.contains("บันทึก"))
        val recalled = processor.process("/recall สำรองข้อมูล")
        assertTrue(recalled.intent == "memory.read")
        assertTrue(recalled.answer.contains("ขั้นตอนสำรองข้อมูล"))
    }
    @Test fun solvesThaiPurchaseChangeWordProblem() {
        val result = processor().process("มีเงิน 1,000 บาท ซื้อสินค้า 3 ชิ้น ราคา 120 บาท 250 บาท และ 180 บาท จ่ายเงินไป 1,000 บาท จะได้รับเงินทอนเท่าไร พร้อมแสดงวิธีคำนวณ")
        assertTrue(result.intent == "reasoning.arithmetic")
        assertTrue(result.answer.contains("450 บาท"))
    }
    @Test fun solvesParenthesizedArithmeticWithPrecedence() {
        val result = processor().process("คำนวณ 1,000 - (120 + 250 + 180)")
        assertTrue(result.intent == "reasoning.arithmetic")
        assertTrue(result.answer.contains("450"))
    }
    @Test fun classifiesQuestionCommandAndAdviceExamples() {
        val result = processor().process("จำแนกข้อความต่อไปนี้:\n1. วันนี้อากาศเป็นอย่างไร\n2. บันทึกข้อความนี้ลงในความจำ\n3. ฉันควรเลือกเก็บข้อมูลไว้ที่ไหน")
        assertTrue(result.intent == "reasoning.intent_classification")
        assertTrue(result.answer.contains("คำถาม"))
        assertTrue(result.answer.contains("คำสั่ง"))
        assertTrue(result.answer.contains("คำขอคำแนะนำ"))
    }
}
