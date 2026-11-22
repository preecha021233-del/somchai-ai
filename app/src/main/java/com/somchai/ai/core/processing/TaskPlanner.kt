package com.somchai.ai.core.processing

enum class TaskType { RESEARCH, EXAM, TRAVEL, DEVELOPMENT, TROUBLESHOOTING, WRITING, GENERAL }

data class TaskPlan(val goal: String, val type: TaskType, val steps: List<String>)

/**
 * Offline task decomposition. Templates are selected by explicit task intent;
 * this is a bounded planner, not a language model or an execution engine.
 */
class TaskPlanner {
    fun plan(goal: String): TaskPlan {
        val clean = goal.trim().take(1000)
        require(clean.isNotEmpty()) { "Task description cannot be empty" }
        val type = classify(clean)
        val steps = when (type) {
            TaskType.EXAM -> listOf(
                "กำหนดวิชา ขอบเขตเนื้อหา และวันสอบ",
                "จัดตารางทบทวนบทเรียนตามหัวข้อ",
                "ทำข้อสอบเก่าและจับเวลาจริง",
                "ตรวจข้อผิดพลาดและทบทวนจุดที่ยังอ่อน",
                "พักผ่อนและเตรียมอุปกรณ์ก่อนวันสอบ"
            )
            TaskType.TRAVEL -> listOf(
                "กำหนดจุดหมาย วันเดินทาง งบประมาณ และผู้เดินทาง",
                "เปรียบเทียบเส้นทางและที่พัก",
                "จองการเดินทางและที่พักหลังเจ้าของอนุมัติ",
                "จัดเตรียมเอกสาร เงิน และสัมภาระ",
                "ตรวจแผนการเดินทางและออกเดินทาง"
            )
            TaskType.RESEARCH -> listOf(
                "กำหนดคำถามและขอบเขตการค้นคว้า",
                "ค้นข้อมูลจากแหล่งที่ได้รับอนุญาต",
                "ตรวจวันที่และความน่าเชื่อถือของแต่ละแหล่ง",
                "เปรียบเทียบข้อค้นพบจากอย่างน้อยสองแหล่ง",
                "สรุปข้อเท็จจริง ความไม่แน่นอน และแหล่งอ้างอิง"
            )
            TaskType.DEVELOPMENT -> listOf(
                "ระบุผลลัพธ์ ข้อกำหนด และเกณฑ์ยอมรับ",
                "ตรวจสอบโครงสร้างไฟล์และส่วนประกอบที่เกี่ยวข้อง",
                "ออกแบบการเปลี่ยนแปลงและแยกงานย่อย",
                "พัฒนาโดยจำกัดสิทธิ์และขอบเขตการแก้ไข",
                "รันทดสอบทั้งกรณีปกติและกรณีผิดพลาด",
                "ตรวจผลกระทบและรายงานสิ่งที่ผ่านหรือยังไม่ผ่าน"
            )
            TaskType.TROUBLESHOOTING -> listOf(
                "รวบรวมอาการ ขั้นตอนที่ทำให้เกิดปัญหา และ log",
                "ทำซ้ำปัญหาในสภาพแวดล้อมที่ควบคุมได้",
                "จัดลำดับสมมติฐานตามหลักฐาน",
                "แก้สาเหตุที่ยืนยันได้โดยเปลี่ยนแปลงให้น้อยที่สุด",
                "ทดสอบซ้ำและตรวจการถดถอยของระบบ"
            )
            TaskType.WRITING -> listOf(
                "กำหนดผู้อ่าน วัตถุประสงค์ และรูปแบบเอกสาร",
                "รวบรวมประเด็นและหลักฐานที่ต้องใช้",
                "จัดโครงเรื่องและลำดับเนื้อหา",
                "เขียนร่างและตรวจความถูกต้อง",
                "ปรับภาษาและตรวจฉบับสมบูรณ์"
            )
            TaskType.GENERAL -> listOf(
                "ระบุเป้าหมาย ผลลัพธ์ที่ต้องการ และข้อจำกัด",
                "แยกงานเป็นขั้นตอนย่อยที่ตรวจสอบได้",
                "ระบุข้อมูลและเครื่องมือที่จำเป็น",
                "ดำเนินการเฉพาะขั้นตอนที่ได้รับอนุญาต",
                "ตรวจผลเทียบกับเป้าหมายและสรุปข้อจำกัด"
            )
        }
        return TaskPlan(clean, type, steps)
    }

    fun classify(goal: String): TaskType {
        val text = goal.lowercase()
        // More specific intents take priority. In Thai, "ทดสอบ" contains the
        // substring "สอบ" but is not an exam-planning request.
        val examText = text.replace("ทดสอบ", "")
        return when {
            containsAny(text, listOf("เขียนบทความ", "เขียนรายงาน", "ร่างเอกสาร", "เรียบเรียง", "write", "draft")) -> TaskType.WRITING
            containsAny(text, listOf("แก้", "ซ่อม", "ปัญหา", "error", "fix", "troubleshoot")) -> TaskType.TROUBLESHOOTING
            containsAny(examText, listOf("สอบ", "ข้อสอบ", "ทบทวนบทเรียน", "exam", "test preparation")) -> TaskType.EXAM
            containsAny(text, listOf("เดินทาง", "ท่องเที่ยว", "เที่ยว", "trip", "travel")) -> TaskType.TRAVEL
            containsAny(text, listOf("ค้นคว้า", "วิจัย", "research", "ศึกษา")) -> TaskType.RESEARCH
            containsAny(text, listOf("พัฒนา", "เขียนโค้ด", "โปรแกรม", "แอป", "build", "develop", "code")) -> TaskType.DEVELOPMENT
            else -> TaskType.GENERAL
        }
    }

    private fun containsAny(text: String, values: List<String>): Boolean =
        values.any { text.contains(it, ignoreCase = true) }
}
