package com.somchai.ai.core.processing

import com.somchai.ai.core.memory.LocalMemoryStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InternalProcessorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun processor(): InternalProcessor {
        val directory = temporaryFolder.newFolder("memory")
        return InternalProcessor(LocalMemoryStore(directory))
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
}
