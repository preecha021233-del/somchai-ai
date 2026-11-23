package com.somchai.ai.core.brain

import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads the official Qwen GGUF asset into app-private storage.
 * The pinned SHA-256 is checked before the model is made visible to the runtime.
 */
class LocalModelStore(private val directory: File) {
    fun modelFile(): File = File(directory, MODEL_FILE_NAME)

    suspend fun downloadDefaultModel(onProgress: (Int) -> Unit = {}): File = withContext(Dispatchers.IO) {
        check(directory.exists() || directory.mkdirs()) { "สร้างโฟลเดอร์เก็บโมเดลไม่สำเร็จ" }
        val target = modelFile()
        if (isVerifiedModel(target)) {
            onProgress(100)
            return@withContext target
        }
        target.delete()
        requireSufficientStorage(StatFs(directory.absolutePath).availableBytes)

        val partial = File(directory, "$MODEL_FILE_NAME.part")
        partial.delete()
        val connection = openVerifiedDownloadConnection()
        try {
            val status = connection.responseCode
            check(status == HttpURLConnection.HTTP_OK) { "ดาวน์โหลดโมเดลไม่สำเร็จ (HTTP $status)" }
            val declared = connection.contentLengthLong
            require(declared <= 0L || declared <= MAX_MODEL_BYTES) {
                "ขนาดไฟล์โมเดลที่เซิร์ฟเวอร์แจ้งเกินขีดจำกัด"
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_MODEL_BYTES) { "ไฟล์โมเดลมีขนาดเกินขีดจำกัด" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        val progressTotal = if (declared > 0L) declared else EXPECTED_BYTES
                        onProgress(((total * 100L) / progressTotal).toInt().coerceIn(0, 99))
                    }
                    output.fd.sync()
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            validateDownloadedModel(total, declared, actualHash, GgufFileValidator.isValid(partial))
            check(partial.renameTo(target)) { "บันทึกไฟล์โมเดลไม่สำเร็จ" }
            onProgress(100)
            target
        } catch (error: Exception) {
            partial.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }


    suspend fun hasVerifiedModel(): Boolean = withContext(Dispatchers.IO) {
        isVerifiedModel(modelFile())
    }

    fun isVerifiedModel(file: File = modelFile()): Boolean =
        GgufFileValidator.isVerifiedDefaultModel(file)

    /**
     * Follow redirects manually so every hop is checked. Automatic redirects could
     * silently downgrade HTTPS or reach a private host before the model hash is checked.
     */
    private fun openVerifiedDownloadConnection(): HttpURLConnection {
        var address = validateDownloadUrl(MODEL_URL)
        for (redirect in 0..MAX_REDIRECTS) {
            val target = URL(address)
            assertPublicHost(target.host)
            val connection = target.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 90_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "SOMCHAI-AI/1.9 local-model-installer")
            connection.setRequestProperty("Accept-Encoding", "identity")
            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                check(redirect < MAX_REDIRECTS) { "เซิร์ฟเวอร์เปลี่ยนเส้นทางเกินจำนวนที่อนุญาต" }
                require(!location.isNullOrBlank()) { "Redirect ไม่มี Location" }
                address = validateDownloadUrl(URL(target, location).toString())
                continue
            }
            if (status != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IllegalStateException("ดาวน์โหลดโมเดลไม่สำเร็จ (HTTP $status)")
            }
            return connection
        }
        error("เซิร์ฟเวอร์เปลี่ยนเส้นทางเกินจำนวนที่อนุญาต")
    }

    private fun assertPublicHost(host: String) {
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) {
            "ปลายทางดาวน์โหลดชี้ไปยังเครือข่ายภายในหรือ IP ที่ไม่ใช่สาธารณะ"
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

    companion object {
        private const val MAX_REDIRECTS = 5

        internal fun requireSufficientStorage(availableBytes: Long) {
            require(availableBytes >= MIN_FREE_BYTES) {
                "พื้นที่ว่างไม่เพียงพอ ต้องมีพื้นที่ว่างอย่างน้อยประมาณ 650 MB"
            }
        }

        internal fun validateDownloadedModel(
            totalBytes: Long,
            declaredBytes: Long,
            actualSha256: String,
            validGgufHeader: Boolean
        ) {
            require(totalBytes in MIN_MODEL_BYTES..MAX_MODEL_BYTES) {
                "ดาวน์โหลดไม่ครบหรือขนาดไฟล์โมเดลไม่ถูกต้อง"
            }
            require(declaredBytes <= 0L || declaredBytes == totalBytes) {
                "ขนาดไฟล์ที่ดาวน์โหลดไม่ตรงกับที่เซิร์ฟเวอร์แจ้ง"
            }
            require(actualSha256.equals(EXPECTED_SHA256, ignoreCase = true)) {
                "ตรวจสอบ SHA-256 ไม่ผ่าน ไฟล์โมเดลถูกปฏิเสธ"
            }
            require(validGgufHeader) { "ไฟล์ที่ดาวน์โหลดไม่ใช่โมเดล GGUF" }
        }

        internal fun validateDownloadUrl(address: String): String {
            val uri = URI(address).normalize()
            require(uri.scheme.equals("https", ignoreCase = true)) { "อนุญาตเฉพาะ URL ดาวน์โหลด HTTPS" }
            require(uri.userInfo == null) { "ไม่อนุญาต URL ดาวน์โหลดที่ฝังข้อมูลบัญชีผู้ใช้" }
            require(uri.port == -1 || uri.port == 443) { "ไม่อนุญาตพอร์ตดาวน์โหลดที่ไม่ใช่ HTTPS มาตรฐาน" }
            val host = uri.host?.lowercase()?.trimEnd('.')
                ?: throw IllegalArgumentException("URL ดาวน์โหลดไม่มีชื่อโฮสต์ที่ถูกต้อง")
            require(host != "localhost" && !host.endsWith(".localhost") &&
                !host.endsWith(".local") && !host.endsWith(".internal") && !host.endsWith(".lan")) {
                "ไม่อนุญาตปลายทางดาวน์โหลดเครือข่ายภายใน"
            }
            val ipLiteral = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) || host.contains(":")
            if (ipLiteral) {
                val literal = host.removePrefix("[").removeSuffix("]")
                require(isPublicAddressLiteral(InetAddress.getByName(literal))) {
                    "ไม่อนุญาต IP ดาวน์โหลดภายในหรือ IP ที่ไม่ใช่สาธารณะ"
                }
            }
            return uri.toASCIIString()
        }

        private fun isPublicAddressLiteral(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress ||
                address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return false
            val bytes = address.address.map { it.toInt() and 0xff }
            if (bytes.size == 4) {
                val a = bytes[0]; val b = bytes[1]; val c = bytes[2]
                if (a == 0 || a == 10 || a == 127 || a >= 224 || (a == 169 && b == 254) ||
                    (a == 172 && b in 16..31) || (a == 192 && b == 168) ||
                    (a == 100 && b in 64..127) || (a == 192 && b == 0 && c in listOf(0, 2)) ||
                    (a == 198 && b in 18..19) || (a == 198 && b == 51 && c == 100) ||
                    (a == 203 && b == 0 && c == 113)) return false
                return true
            }
            if (bytes.size != 16) return false
            val globalUnicast = bytes[0] in 0x20..0x3f
            val documentation = bytes[0] == 0x20 && bytes[1] == 0x01 &&
                bytes[2] == 0x0d && bytes[3] == 0xb8
            val ietfProtocolAssignments = bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] in 0x00..0x01
            val sixToFour = bytes[0] == 0x20 && bytes[1] == 0x02
            val documentation3fff = bytes[0] == 0x3f && (bytes[1] and 0xf0) == 0xf0
            return globalUnicast && !documentation && !ietfProtocolAssignments &&
                !sixToFour && !documentation3fff
        }
        const val MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        const val MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        const val EXPECTED_BYTES = 514_000_000L // Approximate progress denominator only.
        const val EXPECTED_SHA256 = "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db"
        private const val MIN_MODEL_BYTES = 400_000_000L
        private const val MAX_MODEL_BYTES = 600_000_000L
        private const val MIN_FREE_BYTES = 650_000_000L
    }
}

object GgufFileValidator {
    /** Accept only the pinned starter model; never load arbitrary or merely GGUF-shaped files. */
    fun isVerifiedDefaultModel(file: File): Boolean =
        file.isFile && file.length() in 400_000_000L..600_000_000L && isValid(file) &&
            runCatching { sha256(file).equals(LocalModelStore.EXPECTED_SHA256, ignoreCase = true) }
                .getOrDefault(false)

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun isValid(file: File): Boolean {
        if (!file.isFile || file.length() < 4L) return false
        return runCatching {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                input.read(magic) == 4 &&
                    magic.contentEquals(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte()))
            }
        }.getOrDefault(false)
    }
}
