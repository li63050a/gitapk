package com.git.app

import android.app.Application
import com.git.app.log.CrashHandler
import com.git.app.log.Log

class GitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.init(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        Log.i("App", "Git App 启动")
    }
}
