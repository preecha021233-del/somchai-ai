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
        if (!candidate.exists() && !active.exists()) {
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
        ?.mapNotNull { file -> runCatching {
            val raw = file.readText()
            val manifest = validator.validate(raw)
            PluginCandidate(manifest, sha256(raw), smokeTester.run(manifest))
        }.getOrNull() } ?: emptyList()

    fun activePlugins(): List<PluginManifest> = paths.active.listFiles()
        ?.filter { it.isFile && it.extension == "json" }
        ?.mapNotNull { runCatching { validator.validate(it.readText()) }.getOrNull() }
        ?: emptyList()

    @Synchronized
    fun approve(id: String, expectedHash: String): Boolean {
        val source = File(paths.staging, "$id.json")
        require(paths.contained(paths.staging, source) && source.isFile) { "Candidate not found" }
        val raw = source.readText()
        val manifest = validator.validate(raw)
        require(manifest.id == id && sha256(raw) == expectedHash) { "Candidate changed after review" }
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
            smokeTester.run(activated)
            logger.record("PLUGIN_APPROVED", "$id hash=$expectedHash checks=${checks.size}")
            source.delete()
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
        val manifest = validator.validate(source.readText())
        require(manifest.id == id) { "Plugin identity mismatch" }
        val target = File(paths.backups, "disabled-$id-${System.currentTimeMillis()}.json")
        source.copyTo(target, overwrite = false)
        check(source.delete()) { "Could not remove plugin from active set" }
        logger.record("PLUGIN_DISABLED", "$id backup=${target.name}")
        return true
    }

    fun reject(id: String) {
        File(paths.staging, "$id.json").delete()
        logger.record("PLUGIN_REJECTED", id)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
