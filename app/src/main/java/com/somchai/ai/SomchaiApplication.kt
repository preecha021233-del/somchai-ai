package com.somchai.ai

import android.app.Application
import com.somchai.ai.core.CoreRuntime

class SomchaiApplication : Application() {
    lateinit var runtime: CoreRuntime
        private set
    override fun onCreate() {
        super.onCreate()
        runtime = CoreRuntime(this)
    }
}
