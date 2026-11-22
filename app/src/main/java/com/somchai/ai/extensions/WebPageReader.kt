package com.somchai.ai.extensions

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class WebPageContent(val url: String, val title: String, val text: String)

/** Read-only HTTPS page reader with redirect, size, timeout, and private-network guards. */
class WebPageReader : ResearchPageReader {
    override fun read(address: String): WebPageContent {
        var current = validateUrl(address)
        repeat(MAX_REDIRECTS + 1) {
            val target = URL(current)
            assertPublicHost(target.host)
            val connection = target.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9")
                connection.setRequestProperty("Accept-Encoding", "identity")
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: error("Redirect ไม่มี Location")
                    current = validateUrl(URL(target, location).toString())
                    return@repeat
                }
                check(status in 200..299) { "หน้าเว็บตอบกลับ HTTP $status" }
                val contentType = connection.contentType.orEmpty().lowercase()
                require(contentType.contains("text/html") ||
                    contentType.contains("application/xhtml+xml") ||
                    contentType.contains("text/plain")) { "ชนิดเนื้อหาไม่ใช่ HTML หรือข้อความที่อ่านได้" }
                val bytes = connection.inputStream.use { readBounded(it) }
                val charsetName = Regex("charset=([^; ]+)", RegexOption.IGNORE_CASE)
                    .find(contentType)?.groupValues?.getOrNull(1)?.trim('"', '\'')
                val charset = charsetName?.let { runCatching { Charset.forName(it) }.getOrNull() }
                    ?: StandardCharsets.UTF_8
                val source = String(bytes, charset)
                val title = Regex("(?is)<title[^>]*>(.*?)</title>")
                    .find(source)?.groupValues?.getOrNull(1)
                    ?.let { HtmlTextExtractor.extract(it, 180) }.orEmpty().ifBlank { target.host }
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
        val host = uri.host?.lowercase()?.trimEnd('.')
            ?: throw IllegalArgumentException("URL ไม่มีชื่อโฮสต์ที่ถูกต้อง")
        require(host != "localhost" && !host.endsWith(".localhost") &&
            !host.endsWith(".local") && !host.endsWith(".internal") && !host.endsWith(".lan")) {
            "ไม่อนุญาตที่อยู่เครือข่ายภายใน"
        }
        val ipLiteral = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) || host.contains(":")
        if (ipLiteral) {
            val literal = host.removePrefix("[").removeSuffix("]")
            require(isPublicAddress(InetAddress.getByName(literal))) { "ไม่อนุญาต IP ภายในหรือ IP ที่ไม่ใช่สาธารณะ" }
        }
        return uri.toASCIIString()
    }

    private fun assertPublicHost(host: String) {
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) {
            "ปลายทาง DNS ชี้ไปยังเครือข่ายภายในหรือ IP ที่ไม่ใช่สาธารณะ"
        }
    }

    private fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 0xff }
        if (bytes.size == 4) {
            val a = bytes[0]; val b = bytes[1]; val c = bytes[2]
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false
            if (a == 169 && b == 254) return false
            if (a == 172 && b in 16..31) return false
            if (a == 192 && b == 168) return false
            if (a == 100 && b in 64..127) return false
            if (a == 192 && b == 0 && c in listOf(0, 2)) return false
            if (a == 198 && b in 18..19) return false
            if (a == 198 && b == 51 && c == 100) return false
            if (a == 203 && b == 0 && c == 113) return false
            return true
        }
        if (bytes.size == 16) {
            val globalUnicast = bytes[0] in 0x20..0x3f
            val documentation = bytes[0] == 0x20 && bytes[1] == 0x01 &&
                bytes[2] == 0x0d && bytes[3] == 0xb8
            return globalUnicast && !documentation
        }
        return false
    }

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
            .replace('\u0000', ' ')
        return visible.lines().map { it.replace(Regex("[\\t\\x0B\\f\\r ]+"), " ").trim() }
            .filter { it.isNotEmpty() }.joinToString("\n").take(maxChars)
    }
}
