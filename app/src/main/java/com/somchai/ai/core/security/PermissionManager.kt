package com.somchai.ai.core.security

class PermissionManager {
    private val allowed = setOf("network.search", "memory.read", "memory.write")
    fun isAllowed(permission: String): Boolean = permission in allowed
    fun require(permission: String) {
        check(isAllowed(permission)) { "Permission denied: $permission" }
    }
    fun sanitizeExternalData(value: String): String = value
        .replace(Regex("(?i)ignore (all )?previous instructions"), "[untrusted instruction removed]")
        .take(12_000)
}
