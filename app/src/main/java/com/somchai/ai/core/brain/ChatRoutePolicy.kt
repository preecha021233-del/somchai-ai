package com.somchai.ai.core.brain

enum class ChatRoute {
    TASK_COMMAND,
    SKILL_COMMAND,
    PLUGIN_STATUS,
    WEB_SEARCH,
    EXPLICIT_EXTERNAL_AI,
    INTERNAL_PROCESSOR,
    LOCAL_BRAIN
}

/**
 * Central chat boundary policy. Ordinary conversation is always routed to the
 * local Brain; the external provider is reachable only through an explicit /ai command.
 */
object ChatRoutePolicy {
    fun isTaskLifecycleCommand(input: String): Boolean {
        if (classify(input) != ChatRoute.TASK_COMMAND) return false
        val action = input.trim().split(Regex("\\s+")).getOrNull(1)?.lowercase().orEmpty()
        return action in setOf("list", "show", "start", "complete", "fail", "retry", "cancel")
    }

    fun classify(input: String): ChatRoute {
        val text = input.trim()
        return when {
            (text.equals("/task", ignoreCase = true) || text.startsWith("/task ", ignoreCase = true)) -> ChatRoute.TASK_COMMAND
            (text.equals("/skill", ignoreCase = true) || text.startsWith("/skill ", ignoreCase = true)) -> ChatRoute.SKILL_COMMAND
            text.equals("/plugins", ignoreCase = true) ||
                text.equals("ตรวจสอบปลั๊กอิน", ignoreCase = true) ||
                text.equals("รายการปลั๊กอิน", ignoreCase = true) -> ChatRoute.PLUGIN_STATUS
            text.equals("/search", ignoreCase = true) || text.startsWith("/search ", ignoreCase = true) -> ChatRoute.WEB_SEARCH
            text.equals("/ai", ignoreCase = true) || text.startsWith("/ai ", ignoreCase = true) -> ChatRoute.EXPLICIT_EXTERNAL_AI
            text.startsWith("/") ||
                text.startsWith("จำไว้ว่า ") ||
                text.startsWith("จำว่า ") ||
                listOf("วางแผน", "แผนงาน", "แบ่งงาน", "ขั้นตอน").any { phrase -> text.contains(phrase) } ->
                ChatRoute.INTERNAL_PROCESSOR
            else -> ChatRoute.LOCAL_BRAIN
        }
    }
}
