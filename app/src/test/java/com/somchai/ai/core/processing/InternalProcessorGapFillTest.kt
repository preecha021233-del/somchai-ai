package com.somchai.ai.core.processing

import com.somchai.ai.core.memory.LocalMemoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InternalProcessorGapFillTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun planPersistCreatesDurableTask() {
        val tasks = TaskExecutionStore(temp.newFolder("tasks"))
        val processor = InternalProcessor(LocalMemoryStore(temp.newFolder("memory")), tasks = tasks)
        val result = processor.process("/planpersist พัฒนาแอป")
        assertEquals("task.plan", result.intent)
        assertTrue(result.answer.contains("บันทึกแผนเป็นงานรหัส"))
        assertTrue(result.answer.contains("ยังไม่ได้ลงมือทำ"))
        assertEquals(1, tasks.all().size)
    }

    @Test fun tasksCommandListsPersistedTasks() {
        val tasks = TaskExecutionStore(temp.newFolder("tasks"))
        tasks.create(TaskPlanner().plan("พัฒนาแอป SOMCHAI"))
        val processor = InternalProcessor(LocalMemoryStore(temp.newFolder("memory")), tasks = tasks)
        val result = processor.process("/tasks")
        assertEquals("task.list", result.intent)
        assertTrue(result.answer.contains("พัฒนาแอป SOMCHAI"))
    }
}
