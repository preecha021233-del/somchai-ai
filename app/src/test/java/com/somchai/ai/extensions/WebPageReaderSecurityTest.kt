package com.somchai.ai.extensions

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
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
        rejected("https://localhost/")
        rejected("https://api.localhost/")
        rejected("https://printer.local/")
        rejected("https://service.internal/")
        rejected("https://router.lan/")
        rejected("https://127.0.0.1/")
        rejected("https://10.0.0.8/")
        rejected("https://192.168.1.10/")
        rejected("https://169.254.169.254/")
    }

    @Test fun permitsOnlyHttpsAndStandardPort() {
        assertTrue(reader().validateUrl("https://example.com/").startsWith("https://"))
        rejected("http://example.com/")
        rejected("https://example.com:8443/")
    }

    @Test fun rejectsUserInfo() {
        rejected("https://user:password@example.com/")
    }

    @Test fun rejectsRedirectDestinationThatIsPrivate() {
        // Redirect targets pass through the same URL validation before the next DNS/connect hop.
        rejected("https://127.0.0.1/admin")
        rejected("https://metadata.internal/latest")
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

    @Test fun resolvesOnlyOnceAndPinsTheValidatedAnswerAgainstRebinding() {
        var dnsCalls = 0
        val publicAddress = ip("93.184.216.34")
        val subject = reader {
            dnsCalls++
            if (dnsCalls == 1) arrayOf(publicAddress) else arrayOf(ip("127.0.0.1"))
        }
        val selected = subject.resolvePublicAddresses("rebind.example").first()
        var actualTcpDestination: InetSocketAddress? = null
        val factory = PinnedIpSslSocketFactory(
            pinnedIp = selected,
            tlsHost = "rebind.example",
            connectTimeoutMs = 8_000,
            delegate = SSLSocketFactory.getDefault() as SSLSocketFactory,
            socketConnector = { _, destination, _ -> actualTcpDestination = destination }
        )

        // Exercise the exact connector used by the TLS factory without opening a network socket.
        factory.connectRawSocket(Socket(), 443)

        assertEquals(1, dnsCalls)
        assertEquals(publicAddress, actualTcpDestination!!.address)
        assertEquals(443, actualTcpDestination!!.port)
        assertEquals("rebind.example", factory.tlsHost)
    }

    @Test fun rejectsUnsafeAddressFamiliesAndSpecialRanges() {
        val subject = reader()
        listOf(
            "0.0.0.1", "100.64.0.1", "172.16.0.1", "172.31.255.254",
            "192.0.2.1", "198.18.0.1", "198.51.100.1", "203.0.113.1",
            "224.0.0.1", "255.255.255.255", "::1", "fc00::1", "fe80::1",
            "2001:db8::1", "2002::1"
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
