package com.somchai.ai.core.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.URL

class LocalModelDownloadSecurityTest {
    @Test fun acceptsPinnedHttpsModelHost() {
        val validated = LocalModelStore.validateDownloadUrl(LocalModelStore.MODEL_URL)
        assertEquals(LocalModelStore.MODEL_URL, validated)
    }

    @Test fun rejectsHttpDowngradeRedirect() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadUrl("http://cdn.example/model.gguf")
        }
    }

    @Test fun rejectsCredentialsAndNonStandardPorts() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadUrl("https://user:password@cdn.example/model.gguf")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadUrl("https://cdn.example:8443/model.gguf")
        }
    }

    @Test fun rejectsLocalHostnamesAndPrivateIpLiterals() {
        listOf(
            "https://localhost/model.gguf",
            "https://printer.local/model.gguf",
            "https://127.0.0.1/model.gguf",
            "https://10.0.0.8/model.gguf",
            "https://192.168.1.10/model.gguf",
            "https://[::1]/model.gguf"
        ).forEach { address ->
            assertThrows("Must reject $address", IllegalArgumentException::class.java) {
                LocalModelStore.validateDownloadUrl(address)
            }
        }
    }

    @Test fun resolvesRelativeRedirectAndRevalidatesDestination() {
        val initial = URL(LocalModelStore.MODEL_URL)
        val redirected = URL(initial, "/some-model/resolve/main/model.gguf").toString()
        assertEquals("https", URL(LocalModelStore.validateDownloadUrl(redirected)).protocol)
    }

    @Test fun rejectsRedirectToPrivateHostEvenWhenInitialUrlWasHttps() {
        val initial = URL(LocalModelStore.MODEL_URL)
        val redirected = URL(initial, "https://127.0.0.1/private/model.gguf").toString()
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadUrl(redirected)
        }
    }
}
