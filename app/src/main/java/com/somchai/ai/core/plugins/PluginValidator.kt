package com.somchai.ai.core.plugins

import com.somchai.ai.core.security.PermissionManager

class PluginValidator(private val permissions: PermissionManager) {
    private val operations = setOf("web.search", "memory.read", "memory.write")
    fun validate(raw: String): PluginManifest {
        require(raw.toByteArray().size <= 16_384) { "Plugin manifest exceeds 16 KB" }
        val manifest = PluginManifest.parse(raw)
        require(manifest.id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        require(manifest.name.isNotBlank() && manifest.name.length <= 60) { "Invalid plugin name" }
        require(manifest.version > 0) { "Version must be positive" }
        require(manifest.description.length <= 300) { "Description too long" }
        require(manifest.operation in operations) { "Operation is not in the core allowlist" }
        require(manifest.permissions.size <= 4) { "Too many permissions" }
        require(manifest.permissions.distinct().size == manifest.permissions.size) { "Duplicate permissions" }
        manifest.permissions.forEach { permissions.require(it) }
        when (manifest.operation) {
            "web.search" -> require("network.search" in manifest.permissions)
            "memory.read" -> require("memory.read" in manifest.permissions)
            "memory.write" -> require("memory.write" in manifest.permissions)
        }
        return manifest
    }
}
