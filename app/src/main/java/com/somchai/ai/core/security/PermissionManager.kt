package com.somchai.ai.core.security

/**
 * Core-owned capability policy. Plugin manifests cannot grant capabilities:
 * each operation has an exact permission set and unknown operations fail closed.
 */
class PermissionManager {
    private val allowed = setOf("network.search", "memory.read", "memory.write")
    private val operationPermissions = mapOf(
        "web.search" to setOf("network.search")
    )

    fun isAllowed(permission: String): Boolean = permission in allowed

    fun require(permission: String) {
        check(isAllowed(permission)) { "Permission denied: $permission" }
    }

    fun requireOperation(operation: String, requestedPermissions: Collection<String>) {
        val required = operationPermissions[operation]
            ?: throw IllegalArgumentException("Operation is not permitted: $operation")
        require(requestedPermissions.size <= 4) { "Too many permissions" }
        require(requestedPermissions.size == requestedPermissions.toSet().size) {
            "Duplicate permissions"
        }
        requestedPermissions.forEach(::require)
        require(requestedPermissions.toSet() == required) {
            "Permission set does not match operation: $operation"
        }
    }

    /**
     * External page text is data, never authority. Remove control/bidirectional
     * formatting characters, neutralize common instruction overrides, and cap
     * the payload before it reaches a model prompt.
     */
    fun sanitizeExternalData(value: String): String {
        val bounded = value
            .filter { character ->
                character == '\n' || character == '\t' ||
                    (!character.isISOControl() && character.code !in 0x202A..0x202E &&
                        character.code !in 0x2066..0x2069)
            }
            .take(12_000)

        val injectionPatterns = listOf(
            Regex("(?i)ignore\\s+(all\\s+)?previous\\s+instructions"),
            Regex("(?i)disregard\\s+(all\\s+)?(prior|previous)\\s+instructions"),
            Regex("(?i)override\\s+(the\\s+)?system\\s+prompt"),
            Regex("ละเว้นคำสั่ง(ทั้งหมด)?(ก่อนหน้า|ก่อนหน้านี้)"),
            Regex("ไม่ต้องทำตามคำสั่ง(เดิม|ก่อนหน้า)")
        )
        return injectionPatterns.fold(bounded) { text, pattern ->
            pattern.replace(text, "[untrusted instruction removed]")
        }
    }
}
