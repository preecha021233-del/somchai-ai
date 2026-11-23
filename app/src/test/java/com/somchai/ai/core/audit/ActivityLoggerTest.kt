package com.somchai.ai.core.audit

import com.somchai.ai.core.CorePaths
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityLoggerTest {
    @Test fun rotatesAuditLogWithinBoundAndKeepsNewestEvents() {
        val paths = CorePaths.forTesting(createTempDir())
        paths.ensureDirectories()
        val logger = ActivityLogger(paths)
        repeat(3_000) { index ->
            logger.record("TEST_EVENT", "index=$index " + "x".repeat(500))
        }

        val file = java.io.File(paths.audit, "activity.jsonl")
        assertTrue(file.length() <= 1_050_000L)
        val newest = logger.recent(1).single()
        assertTrue(newest.contains("index=2999"))
        assertTrue(logger.recent(80).size <= 80)
    }
}
