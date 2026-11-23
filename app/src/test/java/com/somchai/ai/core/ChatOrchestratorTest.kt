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
        remote: suspend (List<Pair<String, String>>, String) -> String = { _, _ -> "remote" }
    ): Pair<ChatOrchestrator, TaskExecutionStore> {
        val tasks = TaskExecutionStore(temp.newFolder("tasks" + System.nanoTime()))
        val planner = TaskPlanner()
        val memory = LocalMemoryStore(temp.newFolder("memory" + System.nanoTime()))
        return ChatOrchestrator(
            brain, InternalProcessor(memory, planner, tasks = tasks), approved, search,
            configured, remote, tasks, planner
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
}
