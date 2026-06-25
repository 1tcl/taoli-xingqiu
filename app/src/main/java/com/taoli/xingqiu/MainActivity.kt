package com.taoli.xingqiu

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
            val status = findViewById<TextView>(R.id.status_text)

            // Test 1: Database
            try {
                val dbHelper = com.taoli.xingqiu.data.DatabaseHelper(this)
                val count = dbHelper.allRecords.size
                status.text = "✅ 数据库正常，${count}条记录"
            } catch (e: Exception) {
                status.text = "❌ 数据库: ${e.message}"
            }

        } catch (e: Exception) {
            // Fallback: use basic Android view
            val tv = TextView(this)
            tv.text = "崩溃: ${e.message}\n\n${e.stackTraceToString().take(500)}"
            tv.textSize = 12f
            tv.setPadding(32, 32, 32, 32)
            setContentView(tv)
        }
    }
}
