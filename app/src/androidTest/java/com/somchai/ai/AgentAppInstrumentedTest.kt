package com.somchai.ai

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeRule.onNodeWithText("SOMCHAI AI").assertExists()
        composeRule.onNodeWithContentDescription("เปิดเมนู").performClick()
        composeRule.onNodeWithText("ตั้งค่า").assertExists()
        composeRule.onNodeWithText("ปลั๊กอิน").assertExists()
        composeRule.onNodeWithText("บันทึก").assertExists()
    }
}
