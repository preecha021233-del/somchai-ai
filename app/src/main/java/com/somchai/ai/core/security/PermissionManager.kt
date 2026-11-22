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
                    (!character.isISOControl() &&
                        Character.getType(character) != Character.FORMAT)
            }
            .take(12_000)

        // Keep this list deliberately explicit: it is a defense-in-depth filter,
        // not a substitute for treating every external page as untrusted data.
        val injectionPatterns = listOf(
            Regex("(?i)ignore\\s+(all\\s+)?previous\\s+instructions"),
            Regex("(?i)ignore\\s+(the\\s+)?(system|developer)\\s+(prompt|message|instructions)"),
            Regex("(?i)disregard\\s+(all\\s+)?(prior|previous)\\s+instructions"),
            Regex("(?i)override\\s+(the\\s+)?system\\s+prompt"),
            Regex("(?i)(reveal|print|show|repeat)\\s+(the\\s+)?(hidden\\s+)?system\\s+prompt"),
            Regex("(?i)you\\s+are\\s+now\\s+(a|an|the)\\s+[^\\n.!?]{1,80}"),
            Regex("ละเว้นคำสั่ง(ทั้งหมด)?(ก่อนหน้า|ก่อนหน้านี้)"),
            Regex("ไม่ต้องทำตามคำสั่ง(เดิม|ก่อนหน้า)"),
            Regex("(เปิดเผย|แสดง|พิมพ์|บอก)คำสั่ง(ระบบ|ภายใน)"),
            Regex("ตั้งแต่นี้ไปคุณ(คือ|ต้อง)[^\\n.!?]{0,80}")
        )
        return injectionPatterns.fold(bounded) { text, pattern ->
            pattern.replace(text, "[untrusted instruction removed]")
        }
    }
}
