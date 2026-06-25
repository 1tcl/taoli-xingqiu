package com.taoli.xingqiu

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        prefs = getSharedPreferences("taoli_pending", MODE_PRIVATE)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

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

        // Check pending payment
        val pendingAmount = prefs.getString("pending_amount", null)?.toDoubleOrNull()
        if (pendingAmount != null && pendingAmount > 0) {
            val pendingNote = prefs.getString("pending_note", "") ?: ""
            showPaymentDialog(pendingAmount, pendingNote)
            prefs.edit().remove("pending_amount").remove("pending_note").remove("pending_time").apply()
        }

        // Show setup hint on first launch
        if (!prefs.getBoolean("setup_shown", false)) {
            showNotificationListenerSetup()
            prefs.edit().putBoolean("setup_shown", true).apply()
        }
    }

    fun showPaymentDialog(prefillAmount: Double = 0.0, prefillNote: String = "") {
        val view = layoutInflater.inflate(R.layout.dialog_payment, null)
        val amountInput = view.findViewById<EditText>(R.id.pay_amount)
        val noteInput = view.findViewById<EditText>(R.id.pay_note)
        val typeGrid = view.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.type_grid)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel_pay)
        val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_confirm_pay)

        if (prefillAmount > 0) amountInput.setText(String.format("%.2f", prefillAmount))
        if (prefillNote.isNotEmpty()) noteInput.setText(prefillNote)

        val categories = getCategories()
        var selectedType: String? = null

        for (cat in categories) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                setTextColor(Color.parseColor("#2D2D2D"))
                setOnClickListener {
                    selectedType = cat
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
            val amount = amountInput.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedType == null) {
                Toast.makeText(this, "请选择消费类型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val record = Record(
                amount = amount,
                category = selectedType!!,
                note = noteInput.text.toString().trim(),
                time = System.currentTimeMillis()
            )
            dbHelper.insertRecord(record)
            Toast.makeText(this, "已记录: ${selectedType} ¥${String.format("%.2f", amount)}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshAllFragments()
        }
        dialog.show()
    }

    fun getCategories(): List<String> {
        val saved = prefs.getString("categories", null)
        return if (saved != null) saved.split(",").filter { it.isNotBlank() }
        else listOf("饭菜钱", "路程篇", "购物消费", "娱乐消费", "日常用品", "其他")
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
                "（你也可以跳过此步骤，手动点击首页按钮记录消费）"
            )
            .setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("稍后设置", null)
            .show()
    }
}
