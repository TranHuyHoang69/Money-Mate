package com.example.moneymate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.util.AlarmScheduler
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Bỏ qua nếu là tín hiệu khởi động lại (đã có BootReceiver chuyên trách lo)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) return

        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val userId = intent.getStringExtra("USER_ID") ?: ""
        val title = intent.getStringExtra("TITLE") ?: "Nhắc nhở từ MoneyMate"
        val note = intent.getStringExtra("NOTE") ?: ""
        val repeatInterval = intent.getStringExtra("REPEAT_INTERVAL") ?: "Một lần"

        Log.d("ReminderReceiver", "Nhận được tín hiệu kích hoạt Alarm ID: $reminderId - Tiêu đề: $title")

        if (reminderId != -1) {
            // 1. Hiển thị thông báo (Notification) cho người dùng
            showNotification(context, reminderId, title, note)

            // 2. 🔥 TIẾN HÀNH RE-SCHEDULE CHO CHU KỲ TIẾP THEO NẾU CÓ CẤU HÌNH LẶP
            if (repeatInterval != "Một lần" && repeatInterval.isNotEmpty()) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis() // ✅ Dùng thời gian hiện tại làm mốc nạp
                }

                when (repeatInterval) {
                    "Hàng ngày" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    "Hàng tuần" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "Mỗi 2 tuần" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                    "Mỗi 4 tuần" -> calendar.add(Calendar.WEEK_OF_YEAR, 4)
                    "Hàng tháng" -> calendar.add(Calendar.MONTH, 1)
                    "Mỗi 2 tháng" -> calendar.add(Calendar.MONTH, 2)
                    "Hàng quý" -> calendar.add(Calendar.MONTH, 3)
                    "Mỗi 6 tháng" -> calendar.add(Calendar.MONTH, 6)
                    "Mỗi năm" -> calendar.add(Calendar.YEAR, 1)
                    else -> return
                }

                // Phòng hờ nếu tính toán xong mốc này vẫn <= thời gian hiện tại thì tiếp tục cộng thêm 1 ngày
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val nextReminder = ReminderEntity(
                    id = reminderId,
                    userId = userId,
                    title = title,
                    note = note,
                    reminderDateTime = calendar.timeInMillis,
                    repeatInterval = repeatInterval,
                    isActive = true
                )

                // Gọi đặt lịch nối tiếp vòng đời
                AlarmScheduler.scheduleAlarm(context, nextReminder)
                Log.d("ReminderReceiver", "Đã Re-schedule tự động thành công cho [$title] mốc tiếp theo: ${calendar.time}")
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "moneymate_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lời nhắc MoneyMate",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh hiển thị thông báo nhắc nhở ghi chép chi tiêu"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Định nghĩa hành động khi bấm vào thông báo mở app (Cấu hình tùy biến Intent đích của bạn)
        val rootIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_overlay) // Thay bằng R.drawable.ic_notification của bạn
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}