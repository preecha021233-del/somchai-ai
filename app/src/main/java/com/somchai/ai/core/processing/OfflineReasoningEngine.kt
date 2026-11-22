package com.somchai.ai.core.processing

import java.math.BigDecimal
import java.math.RoundingMode

/** Bounded deterministic offline classification and arithmetic; not a generative model. */
class OfflineReasoningEngine {
    fun classifyExamples(input: String): String? {
        val examples = Regex("(?m)^\\s*\\d+[.)、]\\s*(.+)$")
            .findAll(input).map { it.groupValues[1].trim() }.toList()
        if (examples.isEmpty()) return null
        val results = examples.mapIndexed { index, example ->
            val kind = classify(example) ?: return null
            "${index + 1}. $example\nประเภท: ${kind.first}\nเหตุผล: ${kind.second}"
        }
        return "ผลการแยกประเภทข้อความ:\n" + results.joinToString("\n\n")
    }

    private fun classify(text: String): Pair<String, String>? {
        val value = text.trim()
        if (value.isEmpty()) return null
        if (listOf("ควร", "แนะนำ", "ดีไหม", "อย่างไหนดี", "เลือกอะไร").any(value::contains))
            return "คำขอคำแนะนำ" to "ผู้ส่งต้องการข้อเสนอหรือแนวทางช่วยตัดสินใจ"
        if (listOf("บันทึก", "ลบ", "เปิด", "ปิด", "สร้าง", "ส่ง", "คำนวณ", "ช่วยทำ").any(value::contains) &&
            !value.endsWith("?") && !value.contains("ไหม"))
            return "คำสั่ง" to "ข้อความระบุให้ดำเนินการหรือทำบางสิ่ง"
        if (value.endsWith("?") || listOf("ไหม", "หรือไม่", "อย่างไร", "เท่าไร", "อะไร", "ที่ไหน", "เมื่อไร", "กี่").any(value::contains))
            return "คำถาม" to "ข้อความต้องการข้อมูลหรือคำตอบ"
        return null
    }

    fun solveArithmetic(input: String): String? {
        solveChangeProblem(input)?.let { return it }
        solveBatteryProblem(input)?.let { return it }
        val expression = Regex("""(?<![\p{L}\d])[+-]?(?:\d|\()[\d,.()+\-×*/÷\s]*""")
            .find(input)?.value ?: return null
        val result = runCatching { ExpressionParser(expression).parse() }.getOrNull() ?: return null
        return "วิธีคำนวณ:\n${expression.replace("×", "*").replace("÷", "/")} = ${format(result)}"
    }

    private fun solveChangeProblem(input: String): String? {
        if (!input.contains("เงินทอน") || !input.contains("ราคา") || !input.contains("จ่าย")) return null
        val priceSection = Regex("ราคา(.*?)(?:จ่าย|ชำระ)", RegexOption.DOT_MATCHES_ALL)
            .find(input)?.groupValues?.getOrNull(1) ?: return null
        val prices = numberRegex.findAll(priceSection).mapNotNull { it.value.replace(",", "").toBigDecimalOrNull() }.toList()
        val paid = Regex("(?:จ่าย|ชำระ)(?:เงิน)?(?:ไป|เป็น)?\\s*([\\d,]+(?:\\.\\d+)?)")
            .find(input)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        if (prices.isEmpty()) return null
        val total = prices.fold(BigDecimal.ZERO, BigDecimal::add)
        val change = paid.subtract(total)
        if (change.signum() < 0) return "ยอดสินค้า ${format(total)} บาท มากกว่าเงินที่จ่าย ${format(paid)} บาท จึงยังมีเงินไม่พอ"
        return "วิธีคำนวณ:\nราคารวม = ${prices.joinToString(" + ") { format(it) }} = ${format(total)} บาท\n" +
            "เงินทอน = เงินที่จ่าย - ราคารวม\nเงินทอน = ${format(paid)} - ${format(total)} = ${format(change)} บาท"
    }

    private fun solveBatteryProblem(input: String): String? {
        if (!input.contains("แบตเตอรี่") || !input.contains("เครื่องละ")) return null
        val beforeCondition = input.substringBefore("ถ้าใช้")
        val capacities = Regex("""(\d+(?:\.\d+)?)\s*%""")
            .findAll(beforeCondition).mapNotNull { it.groupValues[1].toBigDecimalOrNull() }.toList()
        val used = Regex("""เครื่องละ\s*(\d+(?:\.\d+)?)\s*%""")
            .find(input)?.groupValues?.getOrNull(1)?.toBigDecimalOrNull() ?: return null
        if (capacities.isEmpty() || capacities.size > 10 || capacities.any { it < used || it > BigDecimal("100") }) return null
        val remaining = capacities.map { it.subtract(used) }
        return "วิธีคำนวณ: หักออกเครื่องละ ${format(used)} จุดเปอร์เซ็นต์\n" +
            remaining.mapIndexed { index, value -> "เครื่องที่ ${index + 1}: ${format(value.add(used))}% - ${format(used)}% = ${format(value)}%" }
                .joinToString("\n")
    }

    private fun format(value: BigDecimal): String =
        value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    private val numberRegex = Regex("""\d[\d,]*(?:\.\d+)?""")

    private class ExpressionParser(source: String) {
        private val normalizedSource = source.replace(",", "").filterNot { it.isWhitespace() }
        private val tokens = run {
            val matches = Regex("""\d+(?:\.\d+)?|[+\-×*/÷()]""").findAll(normalizedSource).toList()
            var consumed = 0
            matches.forEach { match ->
                require(match.range.first == consumed) { "Invalid character in expression" }
                consumed = match.range.last + 1
            }
            require(consumed == normalizedSource.length) { "Invalid trailing input" }
            matches.map { it.value }
        }
        private var index = 0
        fun parse(): BigDecimal {
            require(tokens.isNotEmpty())
            val value = expression()
            require(index == tokens.size)
            return value
        }
        private fun expression(): BigDecimal {
            var value = term()
            while (peek("+") || peek("-")) {
                val op = tokens[index++]
                val right = term()
                value = if (op == "+") value.add(right) else value.subtract(right)
            }
            return value
        }
        private fun term(): BigDecimal {
            var value = factor()
            while (peek("*") || peek("/") || peek("×") || peek("÷")) {
                val op = tokens[index++]
                val right = factor()
                value = if (op == "*" || op == "×") value.multiply(right)
                    else value.divide(right, 8, RoundingMode.HALF_UP)
            }
            return value
        }
        private fun factor(): BigDecimal {
            if (peek("-")) { index++; return factor().negate() }
            if (peek("+")) { index++; return factor() }
            if (peek("(")) {
                index++
                val value = expression()
                require(peek(")"))
                index++
                return value
            }
            return (tokens.getOrNull(index++) ?: error("Missing number")).toBigDecimal()
        }
        private fun peek(token: String): Boolean = tokens.getOrNull(index) == token
    }
}
