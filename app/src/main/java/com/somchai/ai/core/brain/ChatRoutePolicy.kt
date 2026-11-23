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
    fun classify(input: String): ChatRoute {
        val text = input.trim()
        return when {
            text.startsWith("/task", ignoreCase = true) -> ChatRoute.TASK_COMMAND
            text.startsWith("/skill", ignoreCase = true) -> ChatRoute.SKILL_COMMAND
            text.equals("/plugins", ignoreCase = true) ||
                text.equals("ตรวจสอบปลั๊กอิน", ignoreCase = true) ||
                text.equals("รายการปลั๊กอิน", ignoreCase = true) -> ChatRoute.PLUGIN_STATUS
            text.startsWith("/search ", ignoreCase = true) -> ChatRoute.WEB_SEARCH
            text.startsWith("/ai ", ignoreCase = true) -> ChatRoute.EXPLICIT_EXTERNAL_AI
            text.startsWith("/") ||
                text.startsWith("จำไว้ว่า ") ||
                text.startsWith("จำว่า ") ||
                listOf("วางแผน", "แผนงาน", "แบ่งงาน", "ขั้นตอน").any { phrase -> text.contains(phrase) } ->
                ChatRoute.INTERNAL_PROCESSOR
            else -> ChatRoute.LOCAL_BRAIN
        }
    }
}
