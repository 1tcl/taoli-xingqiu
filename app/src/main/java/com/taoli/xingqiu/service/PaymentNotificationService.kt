package com.taoli.xingqiu.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.taoli.xingqiu.MainActivity
import com.taoli.xingqiu.data.DatabaseHelper
import com.taoli.xingqiu.model.Record
import java.util.regex.Pattern

/**
 * Monitors notifications for payment-related messages from:
 * - Alipay (com.eg.android.AlipayGphone)
 * - WeChat Pay (com.tencent.mm)
 * - Bank apps (various package names)
 *
 * When a payment notification is detected, extracts the amount and
 * either shows the app dialog or saves a pending detection.
 */
class PaymentNotificationService : NotificationListenerService() {

    companion object {
        // Payment app package names to monitor
        val MONITORED_PACKAGES = setOf(
            "com.eg.android.AlipayGphone",      // Alipay
            "com.tencent.mm",                     // WeChat
            "com.android.mms",                    // SMS (for bank messages)
            "com.google.android.apps.messaging",  // Google Messages
            "com.samsung.android.messaging",      // Samsung Messages
            "com.xiaomi.smsextra",                // Xiaomi SMS
            "com.huawei.message"                   // Huawei SMS
        )

        // Keywords indicating a payment
        val PAYMENT_KEYWORDS = listOf(
            "付款", "支付", "消费", "扣款", "支出",
            "转账", "汇款", "缴费",
            "paid", "payment", "spent", "charged",
            "USD", "CNY", "RMB", "EUR", "¥", "$"
        )

        // Regex patterns to extract amounts
        val AMOUNT_PATTERNS = listOf(
            Regex("""¥\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""￥\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""\$(\d+(?:\.\d{1,2})?)"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*CNY"""),
            Regex("""金额[：:]\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""消费(\d+(?:\.\d{1,2})?)"""),
            Regex("""支出[：:]\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""(\d+(?:\.\d{1,2})?)"""),
        )

        // Notification text to extract
        val DESCRIPTION_KEYS = listOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        android.util.Log.d("TaoliNotif", "收到通知: $packageName")

        if (packageName !in MONITORED_PACKAGES) {
            android.util.Log.d("TaoliNotif", "包名 $packageName 不在监控列表中，跳过")
            return
        }

        val notification = sbn.notification
        val bundle = notification.extras

        // Extract all text from notification
        val textContent = StringBuilder()
        for (key in DESCRIPTION_KEYS) {
            val value = bundle.get(key)
            if (value is CharSequence) {
                textContent.append(value).append(" ")
            }
        }
        val text = textContent.toString()
        android.util.Log.d("TaoliNotif", "通知文本: $text")

        // Check if it's a payment notification
        val isPayment = PAYMENT_KEYWORDS.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
        if (!isPayment) {
            android.util.Log.d("TaoliNotif", "未检测到付款关键词，跳过")
            return
        }

        // Try to extract amount
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    amount = match.groupValues[1].toDoubleOrNull()
                    if (amount != null && amount > 0.01 && amount < 1000000) break
                } catch (e: Exception) {
                    // ignore parse error
                }
            }
        }
        if (amount == null) {
            android.util.Log.d("TaoliNotif", "未能提取金额，跳过")
            return
        }

        android.util.Log.d("TaoliNotif", "检测到付款: ¥$amount, 通知来源: $packageName")

        // Extract note from notification title or text
        val title = bundle.get(Notification.EXTRA_TITLE)?.toString() ?: ""
        val note = if (title.length > 30) title.take(30) + "..." else title

        // Save to pending detection (shared prefs) or broadcast to app
        val prefs = getSharedPreferences("taoli_pending", MODE_PRIVATE)
        prefs.edit().apply {
            putString("pending_amount", amount.toString())
            putString("pending_note", note)
            putLong("pending_time", System.currentTimeMillis())
            apply()
        }

        // Try to open the app or send a broadcast
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("show_payment_dialog", true)
            putExtra("detected_amount", amount)
            putExtra("detected_note", note)
        }
        startActivity(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
