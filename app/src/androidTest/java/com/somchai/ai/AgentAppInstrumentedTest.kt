package com.somchai.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import android.content.ContextWrapper
import com.somchai.ai.core.CoreRuntime
import java.io.File
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Minimal device-side smoke coverage for the actual launcher activity and navigation. */
@RunWith(AndroidJUnit4::class)
class AgentAppInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherRendersChatAndTopRightMenuOpensNavigation() {
        composeRule.onNodeWithText("SOMCHAI AI").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("เปิดเมนู").performClick()
        composeRule.onNodeWithText("ตั้งค่า").assertIsDisplayed()
        composeRule.onNodeWithText("ปลั๊กอิน").assertIsDisplayed()
        composeRule.onNodeWithText("บันทึก").assertIsDisplayed()
    }

    @Test
    fun encryptedMemorySurvivesCoreRuntimeRecreation() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedRoot = File(base.cacheDir, "memory-restart-${UUID.randomUUID()}")
        val isolatedContext = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(isolatedRoot, "files")
        }

        try {
            val firstRuntime = CoreRuntime(isolatedContext)
            val saved = firstRuntime.processor.process("/remember ข้อมูลต้องอยู่หลังเปิดแอปใหม่")
            val id = Regex("\\(รหัส ([0-9a-fA-F-]{36})\\)")
                .find(saved.answer)?.groupValues?.get(1)
            assertTrue("Expected full stable memory UUID", id != null)

            // Recreate the complete runtime against the same app-private directory and Keystore key.
            val reopenedRuntime = CoreRuntime(isolatedContext)
            val recalled = reopenedRuntime.processor.process("/recall $id")
            assertTrue(recalled.answer.contains("[$id] ข้อมูลต้องอยู่หลังเปิดแอปใหม่"))
        } finally {
            isolatedRoot.deleteRecursively()
        }
    }

}