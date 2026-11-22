package com.somchai.ai.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.net.URI
import java.net.InetAddress
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("provider_secure", Context.MODE_PRIVATE)
    private val alias = "somchai.provider.aes"
    data class Provider(val baseUrl: String, val model: String, val apiKey: String)

    fun saveProvider(baseUrl: String, model: String, apiKey: String) {
        val safeBaseUrl = normalizeBaseUrl(baseUrl)
        val safeModel = model.trim()
        val safeKey = apiKey.trim()
        require(safeModel.isNotEmpty() && safeModel.length <= 120 &&
            safeModel.none { it.isWhitespace() || it.isISOControl() }) { "Model name is invalid" }
        require(safeKey.isNotEmpty() && safeKey.length <= 4096) { "API key is invalid" }
        prefs.edit().putString("base_url", safeBaseUrl)
            .putString("model", safeModel)
            .putString("key", Base64.encodeToString(encrypt(safeKey), Base64.NO_WRAP)).apply()
    }

    private fun normalizeBaseUrl(value: String): String {
        val clean = value.trim()
        require(clean.length in 9..500) { "Provider URL is invalid" }
        val uri = URI(clean).normalize()
        require(uri.scheme.equals("https", ignoreCase = true)) { "Provider URL must use HTTPS" }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Provider URL cannot contain credentials, query parameters or fragments"
        }
        require(uri.port == -1 || uri.port == 443) { "Provider URL must use the standard HTTPS port" }
        val host = uri.host?.lowercase()?.trimEnd('.')
            ?: throw IllegalArgumentException("Provider URL has no valid host")
        require(host != "localhost" && !host.endsWith(".localhost") &&
            !host.endsWith(".local") && !host.endsWith(".internal") && !host.endsWith(".lan")) {
            "Provider URL cannot target a local network hostname"
        }
        if (host.matches(Regex("""\\d{1,3}(\\.\\d{1,3}){3}""")) || host.contains(":")) {
            val address = InetAddress.getByName(host.removePrefix("[").removeSuffix("]"))
            require(!address.isAnyLocalAddress && !address.isLoopbackAddress &&
                !address.isLinkLocalAddress && !address.isSiteLocalAddress && !address.isMulticastAddress) {
                "Provider URL cannot target a private IP address"
            }
        }
        require(!uri.path.orEmpty().contains("..")) { "Provider URL path is invalid" }
        return uri.toASCIIString().trimEnd('/')
    }

    fun provider(): Provider? {
        val encoded = prefs.getString("key", null) ?: return null
        return runCatching {
            Provider(
                normalizeBaseUrl(prefs.getString("base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"),
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
