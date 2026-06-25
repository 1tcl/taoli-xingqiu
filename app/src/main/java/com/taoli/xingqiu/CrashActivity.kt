package com.taoli.xingqiu

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.graphics.Color

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val msg = intent.getStringExtra("crash_msg") ?: "未知错误"

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#FFF3E0"))
        }

        val title = TextView(this).apply {
            text = "APP 崩溃了"
            textSize = 20f
            setTextColor(Color.parseColor("#FF6B35"))
        }
        layout.addView(title)

        val text = TextView(this).apply {
            text = msg
            textSize = 12f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
        }
        layout.addView(text)

        scroll.addView(layout)
        setContentView(scroll)
    }
}
