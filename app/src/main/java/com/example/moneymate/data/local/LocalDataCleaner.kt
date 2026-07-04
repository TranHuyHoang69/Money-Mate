package com.example.moneymate.data.local

import android.content.Context
import android.util.Log
import com.example.moneymate.util.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataCleaner @Inject constructor(
    private val database: AppDatabase,
    private val securityLocalDataSource: SecurityLocalDataSource,
    @ApplicationContext private val context: Context
) {
    suspend fun clearUserScopedData() {
        try {
            database.reminderDao()
                .getAllActiveReminders()
                .forEach { reminder ->
                    AlarmScheduler.cancelAlarm(context, reminder.id)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not cancel reminder alarms before clearing local data", e)
        }

        database.clearAllTables()
        securityLocalDataSource.clearSecurityData()
        Log.d(TAG, "User-scoped local data cleared")
    }

    private companion object {
        const val TAG = "LocalDataCleaner"
    }
}
