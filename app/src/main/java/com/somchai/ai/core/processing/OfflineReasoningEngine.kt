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
        // Check interrogative intent before action keywords: e.g. "การบันทึกข้อมูลมีข้อดีอย่างไร"
        // asks for information; the substring "บันทึก" must not turn it into a command.
        if (value.endsWith("?") || listOf(
                "ไหม", "หรือไม่", "หรือเปล่า", "อย่างไร", "เท่าไร", "เท่าไหร่",
                "อะไร", "ที่ไหน", "เมื่อไร", "เมื่อไหร่", "กี่", "ทำไม", "เพราะอะไร"
            ).any(value::contains))
            return "คำถาม" to "ข้อความต้องการข้อมูลหรือคำตอบ"
        val explicitNegation = listOf("ไม่ต้องการ", "ไม่อยาก", "ไม่ให้", "อย่า").any(value::contains)
        if (!explicitNegation &&
            listOf("บันทึก", "ลบ", "เปิด", "ปิด", "สร้าง", "ส่ง", "คำนวณ", "ช่วยทำ").any(value::contains))
            return "คำสั่ง" to "ข้อความระบุให้ดำเนินการหรือทำบางสิ่ง"
        return null
    }

    /** Detects a small set of explicit contradictions without guessing at unstated facts. */
    fun detectContradiction(input: String): String? {
        val text = input.lowercase()
        val weekdays = listOf("วันจันทร์", "วันอังคาร", "วันพุธ", "วันพฤหัสบดี", "วันศุกร์", "วันเสาร์", "วันอาทิตย์")
            .filter { text.contains(it) }
        val explicitConflictConnector = listOf("แต่", "ทว่า", "ขัดแย้ง").any(text::contains)
        val repeatedEventAnchor = listOf("วันนี้", "นัด", "ประชุม", "สอบ", "กิจกรรม", "กำหนดการ")
            .any { anchor -> Regex(Regex.escape(anchor)).findAll(text).count() >= 2 }
        if (weekdays.size > 1 && explicitConflictConnector && repeatedEventAnchor) {
            return "ข้อมูลขัดแย้งกันครับ: ระบุวันในสัปดาห์มากกว่าหนึ่งวันสำหรับเหตุการณ์เดียวกัน (${weekdays.joinToString(" และ ")}) กรุณาระบุวันที่หรือวันที่ถูกต้อง"
        }
        val sameIntegerReference = listOf("จำนวนเต็มเดียวกัน", "จำนวนเดียวกัน", "เลขเดียวกัน")
            .any(text::contains)
        val evenNumbers = Regex("""(?:เลข\s*)?(\d+)\s*(?:เป็น)?\s*เลขคู่""")
            .findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()
        val oddNumbers = Regex("""(?:เลข\s*)?(\d+)\s*(?:เป็น)?\s*เลขคี่""")
            .findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()
        if ((evenNumbers intersect oddNumbers).isNotEmpty() ||
            (sameIntegerReference && text.contains("เลขคู่") && text.contains("เลขคี่"))) {
            return "ข้อมูลขัดแย้งกันครับ: จำนวนเต็มเดียวกันไม่สามารถเป็นทั้งเลขคู่และเลขคี่ได้พร้อมกัน"
        }
        if (text.contains("ไม่เคย") && text.contains("แต่เคย")) {
            val neverTarget = text.substringAfter("ไม่เคย").substringBefore("แต่").trim()
            val everTarget = text.substringAfter("แต่").substringAfter("เคย", "").trim()
                .trimEnd('.', '!', '?', 'ๆ', ' ')
            val normalizedNeverTarget = neverTarget.trimEnd('.', '!', '?', 'ๆ', ' ')
            if (normalizedNeverTarget.isNotEmpty() && normalizedNeverTarget == everTarget) {
                return "ข้อความมีข้อมูลขัดแย้งกัน: ระบุทั้ง 'ไม่เคย' และ 'เคย' กับเรื่องเดียวกัน กรุณาระบุว่าข้อใดถูกต้อง"
            }
        }
        return null
    }

    /** Asks for the specific inputs required by common calculations when they are absent. */
    fun missingInformation(input: String): String? {
        val text = input.lowercase()
        if (text.contains("ความเร็ว") || text.contains("อัตราเร็ว")) {
            val missing = buildList {
                if (!text.contains("ระยะทาง")) add("ระยะทาง")
                if (!text.contains("เวลา")) add("เวลาที่ใช้")
            }
            if (missing.isNotEmpty()) {
                return "การคำนวณความเร็วต้องมีระยะทางและเวลาที่ใช้ครับ ตอนนี้ยังขาด: ${missing.joinToString(" และ ")} กรุณาระบุข้อมูลเพิ่มเติม"
            }
        }
        if (text.contains("เงินทอน") || text.contains("ทอนเท่าไร") || text.contains("ทอนเท่าไหร่")) {
            val missing = buildList {
                if (!(text.contains("ราคา") || text.contains("ชิ้นละ") || text.contains("อันละ"))) add("ราคารวมของสินค้า")
                if (!(text.contains("จ่าย") || text.contains("ชำระ"))) add("จำนวนเงินที่จ่าย")
            }
            if (missing.isNotEmpty()) {
                return "การคำนวณเงินทอนต้องทราบราคารวมของสินค้าและจำนวนเงินที่จ่ายครับ ตอนนี้ยังขาด: ${missing.joinToString(" และ ")} กรุณาระบุข้อมูลเพิ่มเติม"
            }
        }
        return null
    }

    fun solveArithmetic(input: String): String? {
        if (input.length > MAX_ARITHMETIC_INPUT_CHARS) return null
        val numericTokens = numberRegex.findAll(input).toList()
        if (numericTokens.size > MAX_NUMERIC_TOKENS || numericTokens.any { match ->
                match.value.count(Char::isDigit) > MAX_NUMBER_LITERAL_DIGITS
            }) return null
        val asksForChange = listOf("เงินทอน", "ได้ทอน", "ทอนเท่าไร", "ทอนเท่าไหร่").any(input::contains)
        if (asksForChange && !input.contains("จ่าย") && !input.contains("ชำระ")) return null
        solveChangeProblem(input)?.let { return it }
        solveMultiStepBalanceProblem(input)?.let { return it }
        solveBatteryProblem(input)?.let { return it }
        val expression = Regex("""(?<![\p{L}\d])[+-]?(?:\d|\()[\d,.()+\-×*/÷\s]*""")
            .find(input)?.value ?: return null
        val result = runCatching { ExpressionParser(expression).parse() }.getOrNull() ?: return null
        return "วิธีคำนวณ:\n${expression.replace("×", "*").replace("÷", "/")} = ${format(result)}"
    }

    private fun solveChangeProblem(input: String): String? {
        val asksForChange = listOf("เงินทอน", "ได้ทอน", "ทอนเท่าไร", "ทอนเท่าไหร่").any(input::contains)
        if (!asksForChange || (!input.contains("จ่าย") && !input.contains("ชำระ"))) return null

        val priceSection = Regex("ราคา(.*?)(?:จ่าย|ชำระ)", RegexOption.DOT_MATCHES_ALL)
            .find(input)?.groupValues?.getOrNull(1)
        val listedPrices = priceSection?.let { section ->
            numberRegex.findAll(section).mapNotNull {
                it.value.replace(",", "").toBigDecimalOrNull()
            }.toList()
        }.orEmpty()

        val quantityPrice = Regex("""(\d+)\s*(?:ชิ้น|อัน|รายการ).*?(?:ชิ้นละ|อันละ)\s*([\d,]+(?:\.\d+)?)""")
            .find(input)
        val total: BigDecimal
        val calculation: String
        if (quantityPrice != null) {
            val quantity = quantityPrice.groupValues[1].toBigDecimalOrNull() ?: return null
            val unitPrice = quantityPrice.groupValues[2].replace(",", "").toBigDecimalOrNull() ?: return null
            if (quantity <= BigDecimal.ZERO || quantity > BigDecimal("10000") || unitPrice < BigDecimal.ZERO) return null
            total = quantity.multiply(unitPrice)
            calculation = "${format(quantity)} × ${format(unitPrice)} = ${format(total)} บาท"
        } else {
            if (listedPrices.isEmpty()) return null
            total = listedPrices.fold(BigDecimal.ZERO, BigDecimal::add)
            calculation = listedPrices.joinToString(" + ") { format(it) } + " = ${format(total)} บาท"
        }

        val paid = Regex("(?:จ่าย|ชำระ)(?:เงิน)?(?:ไป|เป็น)?\\s*([\\d,]+(?:\\.\\d+)?)")
            .find(input)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val change = paid.subtract(total)
        if (change.signum() < 0) {
            return "ยอดสินค้ารวม ${format(total)} บาท มากกว่าเงินที่จ่าย ${format(paid)} บาท จึงยังมีเงินไม่พอ"
        }
        return "วิธีคำนวณ:\nราคารวม = $calculation\n" +
            "เงินทอน = เงินที่จ่าย - ราคารวม\nเงินทอน = ${format(paid)} - ${format(total)} = ${format(change)} บาท"
    }


    /**
     * Solves a bounded Thai multi-item word problem and shows every intermediate
     * quantity × unit-price calculation before computing the remaining balance.
     */
    private fun solveMultiStepBalanceProblem(input: String): String? {
        val text = input.lowercase()
        if (!listOf("เหลือเงิน", "เงินคงเหลือ", "เหลือเท่าไร", "เหลือเท่าไหร่").any { text.contains(it) }) return null
        val initial = Regex("""(?:มีเงิน|เงินเริ่มต้น|งบประมาณ|งบ)\s*([\d,]+(?:\.\d+)?)\s*บาท""")
            .find(input)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val itemPattern = Regex(
            """(\d+)\s*(?:ชิ้น|เล่ม|ด้าม|อัน|กล่อง|ชุด|รายการ|ขวด|ถุง|ตัว)\s*(?:ชิ้นละ|เล่มละ|ด้ามละ|อันละ|กล่องละ|ชุดละ|รายการละ|ขวดละ|ถุงละ|ตัวละ)\s*([\d,]+(?:\.\d+)?)\s*บาท"""
        )
        val purchases = itemPattern.findAll(input).mapNotNull { match ->
            val quantity = match.groupValues[1].toBigDecimalOrNull() ?: return@mapNotNull null
            val unitPrice = match.groupValues[2].replace(",", "").toBigDecimalOrNull() ?: return@mapNotNull null
            if (quantity <= BigDecimal.ZERO || quantity > BigDecimal("10000") || unitPrice < BigDecimal.ZERO) {
                return@mapNotNull null
            }
            quantity to unitPrice
        }.toList()
        if (purchases.size !in 2..10) return null
        val subtotals = purchases.map { (quantity, price) -> quantity.multiply(price) }
        val spent = subtotals.fold(BigDecimal.ZERO, BigDecimal::add)
        val remaining = initial.subtract(spent)
        if (remaining.signum() < 0) {
            return "ยอดใช้รวม ${format(spent)} บาท มากกว่าเงินเริ่มต้น ${format(initial)} บาท จึงมีเงินไม่พอ"
        }
        val details = purchases.mapIndexed { index, (quantity, price) ->
            "รายการที่ ${index + 1}: ${format(quantity)} × ${format(price)} = ${format(subtotals[index])} บาท"
        }
        return "วิธีคำนวณ:\nเงินเริ่มต้น = ${format(initial)} บาท\n" +
            details.joinToString("\n") + "\n" +
            "ยอดใช้รวม = ${subtotals.joinToString(" + ") { format(it) }} = ${format(spent)} บาท\n" +
            "เงินคงเหลือ = ${format(initial)} - ${format(spent)} = ${format(remaining)} บาท"
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

    companion object {
        private const val MAX_ARITHMETIC_INPUT_CHARS = 4_000
        private const val MAX_NUMERIC_TOKENS = 128
        private const val MAX_NUMBER_LITERAL_DIGITS = 64
        private const val MAX_EXPRESSION_CHARS = 512
        private const val MAX_EXPRESSION_TOKENS = 256
    }

    private class ExpressionParser(source: String) {
        private val normalizedSource = source.replace(",", "").filterNot { it.isWhitespace() }
        private val tokens = run {
            require(normalizedSource.length <= MAX_EXPRESSION_CHARS) { "Expression is too long" }
            val matches = Regex("""\d+(?:\.\d+)?|[+\-×*/÷()]""").findAll(normalizedSource).toList()
            require(matches.size <= MAX_EXPRESSION_TOKENS) { "Expression has too many operations" }
            require(matches.filter { it.value.firstOrNull()?.isDigit() == true }
                .all { it.value.length <= MAX_NUMBER_LITERAL_DIGITS }) { "Numeric literal is too long" }
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
