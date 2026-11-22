package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskExecutionStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun createsStartsAndPersistsTaskAcrossStoreReopen() {
        val directory = temporaryFolder.newFolder("tasks")
        val store = TaskExecutionStore(directory)
        val created = store.create(TaskPlanner().plan("สอบปลายภาค"))
        assertEquals(TaskStepStatus.PENDING, created.steps.first().status)

        val started = store.startStep(created.id, 0)
        assertEquals(TaskStepStatus.IN_PROGRESS, started.steps.first().status)

        val reopened = TaskExecutionStore(directory)
        assertEquals(TaskStepStatus.IN_PROGRESS, reopened.get(created.id)?.steps?.first()?.status)
        val completed = reopened.completeStep(created.id, 0)
        assertEquals(TaskStepStatus.COMPLETED, completed.steps.first().status)
    }

    @Test fun cannotStartLaterStepBeforeEarlierStepCompletes() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("order"))
        val task = store.create(TaskPlanner().plan("พัฒนาแอป"))
        assertThrows(IllegalArgumentException::class.java) { store.startStep(task.id, 1) }
    }

    @Test fun cannotStartTwoStepsAtOnce() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("single-active"))
        val task = store.create(TaskPlanner().plan("พัฒนาแอป"))
        store.startStep(task.id, 0)
        assertThrows(IllegalArgumentException::class.java) { store.startStep(task.id, 1) }
    }

    @Test fun rejectsUnknownTask() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("unknown"))
        assertThrows(IllegalArgumentException::class.java) { store.startStep("missing", 0) }
    }
}
