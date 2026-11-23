package com.somchai.ai.core.processing

import com.somchai.ai.core.memory.LocalMemoryStore
import com.somchai.ai.core.memory.PlaintextMemoryCipher
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InternalProcessorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private fun processor(): InternalProcessor = InternalProcessor(LocalMemoryStore(temporaryFolder.newFolder("memory")))

    @Test fun helpListsTheExactSupportedCommandListInOrder() {
        val answer = processor().process("/help").answer
        val expectedLines = listOf(
            "คำสั่งภายในที่รองรับ:",
            "/help — แสดงคำสั่ง",
            "/plan งาน — แยกงานเป็นขั้นตอน",
            "/planpersist งาน — แยกงานและบันทึกเป็น task",
            "/tasks — แสดงงานที่บันทึกไว้",
            "/task list|show|start|complete|fail|retry|cancel — จัดการสถานะงาน",
            "/remember ข้อความ — บันทึกความจำในเครื่อง",
            "/recall คำค้นหรือรหัส — ค้นคืนความจำ",
            "/forget รหัส — ลบความจำตามรหัส",
            "/search คำค้น — ค้นเว็บเมื่อปลั๊กอิน Web Search ได้รับอนุมัติแล้ว",
            "/plugins — ดูปลั๊กอินที่ใช้งาน รออนุมัติ และปิดใช้งาน",
            "/ai คำถาม — ส่งไปยัง AI ภายนอกเฉพาะเมื่อเจ้าของตั้งค่า Provider และสั่งเอง",
            "/skill list — แสดงทักษะที่เรียนรู้และสถานะ",
            "/skill learn ชื่อ | คำกระตุ้น | ประสบการณ์ | input => expected ;; input => expected — ให้โมเดลภายในสกัดทักษะจากประสบการณ์",
            "/skill propose ชื่อ | คำกระตุ้น | ขั้นตอน | input => expected ;; input => expected — เสนอทักษะใหม่",
            "/skill test รหัส — ทดสอบด้วยโมเดลภายใน",
            "/skill approve รหัส — อนุมัติทักษะที่ผ่านทุกการทดสอบ",
            "/skill delete รหัส — ลบทักษะที่บันทึกไว้",
            "พิมพ์ 'คุณทำอะไรได้บ้าง' เพื่อดูความสามารถปัจจุบัน"
        )
        assertEquals(expectedLines, answer.lines())
    }

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
    @Test fun recallsExactMemoryByIdAndAcceptsUppercaseCommand() {
        val subject = processor()
        val stored = subject.process("/remember ข้อมูลทดสอบรหัสความจำ")
        val id = Regex("\\(รหัส ([0-9a-fA-F-]{8})\\)").find(stored.answer)?.groupValues?.get(1)
        assertTrue("Expected a displayed stable memory ID", id != null)
        val recalled = subject.process("/RECALL $id")
        assertTrue(recalled.intent == "memory.read")
        assertTrue(recalled.answer.contains("ข้อมูลทดสอบรหัสความจำ"))
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

    @Test fun evaluatesArithmeticPrecedenceParenthesesAndUnarySigns() {
        val cases = listOf(
            "คำนวณ 2 + 3 * 4" to "14",
            "คำนวณ (2 + 3) * 4" to "20",
            "คำนวณ 10 - (2 + 3) * 2" to "0",
            "คำนวณ ((2 + 3) * (4 - 1)) / 3" to "5",
            "คำนวณ 2 * (3 + (4 * 2))" to "22",
            "คำนวณ -2 + 3 * 4" to "10",
            "คำนวณ 2 * -(3 + 4)" to "-14",
            "คำนวณ +2.5 * 4" to "10"
        )
        val subject = processor()
        cases.forEach { (input, expected) ->
            val result = subject.process(input)
            assertTrue("Expected $expected from $input, got: ${result.answer}", result.answer.contains("= $expected"))
        }
    }

    @Test fun rejectsMalformedArithmeticInsteadOfReturningPartialResult() {
        val result = processor().process("คำนวณ 2 + * 3")
        assertTrue(result.intent == "unsupported")
    }


    @Test fun answersThunderQuestionWithCausalExplanation() {
        val result = processor().process("ทำไมฟ้าร้อง")
        assertTrue(result.intent == "knowledge.fact")
        assertTrue(result.answer.contains("คลื่นกระแทก"))
    }

    @Test fun explainsBackupAdvantagesAndDisadvantages() {
        val result = processor().process("อธิบายข้อดีข้อเสียของการสำรองข้อมูล")
        assertTrue(result.intent == "knowledge.fact")
        assertTrue(result.answer.contains("ข้อดี"))
        assertTrue(result.answer.contains("ข้อเสีย"))
    }

    @Test fun flagsConflictingWeekdayPremises() {
        val result = processor().process("วันนี้เป็นวันจันทร์ แต่วันนี้เป็นวันอังคาร")
        assertTrue(result.intent == "reasoning.contradiction")
        assertTrue(result.answer.contains("ขัดแย้ง"))
    }

    @Test fun asksForMissingDistanceOrTimeWhenAskedForSpeed() {
        val result = processor().process("รถคันนี้มีความเร็วเท่าไร")
        assertTrue(result.intent == "reasoning.insufficient_information")
        assertTrue(result.answer.contains("ระยะทาง"))
        assertTrue(result.answer.contains("เวลา"))
    }

    @Test fun refusesToInventSourcesWhenNoResearchWasPerformed() {
        val result = processor().process("ขอแหล่งอ้างอิง")
        assertTrue(result.intent == "research.sources_required")
        assertTrue(result.answer.contains("ไม่สร้างรายการอ้างอิง"))
    }

    @Test fun solvesQuantityAndUnitPriceCashChangePrompt() {
        val result = processor().process("ซื้อของ 3 ชิ้น ชิ้นละ 25 บาท จ่าย 100 บาท ได้ทอนเท่าไร")
        assertTrue(result.intent == "reasoning.arithmetic")
        assertTrue(result.answer.contains("25 บาท"))
    }


    @Test fun forgetRejectsAmbiguousShortIdsAndHonorsExactFullId() {
        val directory = temporaryFolder.newFolder("ambiguous-memory-ids")
        val firstId = "12345678-0000-0000-0000-000000000001"
        val secondId = "12345678-0000-0000-0000-000000000002"
        val legacy = listOf(firstId to "รายการแรก", secondId to "รายการที่สอง").joinToString("\n") { (id, content) ->
            JSONObject().put("schemaVersion", 1).put("id", id).put("category", "experience")
                .put("content", content).put("createdAt", "2026-11-23T00:00:00Z").toString()
        }
        java.io.File(directory, "memory.jsonl").writeText(legacy)
        val memory = LocalMemoryStore(directory, PlaintextMemoryCipher)
        val subject = InternalProcessor(memory)

        val ambiguous = subject.process("/forget 12345678")
        assertEquals("memory.delete.ambiguous", ambiguous.intent)
        assertTrue(ambiguous.answer.contains("ใช้รหัสเต็ม"))
        assertEquals(2, memory.all().size)

        val exact = subject.process("/forget $firstId")
        assertEquals("memory.delete", exact.intent)
        assertTrue(exact.answer.contains("ลบความจำ"))
        assertEquals(1, memory.all().size)
        assertFalse(memory.all().any { it.id == firstId })
        assertTrue(memory.all().any { it.id == secondId })
    }

    @Test fun solvesMultiStepThaiPurchaseBudgetProblem() {
        val result = processor().process(
            "มีเงิน 500 บาท ซื้อสมุด 2 เล่ม เล่มละ 30 บาท ซื้อปากกา 3 ด้าม ด้ามละ 10 บาท เหลือเงินเท่าไร"
        )
        assertEquals("reasoning.arithmetic", result.intent)
        assertTrue(result.answer.contains("2 × 30 = 60 บาท"))
        assertTrue(result.answer.contains("3 × 10 = 30 บาท"))
        assertTrue(result.answer.contains("500 - 90 = 410 บาท"))
    }

    @Test fun doesNotCallDifferentWeekdaysForDifferentEventsAContradiction() {
        val result = processor().process("วันนี้เป็นวันจันทร์ และพรุ่งนี้เป็นวันอังคาร")
        assertFalse(result.intent == "reasoning.contradiction")
    }


}
