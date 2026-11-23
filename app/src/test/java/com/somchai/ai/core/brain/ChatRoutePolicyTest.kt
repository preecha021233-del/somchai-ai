package com.somchai.ai.core.brain

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRoutePolicyTest {
    @Test fun ordinaryThaiAndEnglishConversationAlwaysUsesLocalBrain() {
        listOf("สวัสดี", "ช่วยอธิบายเรื่องนี้", "What is artificial intelligence?", "ช่วยคิดเรื่องโครงการ").forEach {
            assertEquals(ChatRoute.LOCAL_BRAIN, ChatRoutePolicy.classify(it))
        }
    }

    @Test fun externalProviderRequiresExplicitAiCommand() {
        assertEquals(ChatRoute.EXPLICIT_EXTERNAL_AI, ChatRoutePolicy.classify("/ai ช่วยอธิบาย"))
        assertEquals(ChatRoute.EXPLICIT_EXTERNAL_AI, ChatRoutePolicy.classify("/AI explain this"))
        assertEquals(ChatRoute.LOCAL_BRAIN, ChatRoutePolicy.classify("ช่วยใช้ AI อธิบายเรื่องนี้"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("/unknown command"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("/taskevil run"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("/skillful prompt"))
    }

    @Test fun builtInCommandsCannotFallThroughToExternalProvider() {
        assertEquals(ChatRoute.TASK_COMMAND, ChatRoutePolicy.classify("/task list"))
        assertEquals(ChatRoute.SKILL_COMMAND, ChatRoutePolicy.classify("/skill list"))
        assertEquals(ChatRoute.PLUGIN_STATUS, ChatRoutePolicy.classify("/plugins"))
        assertEquals(ChatRoute.WEB_SEARCH, ChatRoutePolicy.classify("/search หาข้อมูล"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("/help"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("/plan สอบปลายภาค"))
        assertEquals(ChatRoute.INTERNAL_PROCESSOR, ChatRoutePolicy.classify("จำไว้ว่า นัดหมายพรุ่งนี้"))
    }

    @Test fun taskLifecycleCommandsAreSeparatedFromNewTaskCreation() {
        listOf("/task help", "/task list", "/task show abc", "/task start abc", "/task complete abc",
            "/task fail abc", "/task retry abc", "/task cancel abc").forEach {
            assertEquals(true, ChatRoutePolicy.isTaskLifecycleCommand(it))
        }
        assertEquals(false, ChatRoutePolicy.isTaskLifecycleCommand("/task"))
        assertEquals(false, ChatRoutePolicy.isTaskLifecycleCommand("/task พัฒนาแอป SOMCHAI"))
        assertEquals(false, ChatRoutePolicy.isTaskLifecycleCommand("/taskevil list"))
    }

    @Test fun pluginStatusAliasesNeverRouteToWebSearchOrExternalAi() {
        listOf("/plugins", "ตรวจสอบปลั๊กอิน", "รายการปลั๊กอิน").forEach {
            assertEquals(ChatRoute.PLUGIN_STATUS, ChatRoutePolicy.classify(it))
        }
        assertEquals(ChatRoute.WEB_SEARCH, ChatRoutePolicy.classify("/search somchai"))
        assertEquals(ChatRoute.EXPLICIT_EXTERNAL_AI, ChatRoutePolicy.classify("/ai somchai"))
    }

}
