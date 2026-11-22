package com.somchai.ai.core.processing

/**
 * Small offline fact layer for common, stable questions.
 *
 * This is a deliberately bounded knowledge base, not a general language model.
 * Unknown questions return null so the caller can ask for clarification or
 * report the current limitation honestly.
 */
class BasicKnowledgeEngine {
    fun answer(question: String): String? {
        val q = question.trim().lowercase()
            .replace("ไหม", "")
            .replace("หรือเปล่า", "")
            .replace("ครับ", "")
            .replace("คะ", "")
            .replace("ค่ะ", "")
            .replace(Regex("\\s+"), "")

        return when {
            listOf("แมวมีกี่ขา", "แมวมีขากี่ข้าง", "แมวมีกี่ข้าง").any(q::contains) ->
                "แมวทั่วไปมี 4 ขาครับ ใช้เดิน วิ่ง กระโดด และทรงตัว"
            listOf("สุนัขมีกี่ขา", "หมามีกี่ขา", "หมามีขากี่ข้าง").any(q::contains) ->
                "สุนัขทั่วไปมี 4 ขาครับ"
            listOf("คนมีกี่ขา", "มนุษย์มีกี่ขา").any(q::contains) ->
                "มนุษย์ทั่วไปมี 2 ขาครับ"
            listOf("ปลาอยู่ที่ไหน", "ปลาอาศัยอยู่ที่ไหน").any(q::contains) ->
                "ปลาส่วนใหญ่อาศัยอยู่ในน้ำ ทั้งน้ำจืดและน้ำเค็ม ขึ้นอยู่กับชนิดของปลาครับ"
            listOf("ดวงอาทิตย์ขึ้นทางไหน", "พระอาทิตย์ขึ้นทางไหน").any(q::contains) ->
                "ดวงอาทิตย์ขึ้นทางทิศตะวันออกโดยประมาณ และตกทางทิศตะวันตกครับ"
            listOf("โลกมีกี่ดวงจันทร์", "โลกมีดวงจันทร์กี่ดวง").any(q::contains) ->
                "โลกมีดวงจันทร์ตามธรรมชาติ 1 ดวงครับ"
            listOf("น้ำเดือดกี่องศา", "จุดเดือดของน้ำ").any(q::contains) ->
                "ที่ความดันบรรยากาศมาตรฐาน น้ำบริสุทธิ์เดือดที่ประมาณ 100 องศาเซลเซียสครับ"
            else -> null
        }
    }
}
