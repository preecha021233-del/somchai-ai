package com.somchai.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.somchai.ai.ui.AgentApp
import com.somchai.ai.ui.theme.SomchaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = (application as SomchaiApplication).runtime
        setContent { SomchaiTheme { AgentApp(runtime) } }
    }
}
