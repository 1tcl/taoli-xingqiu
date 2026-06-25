package com.taoli.xingqiu

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            val dbHelper = com.taoli.xingqiu.data.DatabaseHelper(this)
            val count = dbHelper.getAllRecords().size
            val status = findViewById<TextView>(R.id.status_text)
            status.text = "✅ 数据库正常，${count}条记录"
        } catch (e: Exception) {
            val status = findViewById<TextView>(R.id.status_text)
            status.text = "❌ ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
