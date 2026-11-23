package com.somchai.ai.core.plugins

import com.somchai.ai.core.CorePaths
import com.somchai.ai.core.audit.ActivityLogger
import com.somchai.ai.core.security.PermissionManager
import java.io.File
import java.security.MessageDigest

data class PluginCandidate(val manifest: PluginManifest, val hash: String, val smokeChecks: List<String>)

class PluginManager(
    private val paths: CorePaths,
    private val logger: ActivityLogger,
    permissions: PermissionManager
) {
    private val validator = PluginValidator(permissions)
    private val smokeTester = PluginSmokeTester()

    fun ensureStarterCandidate() {
        val candidate = File(paths.staging, "web-search.json")
        val active = File(paths.active, "web-search.json")
        val disabled = paths.backups.listFiles()?.any {
            it.isFile && it.name.startsWith("disabled-web-search-") && it.extension == "json"
        } == true
        if (!candidate.exists() && !active.exists() && !disabled) {
            candidate.writeText(PluginManifest(
                "web-search", "Web Search", 1,
                "ค้นหาข้อมูลสาธารณะจาก DuckDuckGo Instant Answer API",
                listOf("network.search"), "web.search"
            ).toJson().toString(2))
            logger.record("PLUGIN_CANDIDATE_CREATED", "web-search")
        }
    }

    fun candidates(): List<PluginCandidate> = paths.staging.listFiles()
        ?.filter { it.isFile && it.extension == "json" }
        ?.take(MAX_PLUGIN_FILES)
        ?.mapNotNull { file -> runCatching {
            val raw = readBoundedText(file)
            val manifest = validator.validate(raw)
            PluginCandidate(manifest, sha256(raw), smokeTester.run(manifest))
        }.getOrNull() } ?: emptyList()

    /** Only explicitly enabled manifests in the active directory grant runtime access. */
    fun activePlugins(): List<PluginManifest> = paths.active.listFiles()
        ?.filter { it.isFile && it.extension == "json" }
        ?.take(MAX_PLUGIN_FILES)
        ?.mapNotNull { file -> runCatching { validator.validate(readBoundedText(file)) }.getOrNull() }
        ?.filter { it.enabled }
        ?: emptyList()

    fun disabledPlugins(): List<PluginManifest> = paths.backups.listFiles()
        ?.filter { it.isFile && it.name.startsWith("disabled-") && it.extension == "json" }
        ?.take(MAX_PLUGIN_FILES)
        ?.mapNotNull { file -> runCatching { validator.validate(readBoundedText(file)) }.getOrNull() }
        ?.distinctBy { it.id } ?: emptyList()

    @Synchronized
    fun enable(id: String): Boolean {
        require(id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        val source = paths.backups.listFiles()
            ?.filter { it.isFile && it.name.startsWith("disabled-$id-") && it.extension == "json" }
            ?.maxByOrNull { it.lastModified() } ?: return false
        val manifest = validator.validate(readBoundedText(source))
        require(manifest.id == id) { "Plugin identity mismatch" }
        require(manifest.enabled) { "A disabled manifest cannot be reactivated" }
        val target = File(paths.active, "$id.json")
        check(!target.exists()) { "Plugin is already active" }
        val temp = File(paths.active, "$id.tmp")
        source.copyTo(temp, overwrite = false)
        try {
            check(temp.renameTo(target)) { "Atomic plugin reactivation failed" }
            val activated = validator.validate(target.readText())
            check(activated.enabled && activated.id == id) { "Reactivated plugin is not enabled" }
            check(source.delete()) { "Could not remove disabled backup" }
            logger.record("PLUGIN_ENABLED", "$id backup=${source.name}")
            return true
        } catch (error: Exception) {
            temp.delete()
            target.delete()
            throw error
        }
    }

    @Synchronized
    fun approve(id: String, expectedHash: String): Boolean {
        require(id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        val source = File(paths.staging, "$id.json")
        require(paths.contained(paths.staging, source) && source.isFile) { "Candidate not found" }
        val raw = readBoundedText(source)
        val manifest = validator.validate(raw)
        require(manifest.id == id && sha256(raw) == expectedHash) { "Candidate changed after review" }
        require(manifest.enabled) { "A disabled candidate cannot be approved for activation" }
        val checks = smokeTester.run(manifest)
        check(checks.size >= 4) { "Plugin smoke test did not complete" }
        val target = File(paths.active, "$id.json")
        val backup = File(paths.backups, "$id-${System.currentTimeMillis()}.json")
        if (target.exists()) target.copyTo(backup, overwrite = true)
        return try {
            val temp = File(paths.active, "$id.tmp")
            temp.writeText(raw)
            check(temp.renameTo(target)) { "Atomic plugin activation failed" }
            val activated = validator.validate(target.readText())
            check(activated.enabled && activated.id == id) { "Activated plugin is not enabled" }
            smokeTester.run(activated)
            logger.record("PLUGIN_APPROVED", "$id hash=$expectedHash checks=${checks.size}")
            check(source.delete()) { "Could not remove approved candidate" }
            true
        } catch (error: Exception) {
            target.delete()
            if (backup.exists()) backup.copyTo(target, overwrite = true)
            logger.record("PLUGIN_ROLLBACK", "$id: ${error.message}")
            throw error
        }
    }

    @Synchronized
    fun disable(id: String): Boolean {
        require(id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        val source = File(paths.active, "$id.json")
        if (!source.isFile || !paths.contained(paths.active, source)) return false
        val manifest = validator.validate(readBoundedText(source))
        require(manifest.id == id) { "Plugin identity mismatch" }
        val target = File(paths.backups, "disabled-$id-${System.currentTimeMillis()}.json")
        val raw = source.readText()
        // Prefer a same-filesystem rename so disabling is one atomic directory operation.
        // Fall back to a verified temporary copy only when the filesystem cannot rename.
        val moved = source.renameTo(target)
        if (!moved) {
            val temporary = File(paths.backups, ".disabled-$id-${System.nanoTime()}.tmp")
            temporary.writeText(raw)
            check(temporary.readText() == raw) { "Disabled plugin backup verification failed" }
            check(temporary.renameTo(target)) { "Could not publish disabled plugin backup" }
            if (!source.delete()) {
                target.delete()
                throw IllegalStateException("Could not remove plugin from active set")
            }
        }
        try {
            val disabledManifest = validator.validate(target.readText())
            check(disabledManifest.id == id) { "Disabled plugin identity mismatch" }
            logger.record("PLUGIN_DISABLED", "$id backup=${target.name}")
            return true
        } catch (error: Exception) {
            // Restore the active manifest if verification fails after the move.
            if (!source.exists()) target.renameTo(source) else target.delete()
            throw error
        }
    }

    @Synchronized
    fun reject(id: String) {
        require(id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        val candidate = File(paths.staging, "$id.json")
        require(paths.contained(paths.staging, candidate)) { "Candidate path is outside staging" }
        if (candidate.isFile) check(candidate.delete()) { "Could not remove plugin candidate" }
        logger.record("PLUGIN_REJECTED", id)
    }

    private fun readBoundedText(file: File): String {
        require(file.isFile && file.length() in 1..MAX_MANIFEST_BYTES.toLong()) {
            "Plugin manifest is missing, empty, or exceeds the 16 KB limit"
        }
        val bytes = file.readBytes()
        require(bytes.size <= MAX_MANIFEST_BYTES) { "Plugin manifest exceeds the 16 KB limit" }
        return String(bytes, Charsets.UTF_8)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val MAX_MANIFEST_BYTES = 16_384
        private const val MAX_PLUGIN_FILES = 100
    }
}
