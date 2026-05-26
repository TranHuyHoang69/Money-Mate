package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    // Đồng bộ kiểu dữ liệu sang Long để khớp với ID từ tầng Repository truyền xuống
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY reminderDateTime ASC")
    fun getRemindersByUserId(userId: String): Flow<List<ReminderEntity>>

    // 🟢 BỔ SUNG HÀM NÀY: Giúp Repository tìm được thực thể Reminder hiện tại để cập nhật trạng thái switch toggle
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?
}