package com.example.moneymate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Kiểm tra xem có phải tín hiệu khởi động lại hệ thống không
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        // 2. Lấy cả tên lời nhắc và ghi chú từ Intent
        val reminderName = intent.getStringExtra("REMINDER_NAME") ?: "Nhắc nhở"
        val note = intent.getStringExtra("REMINDER_NOTE") ?: ""

        // 3. Tiến hành hiển thị thông báo với định dạng mới
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "money_mate_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nhắc nhở chi tiêu", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Định dạng tiêu đề theo dạng: -MoneyMate-TênLờiNhắc!
        val formattedTitle = "MoneyMate- ${reminderName}!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(formattedTitle)
            .setContentText(note)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}