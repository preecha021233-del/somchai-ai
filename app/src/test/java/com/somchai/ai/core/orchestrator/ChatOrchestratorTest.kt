package com.somchai.ai.core.orchestrator

import com.somchai.ai.core.brain.*
import com.somchai.ai.core.memory.LocalMemoryStore
import com.somchai.ai.core.processing.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChatOrchestratorTest {
    @get:Rule val temp=TemporaryFolder()
    private val fallback=object:Brain {
        override suspend fun respond(request:BrainRequest)=BrainReply("fallback","offline-fallback",true)
    }
    private fun make(brain:Brain=fallback,approved:()->Boolean={false},search:suspend(String)->String={"found:"+it},
        configured:()->Boolean={false},remote: suspend (List<Pair<String, String>>, String) -> String={"remote"}):Pair<ChatOrchestrator,TaskExecutionStore> {
        val tasks=TaskExecutionStore(temp.newFolder("tasks"+System.nanoTime()))
        val planner=TaskPlanner()
        val memory=LocalMemoryStore(temp.newFolder("memory"+System.nanoTime()))
        return ChatOrchestrator(brain,InternalProcessor(memory,planner,tasks=tasks),approved,search,configured,remote,tasks,planner) to tasks
    }
    @Test fun greetingUsesInternalAndDoesNotCallRemote()=runBlocking {
        var calls=0
        val (o)=make(remote={_,_->calls++;"remote"})
        assertEquals("greeting",o.handle("สวัสดี").intent); assertEquals(0,calls)
    }
    @Test fun unsupportedUsesLocalBrainNotRemote()=runBlocking {
        var calls=0
        val local=object:Brain { override suspend fun respond(request:BrainRequest)=BrainReply("local answer","local-test",false) }
        val (o)=make(brain=local,remote={_,_->calls++;"remote"})
        assertEquals("brain.local",o.handle("อธิบายสิ่งที่ไม่อยู่ในกฎ").intent); assertEquals(0,calls)
    }
    @Test fun searchRequiresApproval()=runBlocking {
        var calls=0
        val (o)=make(search={calls++;"found"})
        assertEquals("research.plugin_required",o.handle("/search test").intent); assertEquals(0,calls)
    }
    @Test fun approvedSearchCallsResearchTool()=runBlocking {
        val (o)=make(approved={true},search={"read:"+it})
        assertEquals("read:kotlin",o.handle("/search kotlin").answer)
    }
    @Test fun aiRequiresConfiguredProvider()=runBlocking {
        var calls=0
        val (o)=make(remote={_,_->calls++;"remote"})
        assertEquals("llm.not_configured",o.handle("/ai hello").intent); assertEquals(0,calls)
    }
    @Test fun taskCreatesRecordAndStartsFirstStep()=runBlocking {
        val (o,tasks)=make()
        val result=o.handle("/task พัฒนาแอป SOMCHAI")
        assertEquals("task.create",result.intent)
        assertEquals(TaskStepStatus.IN_PROGRESS,tasks.all().single().steps.first().status)
        assertTrue(result.answer.contains("ยังไม่ได้ดำเนินการภายนอก"))
    }
}
