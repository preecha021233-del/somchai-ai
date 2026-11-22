package com.somchai.ai.core.plugins

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginSmokeTesterTest {
    private val tester = PluginSmokeTester()

    private fun manifest(
        enabled: Boolean = true,
        operation: String = "web.search",
        permissions: List<String> = listOf("network.search")
    ) = PluginManifest(
        "web-search",
        "Web Search",
        1,
        "Search public information",
        permissions,
        operation,
        enabled
    )

    @Test fun runsAllOfflineChecksForApprovedSearchOperation() {
        val checks = tester.run(manifest())
        assertEquals(4, checks.size)
        assertEquals("no executable payload present", checks.last())
    }

    @Test fun rejectsDisabledPlugin() {
        assertThrows(IllegalArgumentException::class.java) {
            tester.run(manifest(enabled = false))
        }
    }

    @Test fun rejectsOperationWithoutSmokeProfile() {
        assertThrows(IllegalStateException::class.java) {
            tester.run(manifest(operation = "shell.exec", permissions = emptyList()))
        }
    }

    @Test fun rejectsSearchPluginWithoutNetworkPermission() {
        assertThrows(IllegalArgumentException::class.java) {
            tester.run(manifest(permissions = emptyList()))
        }
    }
}
