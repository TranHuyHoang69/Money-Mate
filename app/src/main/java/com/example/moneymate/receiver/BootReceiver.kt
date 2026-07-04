package com.example.moneymate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.moneymate.data.local.AppDatabase
import com.example.moneymate.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Nhận tín hiệu hệ thống: $action")

        if (action !in SUPPORTED_BOOT_ACTIONS) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val activeReminders = database.reminderDao().getAllActiveReminders()

                Log.d(TAG, "Tìm thấy ${activeReminders.size} lời nhắc đang bật.")
                activeReminders.forEach { reminder ->
                    AlarmScheduler.scheduleAlarm(context, reminder)
                    Log.d(TAG, "Đã nạp lại lời nhắc: ${reminder.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Không thể khôi phục lịch nhắc sau khi khởi động lại", e)
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
        val SUPPORTED_BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.example.moneymate.TEST_BOOT"
        )
    }
}
