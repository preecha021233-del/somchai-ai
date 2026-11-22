package com.somchai.ai.core.processing

import com.somchai.ai.core.memory.LocalMemoryStore

data class ProcessingResult(val answer: String, val intent: String, val plan: List<String> = emptyList())

/** Offline-first deterministic core. It never opens a network connection. */
class InternalProcessor(
    private val memory: LocalMemoryStore,
    private val planner: TaskPlanner = TaskPlanner(),
    private val knowledge: BasicKnowledgeEngine = BasicKnowledgeEngine(),
    private val reasoning: OfflineReasoningEngine = OfflineReasoningEngine()
) {
    fun process(input: String): ProcessingResult {
        val text = input.trim()
        require(text.isNotEmpty()) { "กรุณาพิมพ์คำสั่งก่อนครับ" }
        require(text.length <= 4000) { "ข้อความยาวเกิน 4,000 ตัวอักษร" }
        val normalized = text.lowercase()
        if (normalized in listOf("/help", "help", "ช่วยเหลือ", "คำสั่ง")) return ProcessingResult(helpText(), "help")
        if (normalized.startsWith("/remember ")) {
            val entry = memory.add(text.removeRange(0, "/remember ".length).trim())
            return ProcessingResult("บันทึกความจำในเครื่องแล้วครับ (รหัส ${entry.id.take(8)})", "memory.write")
        }
        if (normalized.startsWith("/recall")) {
            val entries = memory.search(text.removePrefix("/recall").trim(), 8)
            val answer = if (entries.isEmpty()) "ยังไม่พบความจำที่ตรงกับคำค้นครับ"
                else entries.joinToString("\n") { "• [${it.id.take(8)}] ${it.content}" }
            return ProcessingResult(answer, "memory.read")
        }
        if (normalized.startsWith("/forget ")) {
            val id = text.removeRange(0, "/forget ".length).trim()
            val match = memory.all(500).firstOrNull { it.id == id || it.id.startsWith(id) }
                ?: return ProcessingResult("ไม่พบรหัสความจำนี้ครับ", "memory.delete")
            val deleted = memory.delete(match.id)
            return ProcessingResult(if (deleted) "ลบความจำรายการนั้นแล้วครับ" else "ลบความจำไม่สำเร็จครับ", "memory.delete")
        }
        if (normalized.startsWith("/plan ")) return planResult(text.removeRange(0, "/plan ".length))
        if (isGreeting(normalized)) return ProcessingResult(
            "สวัสดีครับ ผม SOMCHAI ระบบประมวลผลภายในพร้อมทำงานพื้นฐานครับ พิมพ์ /help เพื่อดูคำสั่งที่รองรับ", "greeting"
        )
        if (containsAny(normalized, listOf("คุณทำอะไรได้บ้าง", "ทำอะไรได้บ้าง", "ความสามารถของคุณ", "what can you do"))) {
            return ProcessingResult(
                "ตอนนี้ผมทำงานพื้นฐานแบบออฟไลน์ได้ครับ:\n" +
                    "• ตอบคำทักทายและคำสั่งพื้นฐานตามกฎภายใน\n" +
                    "• แยกงานเป็นขั้นตอนด้วย Task Planner\n" +
                    "• คำนวณนิพจน์และโจทย์ตัวเลขรูปแบบที่รองรับ\n" +
                    "• จำแนกตัวอย่างคำถาม คำสั่ง และคำขอคำแนะนำ\n" +
                    "• บันทึก ค้นคืน และลบความจำในเครื่อง\n" +
                    "• ค้นเว็บเมื่อสั่ง /search และปลั๊กอินค้นหาผ่านการอนุมัติแล้ว\n" +
                    "• ตรวจสอบและจัดการปลั๊กอินตามรายการ Operation ที่ Core อนุญาต\n\n" +
                    "ระบบนี้เป็นเครื่องยนต์กฎและการคำนวณที่มีขอบเขต ไม่ใช่โมเดลภาษาที่คิดได้อิสระครับ",
                "capabilities"
            )
        }
        reasoning.classifyExamples(text)?.let { return ProcessingResult(it, "reasoning.intent_classification") }
        reasoning.solveArithmetic(text)?.let { return ProcessingResult(it, "reasoning.arithmetic") }
        if (containsAny(normalized, listOf("วางแผน", "แผนงาน", "แบ่งงาน", "ขั้นตอน"))) return planResult(text)
        knowledge.answer(text)?.let { return ProcessingResult(it, "knowledge.fact") }
        if (normalized.startsWith("จำไว้ว่า ") || normalized.startsWith("จำว่า ")) {
            val value = text.removePrefix("จำไว้ว่า ").removePrefix("จำว่า ").trim()
            val entry = memory.add(value, "owner-note")
            return ProcessingResult("บันทึกไว้ในความจำภายในแล้วครับ (รหัส ${entry.id.take(8)})", "memory.write")
        }
        return ProcessingResult(
            "ผมรับคำสั่งแล้วครับ แต่ยังไม่มีกฎภายในที่ตรงกับคำขอนี้\n" +
                "ผมไม่ส่งข้อความไปยัง AI ภายนอกเองครับ\n" +
                "ใช้ /plan ตามด้วยงาน เพื่อให้ผมแยกขั้นตอน หรือ /help เพื่อดูคำสั่งที่รองรับ",
            "unsupported"
        )
    }

    private fun planResult(task: String): ProcessingResult {
        val plan = planner.plan(task)
        val answer = buildString {
            append("แผนดำเนินงานสำหรับ: ").append(plan.goal).append("\n")
            plan.steps.forEachIndexed { index, step -> append(index + 1).append(". ").append(step).append("\n") }
            append("หมายเหตุ: นี่คือแผนจากกฎภายใน ยังไม่ได้ลงมือทำขั้นตอนเหล่านี้")
        }
        return ProcessingResult(answer, "task.plan", plan.steps)
    }
    private fun isGreeting(text: String): Boolean =
        text in listOf("สวัสดี", "หวัดดี", "hello", "hi", "hey", "good morning") || text.startsWith("สวัสดี somchai")
    private fun containsAny(text: String, values: List<String>): Boolean = values.any { text.contains(it, ignoreCase = true) }
    private fun helpText(): String =
        "คำสั่งภายในที่รองรับ:\n" +
            "/help — แสดงคำสั่ง\n" +
            "/plan งาน — แยกงานเป็นขั้นตอน\n" +
            "/task list|show|start|complete|fail|retry|cancel — จัดการสถานะงาน\n" +
            "/remember ข้อความ — บันทึกความจำในเครื่อง\n" +
            "/recall คำค้นหรือรหัส — ค้นคืนความจำ\n" +
            "/forget รหัส — ลบความจำตามรหัส\n" +
            "/search คำค้น — ค้นเว็บเมื่อปลั๊กอินได้รับอนุมัติแล้ว\n" +
            "/skill list — แสดงทักษะที่เรียนรู้และสถานะ\n" +
            "/skill learn ชื่อ | คำกระตุ้น | ประสบการณ์ | input => expected ;; input => expected — ให้โมเดลสกัดทักษะจากประสบการณ์\n" +
            "/skill propose ชื่อ | คำกระตุ้น | ขั้นตอน | input => expected ;; input => expected — เสนอทักษะใหม่\n" +
            "/skill test รหัส — ทดสอบด้วยโมเดลภายใน\n" +
            "/skill approve รหัส — อนุมัติทักษะที่ผ่านทุกการทดสอบ\n" +
            "พิมพ์ 'คุณทำอะไรได้บ้าง' เพื่อดูความสามารถปัจจุบัน"
}
