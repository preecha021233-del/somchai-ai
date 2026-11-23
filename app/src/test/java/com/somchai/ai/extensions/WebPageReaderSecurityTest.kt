package com.somchai.ai.extensions

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLParameters
import javax.net.ssl.SNIHostName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WebPageReaderSecurityTest {
    private fun reader(resolver: (String) -> Array<InetAddress> = { arrayOf(ip("93.184.216.34")) }) =
        WebPageReader(resolver)

    private fun ip(value: String): InetAddress = InetAddress.getByName(value)

    private fun rejected(url: String) {
        try {
            reader().validateUrl(url)
            fail("Expected URL to be rejected: $url")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun blocksLocalhostNamesAndPrivateIpv4Literals() {
        listOf(
            "https://localhost/", "https://api.localhost/", "https://printer.local/",
            "https://service.internal/", "https://router.lan/", "https://127.0.0.1/",
            "https://10.0.0.8/", "https://172.16.0.1/", "https://172.31.255.254/",
            "https://192.168.1.10/", "https://169.254.169.254/"
        ).forEach(::rejected)
    }

    @Test fun permitsOnlyHttpsAndStandardPort() {
        assertTrue(reader().validateUrl("https://example.com/").startsWith("https://"))
        rejected("http://example.com/")
        rejected("https://example.com:8443/")
    }

    @Test fun rejectsUserInfo() {
        rejected("https://user:password@example.com/")
    }

    @Test fun redirectResolutionRevalidatesTheEntireDestination() {
        val subject = reader()
        try {
            subject.validateRedirect(java.net.URI("https://public.example/path"), "https://127.0.0.1/admin")
            fail("Redirect to loopback must be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        try {
            subject.validateRedirect(java.net.URI("https://public.example/path"), "http://example.org/")
            fail("Redirect to HTTP must be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun rejectsDnsAnswerIfAnyAddressIsPrivate() {
        val subject = reader { arrayOf(ip("93.184.216.34"), ip("10.1.2.3")) }
        try {
            subject.resolvePublicAddresses("rebind.example")
            fail("Mixed public/private DNS answer must be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun resolvesOnceAndTcpConnectUsesOnlyTheValidatedPinnedIp() {
        var dnsCalls = 0
        val publicAddress = ip("93.184.216.34")
        val subject = reader {
            dnsCalls++
            if (dnsCalls == 1) arrayOf(publicAddress) else arrayOf(ip("127.0.0.1"))
        }
        val pinned = subject.resolvePublicAddresses("rebind.example").first()
        var actualDestination: InetSocketAddress? = null
        connectPinnedSocket(
            socket = Socket(),
            pinnedIp = pinned,
            port = 443,
            timeoutMs = 8_000,
            connector = { _, endpoint, _ -> actualDestination = endpoint }
        )
        assertEquals(1, dnsCalls)
        assertEquals(publicAddress, actualDestination!!.address)
        assertEquals(443, actualDestination!!.port)
    }

    @Test fun tlsUsesOriginalHostnameForSniAndHttpsEndpointVerification() {
        val parameters = configureTlsParameters(SSLParameters(), "research.example")
        assertEquals("HTTPS", parameters.endpointIdentificationAlgorithm)
        assertEquals("research.example", (parameters.serverNames.single() as SNIHostName).asciiName)
    }

    @Test fun rejectsUnsafeAddressFamiliesAndSpecialRanges() {
        val subject = reader()
        listOf(
            "0.0.0.1", "100.64.0.1", "100.127.255.254", "172.16.0.1", "172.31.255.254",
            "192.0.2.1", "192.88.99.1", "198.18.0.1", "198.51.100.1", "203.0.113.1",
            "224.0.0.1", "255.255.255.255", "::1", "fc00::1", "fe80::1",
            "2001:db8::1", "2002::1", "3fff::1"
        ).forEach { address ->
            assertFalse("Must block $address", subject.isPublicAddress(ip(address)))
        }
    }

    @Test fun acceptsPublicIpv4AndGlobalIpv6Examples() {
        val subject = reader()
        assertTrue(subject.isPublicAddress(ip("93.184.216.34")))
        assertTrue(subject.isPublicAddress(ip("2606:4700:4700::1111")))
    }
}
