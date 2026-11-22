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

    @Test fun failedStepCanBeRetriedAndFailureSurvivesReopen() {
        val directory = temporaryFolder.newFolder("retry")
        val store = TaskExecutionStore(directory)
        val task = store.create(TaskPlanner().plan("เขียนรายงาน"))
        store.startStep(task.id, 0)
        val failed = store.failStep(task.id, 0, "ข้อมูลไม่ครบ")
        assertEquals(TaskStepStatus.FAILED, failed.steps[0].status)
        assertEquals("ข้อมูลไม่ครบ", failed.steps[0].failureReason)

        val reopened = TaskExecutionStore(directory)
        assertEquals("ข้อมูลไม่ครบ", reopened.get(task.id)?.steps?.get(0)?.failureReason)
        val retry = reopened.retryStep(task.id, 0)
        assertEquals(TaskStepStatus.PENDING, retry.steps[0].status)
        assertEquals(null, retry.steps[0].failureReason)
    }

    @Test fun cannotFailOrCompleteStepThatIsNotInProgress() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("invalid-transition"))
        val task = store.create(TaskPlanner().plan("งานทั่วไป"))
        assertThrows(IllegalArgumentException::class.java) { store.failStep(task.id, 0, "error") }
        assertThrows(IllegalArgumentException::class.java) { store.completeStep(task.id, 0) }
    }

    @Test fun canCancelPendingOrActiveStepButNotCompletedStep() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("cancel"))
        val task = store.create(TaskPlanner().plan("งานทั่วไป"))
        assertEquals(TaskStepStatus.CANCELLED, store.cancelStep(task.id, 1).steps[1].status)
        store.startStep(task.id, 0)
        store.completeStep(task.id, 0)
        assertThrows(IllegalArgumentException::class.java) { store.cancelStep(task.id, 0) }
    }

    @Test fun rejectsAmbiguousTaskIdPrefix() {
        val store = TaskExecutionStore(temporaryFolder.newFolder("ambiguous"))
        val first = store.create(TaskPlanner().plan("งานแรก"))
        val second = store.create(TaskPlanner().plan("งานสอง"))
        // A deliberately shared prefix must never select an arbitrary task.
        val shared = "00000000-0000-0000-0000-"
        assertTrue(first.id != second.id)
        assertThrows(IllegalArgumentException::class.java) { store.get(shared) }
    }

}
