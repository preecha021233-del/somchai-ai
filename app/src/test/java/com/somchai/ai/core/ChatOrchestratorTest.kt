package com.somchai.ai.core

import com.somchai.ai.core.brain.Brain
import com.somchai.ai.core.brain.BrainReply
import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.memory.LocalMemoryStore
import com.somchai.ai.core.orchestrator.ChatOrchestrator
import com.somchai.ai.core.processing.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChatOrchestratorTest {
    @get:Rule val temp = TemporaryFolder()
    private val fallback = object : Brain {
        override suspend fun respond(request: BrainRequest) = BrainReply("fallback", "offline-fallback", true)
    }
    private fun make(
        brain: Brain = fallback,
        approved: () -> Boolean = { false },
        search: suspend (String) -> String = { "found:" + it },
        configured: () -> Boolean = { false },
        remote: suspend (List<Pair<String, String>>, String) -> String = { _, _ -> "remote" },
        retrieveMemory: (String) -> List<String> = { emptyList() }
    ): Pair<ChatOrchestrator, TaskExecutionStore> {
        val tasks = TaskExecutionStore(temp.newFolder("tasks" + System.nanoTime()))
        val planner = TaskPlanner()
        val memory = LocalMemoryStore(temp.newFolder("memory" + System.nanoTime()))
        return ChatOrchestrator(
            brain, InternalProcessor(memory, planner, tasks = tasks), approved, search,
            configured, remote, tasks, planner, retrieveMemory = retrieveMemory
        ) to tasks
    }
    @Test fun greetingUsesInternalAndDoesNotCallRemote() = runBlocking {
        var calls = 0
        val (orchestrator) = make(remote = { _, _ -> calls++; "remote" })
        assertEquals("greeting", orchestrator.handle("สวัสดี").intent)
        assertEquals(0, calls)
    }
    @Test fun unsupportedUsesLocalBrainNotRemote() = runBlocking {
        var calls = 0
        val local = object : Brain {
            override suspend fun respond(request: BrainRequest) = BrainReply("local answer", "local-test", false)
        }
        val (orchestrator) = make(brain = local, remote = { _, _ -> calls++; "remote" })
        assertEquals("brain.local", orchestrator.handle("อธิบายสิ่งที่ไม่อยู่ในกฎ").intent)
        assertEquals(0, calls)
    }
    @Test fun modelTestRouteBypassesRulesAndInvokesOnlyLocalBrain() = runBlocking {
        var calls = 0
        var captured: BrainRequest? = null
        val local = object : Brain {
            override suspend fun respond(request: BrainRequest): BrainReply {
                calls++
                captured = request
                return BrainReply("ตอบจากโมเดล", "llama.cpp-android", false)
            }
        }
        val (orchestrator) = make(brain = local)
        val result = orchestrator.handle("/model-test สวัสดี")
        assertEquals(1, calls)
        assertEquals("สวัสดี", captured?.prompt)
        assertTrue(captured?.history.isNullOrEmpty())
        assertTrue(captured?.memoryContext.isNullOrEmpty())
        assertEquals("model_test.passed", result.intent)
        assertEquals("llama.cpp-android", result.engineId)
        assertFalse(result.usedFallback)
    }

    @Test fun modelTestRouteReportsFallbackAsFailure() = runBlocking {
        val (orchestrator) = make()
        val result = orchestrator.handle("/model-test ช่วยคิดคำตอบ")
        assertEquals("model_test.failed", result.intent)
        assertTrue(result.usedFallback)
        assertTrue(result.answer.contains("ทดสอบโมเดลภายในไม่ผ่าน"))
    }

    @Test fun relevantMemoriesAreAttachedToLocalBrainRequests() = runBlocking {
        var captured: BrainRequest? = null
        val local = object : Brain {
            override suspend fun respond(request: BrainRequest): BrainReply {
                captured = request
                return BrainReply("local answer", "local-test", false)
            }
        }
        val selected = listOf("[memory-1] โครงการ SOMCHAI พัฒนา AI ภายใน")
        val (orchestrator) = make(
            brain = local,
            retrieveMemory = { query ->
                assertTrue(query.contains("SOMCHAI"))
                selected
            }
        )

        val result = orchestrator.handle("ช่วยอธิบายประเด็น SOMCHAI ที่ไม่อยู่ในกฎ")
        assertEquals("brain.local", result.intent)
        assertEquals(selected, captured?.memoryContext)
    }

    @Test fun explicitRemoteAiRouteNeverRequestsOrAttachesPrivateMemory() = runBlocking {
        var retrievalCalls = 0
        var remoteCalls = 0
        val (orchestrator) = make(
            configured = { true },
            remote = { _, _ -> remoteCalls++; "remote answer" },
            retrieveMemory = { retrievalCalls++; listOf("private memory") }
        )

        val result = orchestrator.handle("/ai ช่วยสรุปคำถามนี้")
        assertEquals("llm.explicit", result.intent)
        assertEquals(1, remoteCalls)
        assertEquals(0, retrievalCalls)
    }

    @Test fun searchRequiresApproval() = runBlocking {
        var calls = 0
        val (orchestrator) = make(search = { calls++; "found" })
        assertEquals("research.plugin_required", orchestrator.handle("/search test").intent)
        assertEquals(0, calls)
    }
    @Test fun approvedSearchCallsResearchTool() = runBlocking {
        val (orchestrator) = make(approved = { true }, search = { "read:" + it })
        assertEquals("read:kotlin", orchestrator.handle("/search kotlin").answer)
    }
    @Test fun aiRequiresConfiguredProvider() = runBlocking {
        var calls = 0
        val (orchestrator) = make(remote = { _, _ -> calls++; "remote" })
        assertEquals("llm.not_configured", orchestrator.handle("/ai hello").intent)
        assertEquals(0, calls)
    }
    @Test fun taskCreatesRecordAndStartsFirstStep() = runBlocking {
        val (orchestrator, tasks) = make()
        val result = orchestrator.handle("/task พัฒนาแอป SOMCHAI")
        assertEquals("task.create", result.intent)
        assertEquals(TaskStepStatus.IN_PROGRESS, tasks.all().single().steps.first().status)
        assertTrue(result.answer.contains("ยังไม่ได้ดำเนินการภายนอก"))
    }

    @Test fun taskStartPersistsFirstStepAndAuditWithoutClaimingExternalExecution() = runBlocking {
        val directory = temp.newFolder("task-p16")
        val events = mutableListOf<Pair<String, String>>()
        val tasks = TaskExecutionStore(directory) { event, detail -> events += event to detail }
        val planner = TaskPlanner()
        val memory = LocalMemoryStore(temp.newFolder("memory-p16"))
        val orchestrator = ChatOrchestrator(
            fallback,
            InternalProcessor(memory, planner, tasks = tasks),
            { false },
            { "unused" },
            { false },
            { _, _ -> "unused" },
            tasks,
            planner,
            audit = { event, detail -> events += event to detail }
        )

        val result = orchestrator.handle("/task พัฒนาแอป Android")
        val record = tasks.all().single()
        assertEquals("task.create", result.intent)
        assertEquals(TaskType.DEVELOPMENT, record.type)
        assertEquals(TaskStepStatus.IN_PROGRESS, record.steps.first().status)
        assertTrue(record.steps.drop(1).all { it.status == TaskStepStatus.PENDING })
        assertTrue(events.any { it.first == "TASK_CREATED" && it.second.contains("id=${record.id}") })
        assertTrue(events.any { it.first == "TASK_STEP_STARTED" && it.second.contains("id=${record.id}") })
        assertTrue(events.any { it.first == "CHAT_TASK_CREATED" && it.second.contains("id=${record.id}") })
        assertTrue(result.answer.contains("ยังไม่ได้ดำเนินการภายนอก"))
        assertFalse(result.answer.contains("ทำงานเสร็จแล้ว"))

        val reopened = TaskExecutionStore(directory)
        assertEquals(record, reopened.get(record.id))
        assertEquals(TaskStepStatus.IN_PROGRESS, reopened.get(record.id)?.steps?.first()?.status)
    }
}
