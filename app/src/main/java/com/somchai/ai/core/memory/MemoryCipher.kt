package com.somchai.ai.core.memory

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface MemoryCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

/** Plain codec used only by JVM unit tests. Production runtime injects AndroidMemoryCipher. */
object PlaintextMemoryCipher : MemoryCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext
    override fun decrypt(ciphertext: ByteArray): ByteArray = ciphertext
}

/** AES-256-GCM with a non-exportable key held by Android Keystore. */
class AndroidMemoryCipher(
    private val alias: String = "somchai_local_memory_aes_gcm_v1"
) : MemoryCipher {
    private val magic = byteArrayOf(0x53, 0x43, 0x4d, 0x31) // SCM1

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        return ByteBuffer.allocate(magic.size + 1 + iv.size + encrypted.size)
            .put(magic)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.size > magic.size + 1 + 12 + 16) { "Encrypted memory file is truncated" }
        val buffer = ByteBuffer.wrap(ciphertext)
        val header = ByteArray(magic.size)
        buffer.get(header)
        require(header.contentEquals(magic)) { "Unknown encrypted memory format" }
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength == 12 && buffer.remaining() > ivLength + 16) { "Invalid memory encryption header" }
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
