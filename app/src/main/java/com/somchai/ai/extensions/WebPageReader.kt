package com.somchai.ai.extensions

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class WebPageContent(val url: String, val title: String, val text: String)

internal data class PinnedHttpResponse(val status: Int, val headers: Map<String, String>, val body: ByteArray)

/**
 * HTTPS-only reader. Each hop resolves once, validates every answer, then connects
 * the TCP socket to the selected InetAddress. No URLConnection/hostname DNS connect path exists.
 */
class WebPageReader(
    private val resolver: (String) -> Array<InetAddress> = InetAddress::getAllByName,
    private val socketConnector: (Socket, InetSocketAddress, Int) -> Unit = { socket, destination, timeout ->
        socket.connect(destination, timeout)
    }
) : ResearchPageReader {
    override fun read(address: String): WebPageContent {
        var current = validateUrl(address)
        for (redirectCount in 0..MAX_REDIRECTS) {
            val uri = URI(current)
            val host = uri.host.trim('[', ']').trimEnd('.')
            val pinnedIp = resolvePublicAddresses(host).first()
            val response = requestPinnedHttps(uri, host, pinnedIp)
            if (response.status in 300..399) {
                check(redirectCount < MAX_REDIRECTS) { "หน้าเว็บเปลี่ยนเส้นทางเกินจำนวนที่อนุญาต" }
                val location = response.headers["location"] ?: error("Redirect ไม่มี Location")
                current = validateRedirect(uri, location)
                continue
            }
            check(response.status in 200..299) { "หน้าเว็บตอบกลับ HTTP \${response.status}" }
            val contentType = response.headers["content-type"].orEmpty().lowercase()
            require(contentType.contains("text/html") || contentType.contains("application/xhtml+xml") ||
                contentType.contains("text/plain")) { "ชนิดเนื้อหาไม่ใช่ HTML หรือข้อความที่อ่านได้" }
            val charsetName = Regex("charset=([^; ]+)", RegexOption.IGNORE_CASE)
                .find(contentType)?.groupValues?.getOrNull(1)?.trim('"', '\'')
            val charset = charsetName?.let { runCatching { Charset.forName(it) }.getOrNull() }
                ?: StandardCharsets.UTF_8
            val source = String(response.body, charset)
            val title = Regex("(?is)<title[^>]*>(.*?)</title>").find(source)?.groupValues?.getOrNull(1)
                ?.let { HtmlTextExtractor.extract(it, 180) }.orEmpty().ifBlank { host }
            val text = HtmlTextExtractor.extract(source)
            check(text.isNotBlank()) { "ไม่พบเนื้อหาข้อความที่อ่านได้ในหน้าเว็บ" }
            return WebPageContent(current, title, text)
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
        require(host != "localhost" && !host.endsWith(".localhost") &&
            host != "local" && !host.endsWith(".local") &&
            host != "internal" && !host.endsWith(".internal") &&
            host != "lan" && !host.endsWith(".lan")) { "ไม่อนุญาตที่อยู่เครือข่ายภายใน" }
        if (isIpLiteral(host)) {
            require(isPublicAddress(InetAddress.getByName(host))) {
                "ไม่อนุญาต IP ภายในหรือ IP ที่ไม่ใช่สาธารณะ"
            }
        }
        return uri.toASCIIString()
    }

    internal fun validateRedirect(base: URI, location: String): String =
        validateUrl(base.resolve(location).toString())

    /** Exactly one DNS resolver call per hop. Reject the whole set if any answer is unsafe. */
    internal fun resolvePublicAddresses(host: String): List<InetAddress> {
        val addresses = resolver(host).distinctBy { it.hostAddress }
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) {
            "ปลายทาง DNS ชี้ไปยังเครือข่ายภายในหรือ IP ที่ไม่ใช่สาธารณะ"
        }
        return addresses
    }

    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 0xff }
        if (bytes.size == 4) {
            val a = bytes[0]; val b = bytes[1]; val c = bytes[2]
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false
            if (a == 169 && b == 254) return false
            if (a == 172 && b in 16..31) return false
            if (a == 192 && b == 168) return false
            if (a == 100 && b in 64..127) return false
            if (a == 192 && b == 0 && c in listOf(0, 2)) return false
            if (a == 192 && b == 88 && c == 99) return false
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
            val protocolAssignments = bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] in 0x00..0x01
            val sixToFour = bytes[0] == 0x20 && bytes[1] == 0x02
            val documentation3fff = bytes[0] == 0x3f && (bytes[1] and 0xf0) == 0xf0
            return globalUnicast && !documentation && !protocolAssignments &&
                !sixToFour && !documentation3fff
        }
        return false
    }

    private fun requestPinnedHttps(uri: URI, host: String, pinnedIp: InetAddress): PinnedHttpResponse {
        val raw = Socket()
        var tls: SSLSocket? = null
        try {
            // IP checked above is the actual TCP destination; hostname is never used for socket connect.
            connectPinnedSocket(raw, pinnedIp, 443, CONNECT_TIMEOUT_MS, socketConnector)
            raw.soTimeout = READ_TIMEOUT_MS
            tls = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, host, 443, true) as SSLSocket
            tls.sslParameters = configureTlsParameters(tls.sslParameters, host)
            tls.startHandshake()

            val path = (uri.rawPath.takeIf { it.isNotEmpty() } ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
            val hostHeader = uri.rawAuthority ?: host
            val output = BufferedOutputStream(tls.outputStream)
            output.write(("GET $path HTTP/1.1\r\n" +
                "Host: $hostHeader\r\n" +
                "User-Agent: $USER_AGENT\r\n" +
                "Accept: text/html,application/xhtml+xml,text/plain;q=0.9\r\n" +
                "Accept-Encoding: identity\r\n" +
                "Connection: close\r\n\r\n").toByteArray(StandardCharsets.US_ASCII))
            output.flush()

            val input = BufferedInputStream(tls.inputStream)
            val statusLine = readAsciiLine(input, MAX_HEADER_BYTES)
            val status = Regex("^HTTP/1\\.[01] ([0-9]{3})(?: .*)?$").matchEntire(statusLine)
                ?.groupValues?.get(1)?.toInt() ?: error("รูปแบบ HTTP status ไม่ถูกต้อง")
            val headers = linkedMapOf<String, String>()
            var headerBytes = statusLine.length
            while (true) {
                val line = readAsciiLine(input, MAX_HEADER_BYTES - headerBytes)
                headerBytes += line.length + 2
                require(headerBytes <= MAX_HEADER_BYTES) { "HTTP headers มีขนาดเกินกำหนด" }
                if (line.isEmpty()) break
                val separator = line.indexOf(':')
                require(separator > 0) { "รูปแบบ HTTP header ไม่ถูกต้อง" }
                val name = line.substring(0, separator).trim().lowercase()
                val value = line.substring(separator + 1).trim()
                headers[name] = if (headers.containsKey(name)) headers[name] + ", " + value else value
            }
            val body = when {
                status in 300..399 || status == 204 || status == 304 -> ByteArray(0)
                headers["transfer-encoding"]?.lowercase()?.contains("chunked") == true ->
                    readChunkedBody(input, MAX_RESPONSE_BYTES)
                headers["content-length"] != null -> {
                    val length = headers.getValue("content-length").toLongOrNull() ?: error("Content-Length ไม่ถูกต้อง")
                    require(length in 0..MAX_RESPONSE_BYTES.toLong()) { "หน้าเว็บมีขนาดเกินขีดจำกัด" }
                    readExactly(input, length.toInt())
                }
                else -> readUntilEof(input, MAX_RESPONSE_BYTES)
            }
            return PinnedHttpResponse(status, headers, body)
        } finally {
            runCatching { tls?.close() }
            runCatching { raw.close() }
        }
    }

    private fun isIpLiteral(host: String) = host.contains(':') ||
        host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))

    private fun readAsciiLine(input: InputStream, maxBytes: Int): String {
        require(maxBytes > 0) { "HTTP line มีขนาดเกินกำหนด" }
        val out = ByteArrayOutputStream()
        var previous = -1
        while (out.size() <= maxBytes) {
            val next = input.read()
            if (next < 0) error("HTTP response จบก่อนกำหนด")
            if (previous == 13 && next == 10) {
                val bytes = out.toByteArray()
                return String(bytes, 0, (bytes.size - 1).coerceAtLeast(0), StandardCharsets.US_ASCII)
            }
            out.write(next)
            previous = next
        }
        error("HTTP line มีขนาดเกินกำหนด")
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val out = ByteArrayOutputStream(length)
        val buffer = ByteArray(8192)
        var remaining = length
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (count < 0) error("HTTP body สั้นกว่า Content-Length")
            out.write(buffer, 0, count)
            remaining -= count
        }
        return out.toByteArray()
    }

    private fun readUntilEof(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(out.size() + count <= maxBytes) { "หน้าเว็บมีขนาดเกินขีดจำกัด" }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private fun readChunkedBody(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val size = readAsciiLine(input, 128).substringBefore(';').trim().toLongOrNull(16)
                ?: error("Chunk size ไม่ถูกต้อง")
            if (size == 0L) {
                while (readAsciiLine(input, MAX_HEADER_BYTES).isNotEmpty()) { /* consume trailers */ }
                break
            }
            require(size <= maxBytes.toLong() && out.size() + size <= maxBytes) { "หน้าเว็บมีขนาดเกินขีดจำกัด" }
            out.write(readExactly(input, size.toInt()))
            require(readAsciiLine(input, 2).isEmpty()) { "Chunk framing ไม่ถูกต้อง" }
        }
        return out.toByteArray()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val MAX_REDIRECTS = 3
        private const val MAX_RESPONSE_BYTES = 1_000_000
        private const val MAX_HEADER_BYTES = 32_768
        private const val USER_AGENT = "SOMCHAI-AI/1.8 (read-only research)"
    }
}

internal fun configureTlsParameters(
    parameters: javax.net.ssl.SSLParameters,
    hostname: String
): javax.net.ssl.SSLParameters {
    val ipv4Literal = hostname.count { it == '.' } == 3 && hostname.all { it.isDigit() || it == '.' }
    if (!hostname.contains(':') && !ipv4Literal) parameters.serverNames = listOf(SNIHostName(hostname))
    parameters.endpointIdentificationAlgorithm = "HTTPS"
    return parameters
}

internal fun connectPinnedSocket(
    socket: Socket,
    pinnedIp: InetAddress,
    port: Int,
    timeoutMs: Int,
    connector: (Socket, InetSocketAddress, Int) -> Unit = { s, endpoint, timeout -> s.connect(endpoint, timeout) }
) {
    require(port == 443) { "อนุญาตเฉพาะ HTTPS port 443" }
    connector(socket, InetSocketAddress(pinnedIp, port), timeoutMs)
}

internal object HtmlTextExtractor {
    fun extract(html: String, maxChars: Int = 8_000): String {
        val visible = html
            .replace(Regex("(?is)<!--.*?-->"), " ")
            .replace(Regex("(?is)<(script|style|noscript|svg|iframe|nav|footer|header|aside|form)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<(?:br|/p|/div|/li|/h[1-6]|/article|/section)[^>]*>"), "\n")
            .replace(Regex("(?s)<[^>]+>"), " ")
            .replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&apos;", "'").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("&#(x[0-9a-f]+|[0-9]+);", RegexOption.IGNORE_CASE)) { match ->
                runCatching {
                    val raw = match.groupValues[1]
                    val codePoint = if (raw.startsWith("x", true)) raw.substring(1).toInt(16) else raw.toInt()
                    String(Character.toChars(codePoint))
                }.getOrDefault(" ")
            }
            .filter { it != '\u0000' }
        return visible.lines().map { line ->
            line.map { if (it.isWhitespace()) ' ' else it }.joinToString("").replace(Regex(" +"), " ").trim()
        }.filter { it.isNotEmpty() }.joinToString("\n").take(maxChars)
    }
}
