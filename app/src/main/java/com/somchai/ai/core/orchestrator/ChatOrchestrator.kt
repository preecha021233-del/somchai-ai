package com.somchai.ai.core.orchestrator

import com.somchai.ai.core.brain.Brain
import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.processing.*

data class ChatTurnResult(val answer:String,val intent:String,val engineId:String,val usedFallback:Boolean,val planSteps:List<String> = emptyList())

class ChatOrchestrator(
    private val brain:Brain, private val processor:InternalProcessor,
    private val searchApproved:()->Boolean, private val search:suspend (String)->String,
    private val remoteConfigured:()->Boolean, private val remoteAsk:suspend (List<Pair<String,String>>,String)->String,
    private val tasks:TaskExecutionStore, private val planner:TaskPlanner,
    private val audit:(String,String)->Unit = { _,_-> }, private val evaluator:ResultEvaluator = ResultEvaluator(),
    private val retrieveMemory:(String)->List<String> = { emptyList() }
) {
    suspend fun handle(userText:String, history:List<Pair<String,String>> = emptyList(),
        approvedSkills:List<String> = emptyList(), memoryContext:List<String> = emptyList()):ChatTurnResult {
        val text=userText.trim()
        require(text.isNotEmpty()) { "กรุณาพิมพ์ข้อความก่อนครับ" }
        require(text.length<=6000) { "ข้อความยาวเกินกำหนด" }
        val normalized=text.lowercase()
        if(normalized=="/ai" || normalized.startsWith("/ai ")) {
            val prompt=text.drop(3).trim()
            if(prompt.isEmpty()) return ChatTurnResult("ใช้รูปแบบ: /ai ตามด้วยคำถาม","llm.help","policy",true)
            if(!remoteConfigured()) return ChatTurnResult("ยังไม่ได้ตั้งค่า LLM Provider ครับ","llm.not_configured","policy",true)
            return try {
                val answer=remoteAsk(history,prompt)
                safeAudit("LLM_EXPLICIT_CALL","chars="+prompt.length)
                ChatTurnResult(answer,"llm.explicit","remote-provider",false)
            } catch(e:Exception) {
                safeAudit("LLM_EXPLICIT_ERROR",e.javaClass.simpleName)
                ChatTurnResult("เรียก AI ภายนอกไม่สำเร็จ ("+e.javaClass.simpleName+")","llm.error","remote-provider",true)
            }
        }
        // Owner-invoked diagnostic route bypasses deterministic rule processing.
        if(normalized=="/model-test" || normalized.startsWith("/model-test ")) {
            val prompt=text.substringAfter(' ', "").trim()
            if(prompt.isEmpty()) return ChatTurnResult("ใช้รูปแบบ: /model-test ตามด้วยคำถาม เพื่อทดสอบโมเดลภายในโดยตรง","model_test.help","policy",true)
            return try {
                val reply=brain.respond(BrainRequest(prompt))
                safeAudit("LOCAL_MODEL_TEST","engine="+reply.engineId+" fallback="+reply.usedFallback+" reason="+(reply.failureReason ?: "none"))
                if(reply.usedFallback) ChatTurnResult("ทดสอบโมเดลภายในไม่ผ่าน: "+(reply.failureReason ?: "ระบบใช้คำตอบสำรอง")+"\n\nคำตอบสำรอง: "+reply.text,"model_test.failed",reply.engineId,true)
                else ChatTurnResult(reply.text,"model_test.passed",reply.engineId,false)
            } catch(e:kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e:Exception) {
                safeAudit("LOCAL_MODEL_TEST_ERROR",e.javaClass.simpleName+": "+e.message.orEmpty().take(200))
                ChatTurnResult("ทดสอบโมเดลภายในไม่สำเร็จ: "+e.javaClass.simpleName+": "+e.message.orEmpty().take(200),"model_test.error","local-model",true)
            }
        }
        if(normalized=="/search" || normalized.startsWith("/search ")) {
            val query=text.drop(7).trim()
            if(query.isEmpty()) return ChatTurnResult("ใช้รูปแบบ: /search ตามด้วยคำค้น","research.help","policy",true)
            if(!searchApproved()) return ChatTurnResult("ปลั๊กอิน Web Search ยังไม่ได้รับอนุมัติ","research.plugin_required","policy",true)
            return try { ChatTurnResult(search(query),"research.search","research-tool",false) }
            catch(e:Exception) {
                safeAudit("RESEARCH_ORCH_ERROR",e.javaClass.simpleName)
                ChatTurnResult("ค้นคว้าไม่สำเร็จ ("+e.javaClass.simpleName+")","research.error","research-tool",true)
            }
        }
        if(normalized=="/task") return ChatTurnResult("ใช้รูปแบบ: /task ตามด้วยเป้าหมายงาน หรือ /task list","task.help","policy",true)
        if(normalized.startsWith("/task ")) {
            val goal=text.substringAfter(' ').trim()
            if(goal.isEmpty()) return ChatTurnResult("กรุณาระบุเป้าหมายงาน","task.help","policy",true)
            val plan=planner.plan(goal)
            val record=tasks.create(plan)
            val started=runCatching { tasks.startStep(record.id,0) }.getOrNull()
            val answer=buildString {
                append("สร้างงานและบันทึกแล้ว รหัส ").append(record.id.take(8)).append("\n")
                append("ประเภท: ").append(plan.type.name).append("\n")
                append("เป้าหมาย: ").append(plan.goal).append("\n")
                plan.steps.forEachIndexed { i,s -> append(i+1).append(". ").append(s).append("\n") }
                if(started!=null) append("ขั้นตอนที่ 1: ").append(started.steps.first().title).append(" — กำลังติดตามสถานะ (ยังไม่ได้ดำเนินการภายนอก)")
                else append("บันทึกแผนแล้ว แต่เริ่มขั้นตอนแรกไม่สำเร็จ")
            }
            safeAudit("CHAT_TASK_CREATED","id="+record.id+" type="+plan.type)
            return ChatTurnResult(answer,"task.create","task-store",true,plan.steps)
        }
        val deterministic = if (text.length <= 4000) {
            processor.process(text)
        } else {
            ProcessingResult(
                "ข้อความยาวเกินขีดจำกัดของระบบประมวลผลสำรอง 4,000 ตัวอักษร โมเดลภายในอาจยังประมวลผลได้",
                "unsupported"
            )
        }
        if(deterministic.intent!="unsupported") {
            val checked=evaluator.evaluate(deterministic)
            safeAudit("CHAT_TURN","intent="+checked.intent+" engine=internal-rule safe="+checked.safe)
            return ChatTurnResult(checked.text,checked.intent,"internal-processor",true,deterministic.plan)
        }
        return try {
            // Retrieve only a small, relevant set for the local brain. This path is
            // intentionally below the explicit /ai route, so private memory is never
            // attached to a remote-provider request.
            val selectedMemory = (memoryContext + runCatching { retrieveMemory(text) }.getOrDefault(emptyList()))
                .filter { it.isNotBlank() }
                .distinct()
                .take(5)
            val reply=brain.respond(BrainRequest(text,history,approvedSkills,selectedMemory))
            val result=if(reply.usedFallback) deterministic else ProcessingResult(reply.text,"brain.local")
            val checked=evaluator.evaluate(result)
            safeAudit("CHAT_TURN","intent="+checked.intent+" engine="+reply.engineId+" fallback="+reply.usedFallback+" failure="+(reply.failureReason ?: "none")+" safe="+checked.safe)
            ChatTurnResult(checked.text,checked.intent,reply.engineId,reply.usedFallback,result.plan)
        } catch(e:Exception) {
            safeAudit("CHAT_BRAIN_ERROR",e.javaClass.simpleName)
            val checked=evaluator.evaluate(deterministic)
            ChatTurnResult(checked.text,checked.intent,"offline-fallback",true)
        }
    }
    private fun safeAudit(event:String,detail:String) { runCatching { audit(event,detail) } }
}
