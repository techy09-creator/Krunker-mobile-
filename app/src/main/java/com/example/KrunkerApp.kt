package com.example

import android.app.Application
import android.content.Context
import android.system.Os
import java.io.File

class KrunkerApp : Application() {

    companion object {
        init {
            try {
                // Configure environment before native graphics libraries load
                Os.setenv("MESA_LOG_LEVEL", "silent", true)
                Os.setenv("LIBGL_DRI3_DISABLE", "1", true)
            } catch (_: Throwable) {
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        try {
            Os.setenv("MESA_LOG_LEVEL", "silent", true)
            Os.setenv("LIBGL_DRI3_DISABLE", "1", true)
        } catch (_: Throwable) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Os.setenv("MESA_LOG_LEVEL", "silent", true)
            Os.setenv("LIBGL_DRI3_DISABLE", "1", true)
        } catch (_: Throwable) {
        }
    }
}
