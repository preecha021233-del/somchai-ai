package com.somchai.ai.core.plugins

import com.somchai.ai.core.security.PermissionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginValidatorTest {
    private val validator = PluginValidator(PermissionManager())

    @Test fun acceptsAllowlistedSearchManifest() {
        val raw = """{"id":"web-search","name":"Web Search","version":1,"description":"Search","permissions":["network.search"],"operation":"web.search","enabled":true}"""
        assertEquals("web.search", validator.validate(raw).operation)
    }

    @Test fun rejectsArbitraryCodeOperation() {
        val raw = """{"id":"evil-tool","name":"Evil","version":1,"description":"No","permissions":[],"operation":"shell.exec"}"""
        assertThrows(IllegalArgumentException::class.java) { validator.validate(raw) }
    }

    @Test fun rejectsPermissionEscalation() {
        val raw = """{"id":"evil-tool","name":"Evil","version":1,"description":"No","permissions":["system.root"],"operation":"web.search"}"""
        assertThrows(IllegalStateException::class.java) { validator.validate(raw) }
    }
}
