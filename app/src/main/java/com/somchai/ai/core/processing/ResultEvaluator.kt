package com.somchai.ai.core.processing

data class EvaluatedAnswer(val text:String,val intent:String,val safe:Boolean,val notes:List<String> = emptyList())

/** Final response gate for known failure patterns; not a general fact checker. */
class ResultEvaluator(private val reasoning: OfflineReasoningEngine = OfflineReasoningEngine()) {
    fun evaluate(result:ProcessingResult):EvaluatedAnswer {
        val notes=mutableListOf<String>()
        var text=result.answer.trim()
        if(text.isBlank()) return EvaluatedAnswer("ระบบไม่มีคำตอบที่ตรวจสอบได้สำหรับคำขอนี้",result.intent,false,listOf("empty-answer"))
        val blocked=listOf("ได้จองให้แล้ว","ชำระเงินเรียบร้อย","ส่งอีเมลแล้ว","ติดตั้งปลั๊กอินโดยอัตโนมัติแล้ว","เรียก AI ภายนอกโดยอัตโนมัติ")
            .firstOrNull { text.contains(it,true) }
        var safe=blocked==null
        if(blocked!=null) {
            notes += "blocked-claim:"+blocked
            text="ระบบระงับคำตอบที่อ้างว่าทำผลข้างเคียงภายนอกแล้วโดยไม่มีหลักฐานยืนยัน"
        }
        if(result.intent=="reasoning.arithmetic" && !reasoning.verifyArithmeticAnswer(text)) {
            safe=false
            notes += "arithmetic-verification-failed"
            text="ระบบตรวจสอบวิธีคำนวณแล้วพบว่าผลลัพธ์ยังยืนยันไม่ได้ จึงไม่แสดงคำตอบตัวเลขครับ"
        }
        if(result.intent=="task.plan" && !text.contains("ยังไม่ได้ลงมือทำ")) {
            text += "\nหมายเหตุ: นี่คือแผนจากกฎภายใน ยังไม่ได้ลงมือทำขั้นตอนเหล่านี้"
            notes += "appended-plan-disclaimer"
        }
        if(result.intent=="unsupported") notes += "unsupported-intent"
        if(text.length>12000) { text=text.take(12000)+"\n…(ตัดความยาวเพื่อความปลอดภัย)"; notes += "truncated" }
        return EvaluatedAnswer(text,result.intent,safe,notes)
    }
}
