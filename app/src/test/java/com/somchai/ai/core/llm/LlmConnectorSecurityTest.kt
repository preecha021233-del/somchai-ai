package com.somchai.ai.core.llm

import org.junit.Assert.assertThrows
import org.junit.Test

class LlmConnectorSecurityTest {
    @Test fun rejectsLoopbackAndPrivateProviderAddresses() {
        listOf("localhost", "127.0.0.1", "10.0.0.8", "192.168.1.20", "172.16.0.1", "169.254.1.2")
            .forEach { host ->
                assertThrows(IllegalArgumentException::class.java) {
                    requirePublicProviderHost(host)
                }
            }
    }

    @Test fun rejectsInvalidProviderHostnames() {
        listOf("", "bad host", "a".repeat(254)).forEach { host ->
            assertThrows(IllegalArgumentException::class.java) {
                requirePublicProviderHost(host)
            }
        }
    }
}
