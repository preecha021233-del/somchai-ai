package com.somchai.ai.core.brain

import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
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
        val available = StatFs(directory.absolutePath).availableBytes
        require(available >= MIN_FREE_BYTES) {
            "พื้นที่ว่างไม่เพียงพอ ต้องมีพื้นที่ว่างอย่างน้อยประมาณ 650 MB"
        }

        val partial = File(directory, "$MODEL_FILE_NAME.part")
        partial.delete()
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 90_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SOMCHAI-AI/1.9 local-model-installer")
        }
        try {
            val status = connection.responseCode
            check(status in 200..299) { "ดาวน์โหลดโมเดลไม่สำเร็จ (HTTP $status)" }
            val declared = connection.contentLengthLong
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
            require(total in MIN_MODEL_BYTES..MAX_MODEL_BYTES) { "ดาวน์โหลดไม่ครบหรือขนาดไฟล์โมเดลไม่ถูกต้อง" }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            require(actualHash.equals(EXPECTED_SHA256, ignoreCase = true)) {
                "ตรวจสอบ SHA-256 ไม่ผ่าน ไฟล์โมเดลถูกปฏิเสธ"
            }
            require(GgufFileValidator.isValid(partial)) { "ไฟล์ที่ดาวน์โหลดไม่ใช่โมเดล GGUF" }
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
        file.isFile && file.length() in MIN_MODEL_BYTES..MAX_MODEL_BYTES &&
            runCatching { GgufFileValidator.sha256(file) == EXPECTED_SHA256 && GgufFileValidator.isValid(file) }
                .getOrDefault(false)

    companion object {
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
