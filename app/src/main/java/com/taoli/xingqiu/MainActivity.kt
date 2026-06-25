package com.taoli.xingqiu

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.taoli.xingqiu.data.DatabaseHelper
import com.taoli.xingqiu.model.Record
import com.taoli.xingqiu.ui.CategoryFragment
import com.taoli.xingqiu.ui.OverviewFragment
import com.taoli.xingqiu.ui.RecordsFragment

class MainActivity : AppCompatActivity() {

    lateinit var dbHelper: DatabaseHelper
    lateinit var prefs: SharedPreferences
    private val fragments = mutableListOf<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val msg = e.toString() + "\n\n" + e.stackTrace.take(10).joinToString("\n")
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("APP 崩溃了")
                    .setMessage(msg)
                    .setPositiveButton("确定") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("启动崩溃")
                .setMessage(e.toString())
                .setPositiveButton("确定") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        dbHelper = DatabaseHelper(this)
        prefs = getSharedPreferences("taoli_pending", MODE_PRIVATE)

        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Initialize fragments
        val overviewFragment = OverviewFragment()
        val recordsFragment = RecordsFragment()
        val categoryFragment = CategoryFragment()

        fragments.add(overviewFragment)
        fragments.add(recordsFragment)
        fragments.add(categoryFragment)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, categoryFragment, "categories").hide(categoryFragment)
            .add(R.id.fragment_container, recordsFragment, "records").hide(recordsFragment)
            .add(R.id.fragment_container, overviewFragment, "overview")
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_overview -> overviewFragment
                R.id.nav_records -> recordsFragment
                R.id.nav_categories -> categoryFragment
                else -> return@setOnItemSelectedListener false
            }
            for (f in fragments) {
                supportFragmentManager.beginTransaction().hide(f).commit()
            }
            supportFragmentManager.beginTransaction().show(fragment).commit()
            true
        }

        // Check for pending payment on launch
        handleIntent(intent)

        // Show notification listener setup hint on first launch
        if (!prefs.getBoolean("setup_shown", false)) {
            showNotificationListenerSetup()
            prefs.edit().putBoolean("setup_shown", true).apply()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val showDialog = intent.getBooleanExtra("show_payment_dialog", false)
        if (showDialog) {
            val amount = intent.getDoubleExtra("detected_amount", 0.0)
            val note = intent.getStringExtra("detected_note") ?: ""
            if (amount > 0) {
                showPaymentDialog(amount, note)
            }
        } else {
            // Check pending from shared prefs
            val pendingAmount = prefs.getString("pending_amount", null)?.toDoubleOrNull()
            if (pendingAmount != null && pendingAmount > 0) {
                val pendingNote = prefs.getString("pending_note", "") ?: ""
                showPaymentDialog(pendingAmount, pendingNote)
                prefs.edit().remove("pending_amount").remove("pending_note").remove("pending_time").apply()
            }
        }
    }

    fun showPaymentDialog(prefillAmount: Double = 0.0, prefillNote: String = "") {
        val view = layoutInflater.inflate(R.layout.dialog_payment, null)
        val amountInput = view.findViewById<EditText>(R.id.pay_amount)
        val noteInput = view.findViewById<EditText>(R.id.pay_note)
        val typeGrid = view.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.type_grid)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_pay)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btn_confirm_pay)

        if (prefillAmount > 0) {
            amountInput.setText(String.format("%.2f", prefillAmount))
        }
        if (prefillNote.isNotEmpty()) {
            noteInput.setText(prefillNote)
        }

        val categories = getCategories()
        var selectedType: String? = null

        // Build type chips
        for (cat in categories) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                setTextColor(Color.parseColor("#2D2D2D"))
                chipBackgroundColor = resources.getColorStateList(android.R.color.white, null)
                setOnClickListener {
                    selectedType = cat
                    // Uncheck others
                    for (i in 0 until typeGrid.childCount) {
                        val c = typeGrid.getChildAt(i) as? Chip
                        c?.isChecked = (c?.text == cat)
                    }
                }
            }
            typeGrid.addView(chip)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val amountStr = amountInput.text.toString()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedType == null) {
                Toast.makeText(this, "请选择消费类型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val note = noteInput.text.toString().trim()
            val record = Record(
                amount = amount,
                category = selectedType!!,
                note = note,
                time = System.currentTimeMillis()
            )
            dbHelper.insertRecord(record)
            Toast.makeText(this, "已记录: ${selectedType} ¥${String.format("%.2f", amount)}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            // Refresh fragments
            refreshAllFragments()
        }

        dialog.show()
    }

    fun getCategories(): List<String> {
        val saved = prefs.getString("categories", null)
        return if (saved != null) {
            saved.split(",").filter { it.isNotBlank() }
        } else {
            listOf("饭菜钱", "路程篇", "购物消费", "娱乐消费", "日常用品", "其他")
        }
    }

    fun saveCategories(categories: List<String>) {
        prefs.edit().putString("categories", categories.joinToString(",")).apply()
    }

    fun refreshAllFragments() {
        for (f in fragments) {
            when (f) {
                is OverviewFragment -> f.refresh()
                is RecordsFragment -> f.refresh()
                is CategoryFragment -> f.refresh()
            }
        }
    }

    private fun showNotificationListenerSetup() {
        MaterialAlertDialogBuilder(this)
            .setTitle("开启付费监测")
            .setMessage(
                "陶离猩球需要开启「通知监听」权限才能自动监测支付宝、微信支付等付费通知。\n\n" +
                "1. 点击「去设置」\n" +
                "2. 找到「陶离猩球」并开启权限\n\n" +
                "（你也可以跳过此步骤，手动点击首页的💰按钮记录消费）"
            )
            .setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("稍后设置", null)
            .show()
    }
}
