package com.somchai.ai.extensions

import com.somchai.ai.core.CorePaths
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.security.PermissionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BuiltinToolExecutorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private class FakeDiscovery(
        private val result: ResearchDiscoveryResult,
        private val onDiscover: () -> Unit = {}
    ) : ResearchDiscoveryProvider {
        override fun discover(query: String): ResearchDiscoveryResult {
            onDiscover()
            return result
        }
    }

    private class FakeReader : ResearchPageReader {
        val readUrls = mutableListOf<String>()
        override fun read(address: String): WebPageContent {
            readUrls += address
            return WebPageContent(address, "Title $address", "Readable evidence for $address")
        }
    }

    private fun executor(
        approved: Boolean,
        discovery: ResearchDiscoveryProvider,
        onBudgetCheck: () -> Unit = {}
    ): BuiltinToolExecutor {
        val paths = CorePaths.forTesting(temporaryFolder.newFolder())
        paths.ensureDirectories()
        return BuiltinToolExecutor(
            permissions = PermissionManager(),
            logger = ActivityLogger(paths),
            checkStorageBudget = onBudgetCheck,
            discoveryProvider = discovery,
            isWebSearchApproved = { approved }
        )
    }

    @Test fun unapprovedSearchIsBlockedBeforeDiscoveryOrNetworkWork() = runBlocking {
        var discovered = false
        var budgetChecked = false
        val candidates = ResearchDiscoveryResult(
            listOf(ResearchCandidate("test", "Page", "https://example.org/page")),
            emptyList()
        )
        val tool = executor(
            approved = false,
            discovery = FakeDiscovery(candidates) { discovered = true },
            onBudgetCheck = { budgetChecked = true }
        )
        assertThrows(IllegalStateException::class.java) {
            runBlocking { tool.search("test query") }
        }
        assertFalse(discovered)
        assertFalse(budgetChecked)
    }

    @Test fun approvedSearchReadsAndReportsActualPages() = runBlocking {
        val paths = CorePaths.forTesting(temporaryFolder.newFolder("approved"))
        paths.ensureDirectories()
        val candidates = listOf(
            ResearchCandidate("source-a", "Source A", "https://a.example/article"),
            ResearchCandidate("source-b", "Source B", "https://b.example/article")
        )
        val reader = FakeReader()
        val tool = BuiltinToolExecutor(
            permissions = PermissionManager(),
            logger = ActivityLogger(paths),
            checkStorageBudget = {},
            pageReader = reader,
            discoveryProvider = FakeDiscovery(ResearchDiscoveryResult(candidates, emptyList())),
            isWebSearchApproved = { true }
        )
        val result = tool.search("test query")
        assertEquals(2, reader.readUrls.size)
        assertTrue(result.contains("อ่านแหล่งข้อมูลจริงอย่างน้อย 2 หน้าแล้ว"))
        assertTrue(result.contains("[S1] Source A"))
        assertTrue(result.contains("[S2] Source B"))
        assertTrue(result.contains("https://a.example/article"))
        assertTrue(result.contains("https://b.example/article"))
    }

    @Test fun invalidQueryIsRejectedBeforeDiscovery() = runBlocking {
        var discovered = false
        val paths = CorePaths.forTesting(temporaryFolder.newFolder("invalid"))
        paths.ensureDirectories()
        val tool = BuiltinToolExecutor(
            PermissionManager(), ActivityLogger(paths), {},
            discoveryProvider = FakeDiscovery(ResearchDiscoveryResult(emptyList(), emptyList())) {
                discovered = true
            },
            isWebSearchApproved = { true }
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { tool.search(" ") }
        }
        assertFalse(discovered)
    }
}
