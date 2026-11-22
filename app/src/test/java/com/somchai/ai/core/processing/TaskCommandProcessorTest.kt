package com.somchai.ai.core.processing

import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskCommandProcessorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun chatCommandsDriveTaskLifecycleAndUseOneBasedStepNumbers() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("commands"))
        val task = store.create(TaskPlanner().plan("เขียนรายงาน"))
        val commands = TaskCommandProcessor(store)

        assertTrue(commands.execute("/task list").contains(task.id.take(8)))
        assertTrue(commands.execute("/task start ${task.id.take(8)} 1").contains("กำลังทำ"))
        assertTrue(commands.execute("/task fail ${task.id.take(8)} 1 | ข้อมูลไม่ครบ").contains("ล้มเหลว"))
        assertTrue(commands.execute("/task show ${task.id.take(8)}").contains("ข้อมูลไม่ครบ"))
        assertTrue(commands.execute("/task retry ${task.id.take(8)} 1").contains("รอดำเนินการ"))
        assertTrue(commands.execute("/task start ${task.id.take(8)} 1").contains("กำลังทำ"))
        assertTrue(commands.execute("/task complete ${task.id.take(8)} 1").contains("เสร็จแล้ว"))
    }

    @Test fun rejectsInvalidStepNumbersAndUnknownCommands() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("invalid"))
        val task = store.create(TaskPlanner().plan("งานทั่วไป"))
        val commands = TaskCommandProcessor(store)
        assertThrows(IllegalArgumentException::class.java) {
            commands.execute("/task start ${task.id.take(8)} 0")
        }
        assertTrue(commands.execute("/task unknown").contains("/task list"))
    }
}
