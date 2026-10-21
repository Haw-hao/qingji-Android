package ink.qingji.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ink.qingji.app.MainActivity
import ink.qingji.app.R
import ink.qingji.app.data.*
import java.time.LocalDateTime

class QingjiNotificationListenerService : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ReminderChannelId, "轻记自动记账", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "识别到支付通知后的记账提醒"
            })
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val parsed = parsePaymentNotification(sbn.packageName,
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()) ?: return
        val prefs = getSharedPreferences("qingji_settings", MODE_PRIVATE)
        val mode = prefs.getString("notification_mode", "remind") ?: "remind"
        if (mode == "off" || !markHandled(prefs, sbn)) return
        if (mode == "auto") {
            val now = LocalDateTime.now()
            TransactionDb(this).use { db -> db.add(Transaction(0, parsed.merchant, parsed.category,
                parsed.method, parsed.amount, parsed.income, transactionDate(now), transactionTime(now), 0)) }
        } else showReminder(parsed)
    }

    private fun markHandled(prefs: SharedPreferences, sbn: StatusBarNotification): Boolean {
        val key = "${sbn.packageName}:${sbn.postTime}:${sbn.notification.extras}".hashCode().toString()
        val handled = prefs.getStringSet("handled_notification_keys", emptySet()).orEmpty()
        if (key in handled) return false
        prefs.edit().putStringSet("handled_notification_keys", (handled + key).toList().takeLast(200).toSet()).apply()
        return true
    }

    private fun showReminder(parsed: ParsedPaymentNotification) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, parsed.originalText.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val direction = if (parsed.income) "收入" else "支出"
        val merchant = parsed.merchant.ifBlank { parsed.category }
        val notification = Notification.Builder(this, ReminderChannelId)
            .setSmallIcon(R.drawable.qingji_app_icon)
            .setContentTitle("识别到$direction ¥${"%.2f".format(parsed.amount)}")
            .setContentText("$merchant · ${parsed.method} · 打开轻记确认记账")
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(parsed.originalText.hashCode(), notification)
    }

    companion object { const val ReminderChannelId = "qingji_payment_reminders" }
}
