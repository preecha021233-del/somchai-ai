package com.somchai.ai.core.llm

import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import java.net.InetAddress
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

    @Test fun providerDnsUsesOneValidatedAddressSetAndRejectsRebindingAnswers() {
        var calls = 0
        val publicIp = InetAddress.getByName("93.184.216.34")
        val resolved = resolvePublicProviderAddresses("provider.example") {
            calls++
            if (calls == 1) arrayOf(publicIp) else arrayOf(InetAddress.getByName("127.0.0.1"))
        }
        assertEquals(listOf(publicIp), resolved)
        assertEquals(1, calls)
        assertThrows(IllegalArgumentException::class.java) {
            resolvePublicProviderAddresses("provider.example") {
                calls++
                arrayOf(InetAddress.getByName("127.0.0.1"))
            }
        }
        assertEquals(2, calls)
    }

    @Test fun rejectsReservedIpv6ProviderDnsAnswers() {
        listOf("2001:2::1", "2002::1", "3fff::1").forEach { reserved ->
            assertThrows(IllegalArgumentException::class.java) {
                resolvePublicProviderAddresses("provider.example") {
                    arrayOf(InetAddress.getByName(reserved))
                }
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
