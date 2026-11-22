package com.somchai.ai.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("provider_secure", Context.MODE_PRIVATE)
    private val alias = "somchai.provider.aes"
    data class Provider(val baseUrl: String, val model: String, val apiKey: String)

    fun saveProvider(baseUrl: String, model: String, apiKey: String) {
        require(baseUrl.startsWith("https://")) { "Provider URL must use HTTPS" }
        require(model.isNotBlank() && apiKey.isNotBlank()) { "Model and API key are required" }
        prefs.edit().putString("base_url", baseUrl.trim().trimEnd('/'))
            .putString("model", model.trim())
            .putString("key", Base64.encodeToString(encrypt(apiKey), Base64.NO_WRAP)).apply()
    }

    fun provider(): Provider? {
        val encoded = prefs.getString("key", null) ?: return null
        return runCatching {
            Provider(
                prefs.getString("base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
                prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini",
                decrypt(Base64.decode(encoded, Base64.NO_WRAP))
            )
        }.getOrNull()
    }

    fun hasKey(): Boolean = !prefs.getString("key", null).isNullOrBlank()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private fun encrypt(value: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    }

    private fun decrypt(value: ByteArray): String {
        require(value.size > 12) { "Invalid encrypted secret" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, value.copyOfRange(0, 12)))
        return cipher.doFinal(value.copyOfRange(12, value.size)).toString(Charsets.UTF_8)
    }
}
