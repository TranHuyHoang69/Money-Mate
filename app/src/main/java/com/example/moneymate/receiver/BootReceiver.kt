package com.example.moneymate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.moneymate.data.local.AppDatabase
import com.example.moneymate.data.local.ReminderEntity // 🌟 ĐÃ THÊM: Import trực tiếp Entity để ép kiểu dữ liệu tường minh
import com.example.moneymate.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Nhận được tín hiệu hệ thống: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "com.example.moneymate.TEST_BOOT"
            ) {

            Log.d("BootReceiver", "Thiết bị vừa tái khởi động xong! Tiến hành nạp lại danh sách Reminder...")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 🛠️ SỬA TẠI ĐÂY: Thay 'getDatabase' bằng phương thức Singleton thực tế của bạn (ví dụ: getInstance)
                    // Nếu AppDatabase của bạn dùng tên khác, hãy đổi tên tương ứng ở đây.
                    val database = AppDatabase.getInstance(context)

                    val activeReminders: List<ReminderEntity> = database.reminderDao().getAllActiveReminders()

                    Log.d("BootReceiver", "Tìm thấy ${activeReminders.size} lời nhắc đang ở trạng thái kích hoạt.")

                    // 🛠️ SỬA TẠI ĐÂY: Chỉ định rõ kiểu dữ liệu (reminder: ReminderEntity) để trình biên dịch không bị nhận diện sai
                    activeReminders.forEach { reminder: ReminderEntity ->
                        AlarmScheduler.scheduleAlarm(context, reminder)
                        Log.d("BootReceiver", "Đã nạp lại thành công cho: ${reminder.title}")
                    }
                    Log.d("BootReceiver", "Hoàn tất quá trình khôi phục toàn bộ lời nhắc!")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Lỗi nghiêm trọng xảy ra khi khôi phục Alarm: ${e.localizedMessage}")
                    e.printStackTrace()
                }
            }
        }
    }
}