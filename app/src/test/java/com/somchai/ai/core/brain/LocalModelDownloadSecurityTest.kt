package com.somchai.ai.core.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.URL
import java.net.InetAddress
import java.io.File

class LocalModelDownloadSecurityTest {
    @Test fun rejectsInsufficientFreeStorage() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.requireSufficientStorage(649_999_999L)
        }
        LocalModelStore.requireSufficientStorage(650_000_000L)
    }

    @Test fun rejectsTruncatedModelDownload() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadedModel(
                totalBytes = 399_999_999L,
                declaredBytes = 500_000_000L,
                actualSha256 = LocalModelStore.EXPECTED_SHA256,
                validGgufHeader = true
            )
        }
    }

    @Test fun rejectsWrongModelSha256() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadedModel(
                totalBytes = 500_000_000L,
                declaredBytes = 500_000_000L,
                actualSha256 = "0".repeat(64),
                validGgufHeader = true
            )
        }
    }

    @Test fun rejectsInvalidGgufHeaderAndMismatchedContentLength() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadedModel(
                500_000_000L, 500_000_000L, LocalModelStore.EXPECTED_SHA256, false
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalModelStore.validateDownloadedModel(
                500_000_000L, 510_000_000L, LocalModelStore.EXPECTED_SHA256, true
            )
        }
    }

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
    @Test fun modelDownloaderUsesOnlyValidatedDnsAddresses() {
        var dnsCalls = 0
        val publicIp = InetAddress.getByName("93.184.216.34")
        val store = LocalModelStore(File("build/test-models")) {
            dnsCalls++
            if (dnsCalls == 1) arrayOf(publicIp) else arrayOf(InetAddress.getByName("127.0.0.1"))
        }
        assertEquals(listOf(publicIp), store.resolvePublicAddresses("model.example"))
        assertEquals(1, dnsCalls)
        assertThrows(IllegalArgumentException::class.java) {
            store.resolvePublicAddresses("model.example")
        }
        assertEquals(2, dnsCalls)
    }

    @Test fun rejectsMixedPublicAndPrivateModelDnsAnswers() {
        val store = LocalModelStore(File("build/test-models")) {
            arrayOf(InetAddress.getByName("93.184.216.34"), InetAddress.getByName("10.0.0.1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.resolvePublicAddresses("rebind.example")
        }
    }

    @Test fun rejectsReservedAndTransitionIpv6ModelDestinations() {
        listOf(
            "https://[2001:2::1]/model.gguf",
            "https://[2002::1]/model.gguf",
            "https://[3fff::1]/model.gguf"
        ).forEach { address ->
            assertThrows("Must reject $address", IllegalArgumentException::class.java) {
                LocalModelStore.validateDownloadUrl(address)
            }
        }
        assertEquals(
            "https://[2606:4700:4700::1111]/model.gguf",
            LocalModelStore.validateDownloadUrl("https://[2606:4700:4700::1111]/model.gguf")
        )
    }

}
