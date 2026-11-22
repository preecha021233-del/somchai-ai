package com.somchai.ai.core.memory

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class AesGcmMemoryCipherTest {
    private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
    private val cipher = AesGcmMemoryCipher { key }

    @Test fun encryptsAndDecryptsPlaintext() {
        val plaintext = "private owner memory".toByteArray()
        val encrypted = cipher.encrypt(plaintext)
        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains("private owner memory"))
        assertArrayEquals(plaintext, cipher.decrypt(encrypted))
    }

    @Test fun authenticatesEmptyPayload() {
        assertArrayEquals(ByteArray(0), cipher.decrypt(cipher.encrypt(ByteArray(0))))
    }

    @Test fun rejectsModifiedCiphertext() {
        val encrypted = cipher.encrypt("integrity".toByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last() xor 0x01)
        assertThrows(Exception::class.java) { cipher.decrypt(encrypted) }
    }
}
