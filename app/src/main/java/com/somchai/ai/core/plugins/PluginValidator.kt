package com.somchai.ai.core.plugins

import com.somchai.ai.core.security.PermissionManager

class PluginValidator(private val permissions: PermissionManager) {
    fun validate(raw: String): PluginManifest {
        require(raw.toByteArray().size <= 16_384) { "Plugin manifest exceeds 16 KB" }
        val manifest = PluginManifest.parse(raw)
        require(manifest.id.matches(Regex("[a-z][a-z0-9-]{2,39}"))) { "Invalid plugin ID" }
        require(manifest.name.isNotBlank() && manifest.name.length <= 60) { "Invalid plugin name" }
        require(manifest.version > 0) { "Version must be positive" }
        require(manifest.description.length <= 300) { "Description too long" }
        permissions.requireOperation(manifest.operation, manifest.permissions)
        return manifest
    }
}
