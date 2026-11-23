package com.somchai.ai.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets

class WebPageReaderTest {
    private val reader = WebPageReader()

    @Test fun decodesGzipEncodedResearchResponses() {
        val expected = "<html><title>ทดสอบ</title><p>เนื้อหาภาษาไทย</p></html>"
        val compressed = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(expected.toByteArray(StandardCharsets.UTF_8)) }
        }.toByteArray()
        val decoded = reader.decodeContentEncoding(compressed, "gzip")
        assertEquals(expected, String(decoded, StandardCharsets.UTF_8))
        assertEquals(expected, String(reader.decodeContentEncoding(expected.toByteArray(), "identity"), StandardCharsets.UTF_8))
    }

    @Test fun extractsVisiblePageTextAndDropsScriptsAndNavigation() {
        val text = HtmlTextExtractor.extract(
            """<html><head><title>ตัวอย่าง</title><script>alert('x')</script></head>
                <body><nav>เมนู</nav><h1>หัวข้อหลัก</h1><p>เนื้อหา &amp; รายละเอียด</p>
                <style>.hidden { display:none }</style></body></html>"""
        )
        assertTrue(text.contains("หัวข้อหลัก"))
        assertTrue(text.contains("เนื้อหา & รายละเอียด"))
        assertFalse(text.contains("alert"))
        assertFalse(text.contains("เมนู"))
        assertFalse(text.contains("display:none"))
    }

    @Test fun decodesNumericEntitiesAndBoundsExtractedText() {
        assertTrue(HtmlTextExtractor.extract("A &#66; &#x43;").contains("A B C"))
        assertTrue(HtmlTextExtractor.extract("x".repeat(100), 20).length <= 20)
    }

    @Test fun allowsOnlyPublicIpv4TargetsThroughWellKnownNat64() {
        assertTrue(reader.isPublicAddress(java.net.InetAddress.getByName("64:ff9b::5db8:d822")))
        assertFalse(reader.isPublicAddress(java.net.InetAddress.getByName("64:ff9b::a00:1")))
    }

    @Test fun acceptsHttpsPublicHostname() {
        assertTrue(reader.validateUrl("https://example.com/article").startsWith("https://"))
    }

    @Test fun rejectsHttpLocalhostPrivateIpAndUnsafePort() {
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("http://example.com") }
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://localhost/admin") }
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://127.0.0.1/admin") }
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://example.com:8443/page") }
    }

    @Test fun rejectsEmbeddedCredentialsAndPrivateHostSuffixes() {
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://user:pass@example.com/") }
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://router.local/") }
        assertThrows(IllegalArgumentException::class.java) { reader.validateUrl("https://service.internal/") }
    }
    @Test fun rejectsReservedAndTransitionIpv6AddressRanges() {
        listOf(
            "https://[2001:2::1]/resource",
            "https://[2002::1]/resource",
            "https://[3fff::1]/resource"
        ).forEach { address ->
            assertThrows("Must reject $address", IllegalArgumentException::class.java) {
                reader.validateUrl(address)
            }
        }
        assertTrue(reader.validateUrl("https://[2606:4700:4700::1111]/resource").startsWith("https://"))
    }

}
