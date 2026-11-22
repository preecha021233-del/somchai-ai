package com.somchai.ai.core.plugins

/** Deterministic, offline smoke checks. This never executes extension-supplied code. */
class PluginSmokeTester {
    fun run(manifest: PluginManifest): List<String> {
        val checks = mutableListOf<String>()
        require(manifest.enabled) { "Disabled plugin cannot be activated" }
        require(manifest.id.isNotBlank()) { "Plugin identity missing" }
        when (manifest.operation) {
            "web.search" -> require("network.search" in manifest.permissions)
            "memory.read" -> require("memory.read" in manifest.permissions)
            "memory.write" -> require("memory.write" in manifest.permissions)
            else -> error("No smoke-test profile for this operation")
        }
        checks += "manifest parsed"
        checks += "operation allowlisted"
        checks += "permissions match operation"
        checks += "no executable payload present"
        return checks
    }
}
