package com.somchai.ai.core.processing

data class TaskPlan(val goal: String, val steps: List<String>)

/**
 * A deterministic planner for common user requests. It decomposes work into
 * explicit steps; it does not claim model-level reasoning or execute side effects.
 */
class TaskPlanner {
    fun plan(goal: String): TaskPlan {
        val clean = goal.trim().take(1000)
        require(clean.isNotEmpty()) { "Task description cannot be empty" }
        val steps = when {
            containsAny(clean, listOf("ค้นคว้า", "วิจัย", "research", "ศึกษา")) -> listOf(
                "กำหนดคำถามและขอบเขตการค้นคว้า",
                "ค้นข้อมูลจากแหล่งที่ได้รับอนุญาต",
                "เปรียบเทียบแหล่งข้อมูลและวันที่",
                "แยกข้อเท็จจริงออกจากข้อคิดเห็นและความไม่แน่นอน",
                "สรุปผลพร้อมระบุแหล่งที่มา"
            )
            containsAny(clean, listOf("สร้าง", "พัฒนา", "เขียน", "build", "develop")) -> listOf(
                "ระบุผลลัพธ์และข้อกำหนด",
                "ตรวจสอบไฟล์และส่วนประกอบที่เกี่ยวข้อง",
                "ออกแบบการเปลี่ยนแปลงที่เล็กและตรวจสอบได้",
                "พัฒนาในขอบเขตที่กำหนด",
                "รันทดสอบและตรวจสอบผล",
                "รายงานข้อจำกัดและสิ่งที่ยังไม่ผ่าน"
            )
            containsAny(clean, listOf("แก้", "ซ่อม", "ปัญหา", "error", "fix")) -> listOf(
                "รวบรวมอาการและข้อความผิดพลาด",
                "แยกสาเหตุที่เป็นไปได้",
                "ตรวจสอบหลักฐานก่อนแก้ไข",
                "แก้สาเหตุโดยกระทบส่วนอื่นให้น้อยที่สุด",
                "ทดสอบกรณีเดิมและกรณีที่เกี่ยวข้อง"
            )
            else -> listOf(
                "ทำความเข้าใจเป้าหมายและข้อจำกัด",
                "แยกงานเป็นขั้นตอนย่อย",
                "เลือกเครื่องมือที่ได้รับอนุญาต",
                "ดำเนินการและตรวจสอบผล",
                "สรุปผลและสิ่งที่ยังไม่แน่นอน"
            )
        }
        return TaskPlan(clean, steps)
    }

    private fun containsAny(text: String, values: List<String>): Boolean =
        values.any { text.contains(it, ignoreCase = true) }
}
