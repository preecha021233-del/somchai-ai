package com.somchai.ai.extensions

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class WebPageContent(val url: String, val title: String, val text: String)

/**
 * Read-only HTTPS reader. DNS is resolved once per hop and the TCP socket is
 * connected to that validated address; the URL hostname is retained for SNI,
 * Host and the platform's normal HTTPS hostname/certificate verification.
 */
class WebPageReader(
    private val resolver: (String) -> Array<InetAddress> = InetAddress::getAllByName
) : ResearchPageReader {
    override fun read(address: String): WebPageContent {
        var current = validateUrl(address)
        for (redirectCount in 0..MAX_REDIRECTS) {
            val target = URL(current)
            val host = target.host.trim('[', ']').trimEnd('.')
            val pinnedAddresses = resolvePublicAddresses(host)
            val pinnedIp = pinnedAddresses.first()
            val connection = (target.openConnection(Proxy.NO_PROXY) as HttpsURLConnection).apply {
                // The custom factory connects the TCP socket to pinnedIp, never to a newly
                // resolved hostname. HttpsURLConnection still verifies the URL hostname.
                sslSocketFactory = PinnedIpSslSocketFactory(
                    pinnedIp = pinnedIp,
                    tlsHost = host,
                    connectTimeoutMs = CONNECT_TIMEOUT_MS,
                    delegate = HttpsURLConnection.getDefaultSSLSocketFactory()
                )
                hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Host", host)
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9")
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    check(redirectCount < MAX_REDIRECTS) { "หน้าเว็บเปลี่ยนเส้นทางเกินจำนวนที่อนุญาต" }
                    val location = connection.getHeaderField("Location")
                        ?: error("Redirect ไม่มี Location")
                    // The next loop validates, resolves and pins the new destination independently.
                    current = validateUrl(URL(target, location).toString())
                    continue
                }
                check(status in 200..299) { "หน้าเว็บตอบกลับ HTTP $status" }
                val contentType = connection.contentType.orEmpty().lowercase()
                require(contentType.contains("text/html") ||
                    contentType.contains("application/xhtml+xml") ||
                    contentType.contains("text/plain")) {
                    "ชนิดเนื้อหาไม่ใช่ HTML หรือข้อความที่อ่านได้"
                }
                val bytes = connection.inputStream.use { readBounded(it) }
                val charsetName = Regex("charset=([^; ]+)", RegexOption.IGNORE_CASE)
                    .find(contentType)?.groupValues?.getOrNull(1)?.trim('"', '\'')
                val charset = charsetName?.let { runCatching { Charset.forName(it) }.getOrNull() }
                    ?: StandardCharsets.UTF_8
                val source = String(bytes, charset)
                val title = Regex("(?is)<title[^>]*>(.*?)</title>")
                    .find(source)?.groupValues?.getOrNull(1)
                    ?.let { HtmlTextExtractor.extract(it, 180) }.orEmpty().ifBlank { host }
                val text = HtmlTextExtractor.extract(source)
                check(text.isNotBlank()) { "ไม่พบเนื้อหาข้อความที่อ่านได้ในหน้าเว็บ" }
                return WebPageContent(current, title, text)
            } finally {
                connection.disconnect()
            }
        }
        error("หน้าเว็บเปลี่ยนเส้นทางเกินจำนวนที่อนุญาต")
    }

    internal fun validateUrl(address: String): String {
        val uri = URI(address).normalize()
        require(uri.scheme.equals("https", ignoreCase = true)) { "อนุญาตเฉพาะ URL แบบ HTTPS" }
        require(uri.userInfo == null) { "ไม่อนุญาต URL ที่ฝังข้อมูลบัญชีผู้ใช้" }
        require(uri.port == -1 || uri.port == 443) { "ไม่อนุญาตพอร์ตที่ไม่ใช่ HTTPS มาตรฐาน" }
        val host = uri.host?.lowercase()?.trim('[', ']')?.trimEnd('.')
            ?: throw IllegalArgumentException("URL ไม่มีชื่อโฮสต์ที่ถูกต้อง")
        require(host != "localhost" && host.endsWith(".localhost").not() &&
            host != "local" && !host.endsWith(".local") &&
            host != "internal" && !host.endsWith(".internal") &&
            host != "lan" && !host.endsWith(".lan")) {
            "ไม่อนุญาตที่อยู่เครือข่ายภายใน"
        }
        return uri.toASCIIString()
    }

    /** One resolver call per request hop. Reject the whole answer if any address is unsafe. */
    internal fun resolvePublicAddresses(host: String): List<InetAddress> {
        val addresses = resolver(host).distinctBy { it.hostAddress }
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) {
            "ปลายทาง DNS ชี้ไปยังเครือข่ายภายในหรือ IP ที่ไม่ใช่สาธารณะ"
        }
        return addresses
    }

    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 0xff }
        if (bytes.size == 4) {
            val a = bytes[0]; val b = bytes[1]; val c = bytes[2]
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false
            if (a == 169 && b == 254) return false // includes 169.254.169.254 cloud metadata
            if (a == 172 && b in 16..31) return false
            if (a == 192 && b == 168) return false
            if (a == 100 && b in 64..127) return false
            if (a == 192 && b == 0 && c in listOf(0, 2)) return false
            if (a == 198 && b in 18..19) return false
            if (a == 198 && b == 51 && c == 100) return false
            if (a == 203 && b == 0 && c == 113) return false
            if (a == 255) return false
            return true
        }
        if (bytes.size == 16) {
            val globalUnicast = bytes[0] in 0x20..0x3f
            val documentation = bytes[0] == 0x20 && bytes[1] == 0x01 &&
                bytes[2] == 0x0d && bytes[3] == 0xb8
            val ietfProtocolAssignments = bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] in 0x00..0x01
            val sixToFour = bytes[0] == 0x20 && bytes[1] == 0x02
            val documentation3fff = bytes[0] == 0x3f && (bytes[1] and 0xf0) == 0xf0
            return globalUnicast && !documentation && !ietfProtocolAssignments &&
                !sixToFour && !documentation3fff
        }
        return false
    }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_RESPONSE_BYTES) { "หน้าเว็บมีขนาดเกินขีดจำกัด" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val MAX_REDIRECTS = 3
        private const val MAX_RESPONSE_BYTES = 1_000_000
        private const val USER_AGENT = "SOMCHAI-AI/1.8 (read-only research)"
    }
}

/**
 * HttpsURLConnection asks this factory to open a socket for the URL host.
 * We deliberately ignore that hostname for TCP routing and connect only to
 * the previously validated pinnedIp. The layered TLS socket keeps tlsHost for SNI.
 */
internal class PinnedIpSslSocketFactory(
    private val pinnedIp: InetAddress,
    internal val tlsHost: String,
    private val connectTimeoutMs: Int,
    private val delegate: SSLSocketFactory,
    private val socketConnector: (Socket, InetSocketAddress, Int) -> Unit = { socket, destination, timeout ->
        socket.connect(destination, timeout)
    }
) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(host: String, port: Int): Socket = connectPinned(port)
    override fun createSocket(host: String, port: Int, localHost: java.net.InetAddress, localPort: Int): Socket =
        connectPinned(port, localHost, localPort)
    override fun createSocket(host: java.net.InetAddress, port: Int): Socket = connectPinned(port)
    override fun createSocket(address: java.net.InetAddress, port: Int, localAddress: java.net.InetAddress, localPort: Int): Socket =
        connectPinned(port, localAddress, localPort)

    internal fun connectRawSocket(raw: Socket, port: Int) {
        require(port == 443) { "อนุญาตเฉพาะ HTTPS port 443" }
        socketConnector(raw, InetSocketAddress(pinnedIp, port), connectTimeoutMs)
    }

    private fun connectPinned(port: Int, localAddress: InetAddress? = null, localPort: Int = 0): Socket {
        require(port == 443) { "อนุญาตเฉพาะ HTTPS port 443" }
        val raw = Socket()
        try {
            if (localAddress != null) raw.bind(InetSocketAddress(localAddress, localPort))
            // This is the only TCP destination: no hostname lookup occurs here.
            connectRawSocket(raw, port)
            val tls = delegate.createSocket(raw, tlsHost, port, true) as SSLSocket
            val parameters = tls.sslParameters
            val isIpv4Literal = tlsHost.count { it == '.' } == 3 && tlsHost.all { it.isDigit() || it == '.' }
            if (!tlsHost.contains(':') && !isIpv4Literal) {
                parameters.serverNames = listOf(SNIHostName(tlsHost))
            }
            parameters.endpointIdentificationAlgorithm = "HTTPS"
            tls.sslParameters = parameters
            return tls
        } catch (failure: Throwable) {
            runCatching { raw.close() }
            throw failure
        }
    }
}

internal object HtmlTextExtractor {
    fun extract(html: String, maxChars: Int = 8_000): String {
        val visible = html
            .replace(Regex("(?is)<!--.*?-->"), " ")
            .replace(Regex("(?is)<(script|style|noscript|svg|iframe|nav|footer|header|aside|form)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<(?:br|/p|/div|/li|/h[1-6]|/article|/section)[^>]*>"), "\\n")
            .replace(Regex("(?s)<[^>]+>"), " ")
            .replace("&nbsp;", " ").replace("&quot;", "\\\"").replace("&#39;", "'")
            .replace("&apos;", "'").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("&#(x[0-9a-f]+|[0-9]+);", RegexOption.IGNORE_CASE)) { match ->
                runCatching {
                    val raw = match.groupValues[1]
                    val codePoint = if (raw.startsWith("x", true)) raw.substring(1).toInt(16) else raw.toInt()
                    String(Character.toChars(codePoint))
                }.getOrDefault(" ")
            }
            .replace('\\u0000', ' ')
        return visible.lines().map { it.replace(Regex("[\\\\t\\\\x0B\\\\f\\\\r ]+"), " ").trim() }
            .filter { it.isNotEmpty() }.joinToString("\\n").take(maxChars)
    }
}
