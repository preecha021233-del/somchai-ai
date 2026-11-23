package com.somchai.ai.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class ResearchCoordinatorTest {
    private class FakeDiscovery(
        private val result: ResearchDiscoveryResult? = null,
        private val failure: Exception? = null
    ) : ResearchDiscoveryProvider {
        override fun discover(query: String): ResearchDiscoveryResult {
            failure?.let { throw it }
            return result ?: ResearchDiscoveryResult(emptyList(), emptyList())
        }
    }

    private class FakeReader(
        private val failures: Set<String> = emptySet()
    ) : ResearchPageReader {
        override fun read(address: String): WebPageContent {
            if (address in failures) throw SocketTimeoutException("simulated timeout")
            return WebPageContent(address, "Title $address", "Readable page text for $address")
        }
    }

    @Test fun successfulDiscoveryReadsPagesAndCountsOnlyActualReads() {
        val candidates = listOf(
            ResearchCandidate("source-a", "A", "https://a.example/page"),
            ResearchCandidate("source-b", "B", "https://b.example/page")
        )
        val report = ResearchCoordinator(
            FakeDiscovery(ResearchDiscoveryResult(candidates, emptyList())),
            FakeReader()
        ).research("somchai")
        assertEquals(2, report.pagesRead.size)
        assertTrue(report.meetsTwoSourceMinimum)
    }

    @Test fun emptyDiscoveryDoesNotClaimAnyPageWasRead() {
        val report = ResearchCoordinator(
            FakeDiscovery(ResearchDiscoveryResult(emptyList(), emptyList())),
            FakeReader()
        ).research("no result")
        assertTrue(report.discovered.isEmpty())
        assertTrue(report.pagesRead.isEmpty())
        assertFalse(report.meetsTwoSourceMinimum)
    }

    @Test fun continuesPastUnreadableCandidatesUntilSuccessfulPageLimitIsReached() {
        val candidates = (1..7).map { index ->
            ResearchCandidate("source-$index", "Page $index", "https://page$index.example/article")
        }
        val failures = candidates.take(5).map { it.url }.toSet()
        val report = ResearchCoordinator(
            FakeDiscovery(ResearchDiscoveryResult(candidates, emptyList())),
            FakeReader(failures),
            maxPages = 2
        ).research("fallback sources")

        assertEquals(listOf("https://page6.example/article", "https://page7.example/article"),
            report.pagesRead.map { it.url })
        assertTrue(report.meetsTwoSourceMinimum)
        assertEquals(5, report.pageErrors.size)
    }

    @Test fun providerTimeoutIsPropagatedForExplicitErrorHandling() {
        assertThrows(SocketTimeoutException::class.java) {
            ResearchCoordinator(
                FakeDiscovery(failure = SocketTimeoutException("simulated timeout")),
                FakeReader()
            ).research("slow query")
        }
    }

    @Test fun providerErrorsAreNotConvertedIntoSuccessfulEmptySearches() {
        assertThrows(IOException::class.java) {
            ResearchCoordinator(
                FakeDiscovery(failure = IOException("simulated provider error")),
                FakeReader()
            ).research("broken query")
        }
    }

    @Test fun unreadablePagesAreNotCountedTowardTwoSourceMinimum() {
        val candidates = listOf(
            ResearchCandidate("source-a", "A", "https://a.example/page"),
            ResearchCandidate("source-b", "B", "https://b.example/page")
        )
        val report = ResearchCoordinator(
            FakeDiscovery(ResearchDiscoveryResult(candidates, emptyList())),
            FakeReader(setOf("https://b.example/page"))
        ).research("partial query")
        assertEquals(1, report.pagesRead.size)
        assertFalse(report.meetsTwoSourceMinimum)
        assertEquals(1, report.pageErrors.size)
    }
    @Test fun fragmentAndDefaultPortVariantsDoNotCountAsDistinctSources() {
        val duplicateCandidates = listOf(
            ResearchCandidate("a", "A", "https://EXAMPLE.com:443/article#section-one"),
            ResearchCandidate("b", "B", "https://example.com/article#section-two")
        )
        val report = ResearchCoordinator(
            FakeDiscovery(ResearchDiscoveryResult(duplicateCandidates, emptyList())),
            FakeReader()
        ).research("duplicate source variants")

        assertEquals(1, report.pagesRead.size)
        assertFalse(report.meetsTwoSourceMinimum)
    }

}
