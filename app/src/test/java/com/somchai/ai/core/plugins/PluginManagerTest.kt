package com.somchai.ai.core.plugins

import com.somchai.ai.core.CorePaths
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.security.PermissionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PluginManagerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private data class Fixture(
        val paths: CorePaths,
        val logger: ActivityLogger,
        val manager: PluginManager
    )

    private fun fixture(): Fixture {
        val paths = CorePaths.forTesting(temporaryFolder.newFolder("somchai"))
        paths.ensureDirectories()
        val logger = ActivityLogger(paths)
        val manager = PluginManager(paths, logger, PermissionManager())
        return Fixture(paths, logger, manager)
    }

    @Test fun pluginMustBeApprovedBeforeActivationAndCanBeDisabledAndReenabled() {
        val fixture = fixture()
        fixture.manager.ensureStarterCandidate()
        val candidate = fixture.manager.candidates().single()

        assertTrue(fixture.manager.activePlugins().isEmpty())
        assertTrue(fixture.manager.approve(candidate.manifest.id, candidate.hash))
        assertEquals(1, fixture.manager.activePlugins().size)
        assertTrue(fixture.manager.candidates().isEmpty())

        assertTrue(fixture.manager.disable(candidate.manifest.id))
        assertTrue(fixture.manager.activePlugins().isEmpty())
        assertEquals(candidate.manifest.id, fixture.manager.disabledPlugins().single().id)

        assertTrue(fixture.manager.enable(candidate.manifest.id))
        assertEquals(candidate.manifest.id, fixture.manager.activePlugins().single().id)
        assertTrue(fixture.manager.disabledPlugins().isEmpty())

        val audit = fixture.logger.recent(20).joinToString("\n")
        listOf("PLUGIN_CANDIDATE_CREATED", "PLUGIN_APPROVED", "PLUGIN_DISABLED", "PLUGIN_ENABLED")
            .forEach { event -> assertTrue("Missing audit event $event", audit.contains(event)) }
    }

    @Test fun approvalRejectsCandidateChangedAfterOwnerReview() {
        val fixture = fixture()
        fixture.manager.ensureStarterCandidate()
        val candidate = fixture.manager.candidates().single()
        File(fixture.paths.staging, "${candidate.manifest.id}.json").appendText(" ")

        assertThrows(IllegalArgumentException::class.java) {
            fixture.manager.approve(candidate.manifest.id, candidate.hash)
        }
        assertTrue(fixture.manager.activePlugins().isEmpty())
        assertFalseAuditApproval(fixture.logger)
    }

    private fun assertFalseAuditApproval(logger: ActivityLogger) {
        assertTrue(logger.recent(20).none { it.contains("PLUGIN_APPROVED") })
    }

    @Test fun rejectionRemovesOnlyAValidPluginIdentityAndRecordsIt() {
        val fixture = fixture()
        fixture.manager.ensureStarterCandidate()

        assertThrows(IllegalArgumentException::class.java) {
            fixture.manager.reject("../outside")
        }
        assertEquals(1, fixture.manager.candidates().size)

        fixture.manager.reject("web-search")
        assertTrue(fixture.manager.candidates().isEmpty())
        assertTrue(fixture.logger.recent(20).any { it.contains("PLUGIN_REJECTED") })
    }

    @Test fun ignoresAndRejectsOversizedPluginManifestsBeforeReadingThem() {
        val fixture = fixture()
        val huge = File(fixture.paths.staging, "oversized.json")
        huge.writeText(" ".repeat(16_385))

        assertTrue(fixture.manager.candidates().isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            fixture.manager.approve("oversized", "0".repeat(64))
        }
        assertTrue(fixture.manager.activePlugins().isEmpty())
    }

    @Test fun reportsPendingActiveAndDisabledManifestsInTheirCorrectLifecycleBuckets() {
        val fixture = fixture()
        fixture.manager.ensureStarterCandidate()
        val pending = fixture.manager.candidates().single()
        assertEquals("web-search", pending.manifest.id)
        assertTrue(fixture.manager.activePlugins().isEmpty())
        assertTrue(fixture.manager.disabledPlugins().isEmpty())

        assertTrue(fixture.manager.approve(pending.manifest.id, pending.hash))
        assertEquals(listOf("web-search"), fixture.manager.activePlugins().map { it.id })
        assertTrue(fixture.manager.candidates().isEmpty())

        assertTrue(fixture.manager.disable("web-search"))
        assertTrue(fixture.manager.activePlugins().isEmpty())
        assertEquals(listOf("web-search"), fixture.manager.disabledPlugins().map { it.id })
        assertTrue(fixture.manager.candidates().isEmpty())
    }
}
