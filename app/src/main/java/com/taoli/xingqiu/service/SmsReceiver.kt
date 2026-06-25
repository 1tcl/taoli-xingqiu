package com.taoli.xingqiu.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import java.util.regex.Pattern

/**
 * Listens for incoming SMS messages and checks for bank/payment SMS.
 * This is a backup for NotificationListenerService.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val BANK_SMS_KEYWORDS = listOf(
            "银行", "信用卡", "借记卡", "储蓄卡",
            "支出", "消费", "扣款", "转账", "汇款",
            "支付宝", "微信支付"
        )

        private val AMOUNT_PATTERNS = listOf(
            Pattern.compile("(?:消费|扣款|支出|转账|付款)(?:金额)?[：:]?\s*(\d+(?:\.\d{1,2})?)"),
            Pattern.compile("(\d+(?:\.\d{1,2})?)\s*元"),
            Pattern.compile("¥\s*(\d+(?:\.\d{1,2})?)"),
            Pattern.compile("￥\s*(\d+(?:\.\d{1,2})?)"),
            Pattern.compile("\$(\d+(?:\.\d{1,2})?)"),
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            @Suppress("DEPRECATION")
            android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent)
        }

        for (message in messages) {
            val body = message.messageBody ?: continue
            val sender = message.originatingAddress ?: ""

            val isBankSms = BANK_SMS_KEYWORDS.any { keyword ->
                body.contains(keyword, ignoreCase = true)
            }
            if (!isBankSms) continue

            var amount: Double? = null
            for (pattern in AMOUNT_PATTERNS) {
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    amount = matcher.group(1)?.toDoubleOrNull()
                    if (amount != null && amount > 0.01) break
                }
            }
            if (amount == null) continue

            // Save pending payment
            val prefs = context.getSharedPreferences("taoli_pending", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("pending_amount", amount.toString())
                putString("pending_note", body.take(50))
                putLong("pending_time", System.currentTimeMillis())
                apply()
            }

            Log.d("SmsReceiver", "Detected payment SMS: $sender - $amount CNY")
        }
    }
}
