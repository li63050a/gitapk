package com.git.app

import android.app.Application
import com.git.app.log.CrashHandler
import com.git.app.log.Log

class GitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.init(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        // Ensure the app's private external storage directory
        // (/storage/emulated/0/Android/data/<package>/files) exists so it is
        // visible in a file manager.
        applicationContext.getExternalFilesDir(null)
        Log.i("App", "Git App 启动")
    }
}
