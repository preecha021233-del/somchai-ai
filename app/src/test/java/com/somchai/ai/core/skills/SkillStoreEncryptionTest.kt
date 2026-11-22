package com.somchai.ai.core.skills

import com.somchai.ai.core.memory.AesGcmMemoryCipher
import com.somchai.ai.core.memory.MemoryCipher
import com.somchai.ai.core.brain.BrainRequest
import com.somchai.ai.core.brain.OnDeviceInferenceEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.Key
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SkillStoreEncryptionTest {
    private class FakeEngine : OnDeviceInferenceEngine {
        override val engineId = "test-engine"
        override fun isModelLoaded() = true
        override suspend fun generate(request: BrainRequest): String = "อ่านข้อมูล แยกประเด็น และสรุปอย่างเป็นขั้นตอน"
    }

    @Test fun skillFileIsEncryptedAndTamperingIsRejected() {
        val dir = createTempDir()
        try {
            val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            val cipher = AesGcmMemoryCipher { key }
            val store = SkillStore(dir, cipher)
            val skill = store.propose(
                "ความลับทดสอบ",
                listOf("ความลับทดสอบ"),
                "ขั้นตอนลับที่ไม่ควรปรากฏในไฟล์",
                listOf(
                    SkillTestCase("ข้อมูลทดสอบหนึ่ง", "คำตอบหนึ่ง"),
                    SkillTestCase("ข้อมูลทดสอบสอง", "คำตอบสอง")
                )
            )

            val file = File(dir, "skills.enc")
            val bytes = file.readBytes()
            val raw = String(bytes, Charsets.UTF_8)
            assertFalse(raw.contains("ความลับทดสอบ"))
            assertFalse(raw.contains("ขั้นตอนลับที่ไม่ควรปรากฏในไฟล์"))
            assertTrue(store.get(skill.id) != null)

            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
            file.writeBytes(bytes)
            try {
                store.all()
                throw AssertionError("Tampered encrypted skill file must be rejected")
            } catch (_: Exception) {
                // AES-GCM authentication must reject modified ciphertext.
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
