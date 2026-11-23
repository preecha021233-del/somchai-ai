package com.somchai.ai.core.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
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
        assertEquals(TaskStepStatus.CANCELLED, store.cancelStep(task.id, 0).steps[0].status)
        val activeTask = store.create(TaskPlanner().plan("งานอีกชิ้น"))
        store.startStep(activeTask.id, 0)
        assertEquals(TaskStepStatus.CANCELLED, store.cancelStep(activeTask.id, 0).steps[0].status)
        val completedTask = store.create(TaskPlanner().plan("งานที่เสร็จแล้ว"))
        store.startStep(completedTask.id, 0)
        store.completeStep(completedTask.id, 0)
        assertThrows(IllegalArgumentException::class.java) { store.cancelStep(completedTask.id, 0) }
    }


    @Test fun recordsAuditEventsForEveryPersistedTaskTransition() {
        val events = mutableListOf<Pair<String, String>>()
        val store = TaskExecutionStore(temporaryFolder.newFolder("audit")) { event, detail ->
            events += event to detail
        }
        val task = store.create(TaskPlanner().plan("งานตรวจสอบ"))
        store.startStep(task.id, 0)
        store.failStep(task.id, 0, "ต้องแก้ข้อมูล")
        store.retryStep(task.id, 0)
        store.startStep(task.id, 0)
        store.completeStep(task.id, 0)

        assertEquals(
            listOf(
                "TASK_CREATED",
                "TASK_STEP_STARTED",
                "TASK_STEP_FAILED",
                "TASK_STEP_RETRY_QUEUED",
                "TASK_STEP_STARTED",
                "TASK_STEP_COMPLETED"
            ),
            events.map { it.first }
        )
        assertTrue(events.all { it.second.contains("id=${task.id}") })
        assertTrue(events.drop(1).all { it.second.contains("step=1") })
        assertTrue(events.last().second.contains("status=COMPLETED"))
        assertFalse(events.any { it.second.contains("งานตรวจสอบ") })
    }

    @Test fun auditFailureDoesNotUndoPersistedTaskTransition() {
        val directory = temporaryFolder.newFolder("audit-failure")
        val store = TaskExecutionStore(directory) { _, _ -> error("logger unavailable") }
        val task = store.create(TaskPlanner().plan("งานยังต้องบันทึกได้"))
        val started = store.startStep(task.id, 0)
        assertEquals(TaskStepStatus.IN_PROGRESS, started.steps.first().status)
        assertEquals(TaskStepStatus.IN_PROGRESS, TaskExecutionStore(directory).get(task.id)?.steps?.first()?.status)
    }

}
