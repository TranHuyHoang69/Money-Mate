package com.example.moneymate.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.moneymate.R
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.util.AlarmScheduler
import com.example.moneymate.util.ReminderRepeat
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
        val repeat = ReminderRepeat.fromStored(repeatInterval)

        Log.d("ReminderReceiver", "Nhận được tín hiệu kích hoạt Alarm ID: $reminderId - Tiêu đề: $title")

        if (reminderId != -1) {
            // 1. Hiển thị thông báo (Notification) cho người dùng
            showNotification(context, reminderId, title, note)

            // 2. 🔥 TIẾN HÀNH RE-SCHEDULE CHO CHU KỲ TIẾP THEO NẾU CÓ CẤU HÌNH LẶP
            if (repeat != ReminderRepeat.ONCE) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis() // ✅ Dùng thời gian hiện tại làm mốc nạp
                }

                repeat.addTo(calendar)

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "moneymate_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lời nhắc MoneyMate",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh hiển thị thông báo nhắc nhở ghi chép chi tiêu"
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

        val notificationTitle = title.ifBlank { "Nhắc nhở MoneyMate" }
        val notificationMessage = message.ifBlank { "Đừng quên nhập chi tiêu ngày hôm nay !" }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
