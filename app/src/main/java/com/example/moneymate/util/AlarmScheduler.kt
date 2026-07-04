package com.example.moneymate.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.receiver.ReminderReceiver
import java.util.Calendar
import java.util.Date

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, reminder: ReminderEntity) {
        if (!reminder.isActive) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Kiểm tra quyền đặt Alarm chính xác trên Android 12 (API 31) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "Chưa được cấp quyền SCHEDULE_EXACT_ALARM. Bỏ qua đặt lịch.")
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("USER_ID", reminder.userId)
            putExtra("TITLE", reminder.title)
            putExtra("NOTE", reminder.note)
            putExtra("REPEAT_INTERVAL", reminder.repeatInterval)
        }

        // Dùng FLAG_UPDATE_CURRENT để ghi đè dữ liệu mới nếu trùng ID
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var triggerTime = reminder.reminderDateTime
        val now = System.currentTimeMillis()
        val repeat = ReminderRepeat.fromStored(reminder.repeatInterval)

        // 🔥 XỬ LÝ THỜI GIAN QUÁ KHỨ: Tịnh tiến đến chu kỳ kế tiếp trong tương lai
        if (triggerTime <= now && repeat != ReminderRepeat.ONCE) {
            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }

            while (calendar.timeInMillis <= now) {
                if (!repeat.addTo(calendar)) break
            }
            triggerTime = calendar.timeInMillis
            Log.d("AlarmScheduler", "Đã điều chỉnh mốc thời gian quá khứ của [${reminder.title}] sang tương lai: ${Date(triggerTime)}")
        }

        // Đối với lời nhắc một lần mà đã qua giờ thì hủy/không đặt lịch
        if (triggerTime <= now) {
            Log.w("AlarmScheduler", "Bỏ qua lời nhắc một lần nằm ở quá khứ: ${reminder.title}")
            return
        }

        // Đặt lịch xuyên Doze Mode dựa trên phiên bản Android SDK
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
        Log.d("AlarmScheduler", "Đã đặt lịch Alarm thành công cho [${reminder.title}] vào lúc: ${Date(triggerTime)}")
    }

    fun cancelAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Đã hủy Alarm thành công cho ID: $reminderId")
        }
    }
}
