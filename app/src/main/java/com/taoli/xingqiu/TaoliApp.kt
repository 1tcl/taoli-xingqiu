package com.taoli.xingqiu

import android.app.Application
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.StringWriter
import java.io.PrintWriter

class TaoliApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set crash handler BEFORE anything else
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TaoliApp", "CRASH!", throwable)

            // Save crash to shared prefs
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashMsg = throwable.javaClass.simpleName + ": " + throwable.message + "\n" + sw.toString().take(1000)
            getSharedPreferences("taoli_crash", MODE_PRIVATE).edit()
                .putString("last_crash", crashMsg)
                .putLong("crash_time", System.currentTimeMillis())
                .apply()

            // Try to show dialog on main thread
            try {
                Handler(Looper.getMainLooper()).post {
                    try {
                        val intent = Intent(this, CrashActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("crash_msg", crashMsg)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("TaoliApp", "Could not start crash activity", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("TaoliApp", "Could not post to main thread", e)
            }

            // Call old handler
            oldHandler?.uncaughtException(thread, throwable)
        }
    }
}
