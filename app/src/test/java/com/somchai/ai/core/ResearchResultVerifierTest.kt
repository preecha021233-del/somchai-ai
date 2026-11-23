package com.somchai.ai.core

import com.somchai.ai.core.research.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchResultVerifierTest {
    private val verifier = ResearchResultVerifier()
    @Test fun discoveryOnlyIsNotReadEvidence() {
        val r = verifier.verify("q", listOf(SourceSnippet("search", text = "snippet")), emptyList())
        assertEquals(EvidenceLevel.DISCOVERY_ONLY, r.evidenceLevel)
        assertEquals(0, r.pagesRead)
    }
    @Test fun twoDistinctPagesAreMultiSource() {
        val r = verifier.verify("q", emptyList(), listOf(
            ReadPageEvidence("a", "https://a.example/p", "Kotlin language Android"),
            ReadPageEvidence("b", "https://b.example/p", "Kotlin language tools")
        ))
        assertEquals(EvidenceLevel.READ_MULTI_SOURCE, r.evidenceLevel)
        assertEquals(2, r.pagesRead)
        assertTrue(r.comparison.contains("เปรียบเทียบคำสำคัญ"))
    }
    @Test fun duplicateUrlCannotInflateCount() {
        val r = verifier.verify("q", emptyList(), listOf(
            ReadPageEvidence("a", "https://example.org/p#1", "Kotlin language"),
            ReadPageEvidence("b", "https://EXAMPLE.org:443/p#2", "Kotlin language")
        ))
        assertEquals(EvidenceLevel.READ_SINGLE_SOURCE, r.evidenceLevel)
        assertEquals(1, r.pagesRead)
    }
    @Test fun emptyEvidenceIsNone() {
        assertEquals(EvidenceLevel.NONE, verifier.verify("q", emptyList(), emptyList()).evidenceLevel)
    }
}
