package com.example.moneymate.domain.repository

import com.example.moneymate.domain.Result
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    // Không dùng ReminderEntity ở đây, trả về Result bọc dữ liệu cho chuẩn quy trình app
    fun getAllRemindersLocal(userId: String): Flow<Result<List<com.example.moneymate.data.local.ReminderEntity>>>

    // Thay vì bắt Domain nhận Context và Entity, ta truyền Entity thô qua (hoặc Model Domain nếu có)
    suspend fun insertReminder(reminder: com.example.moneymate.data.local.ReminderEntity): Result<Unit>

    suspend fun updateReminderStatus(
        reminderId: Long,
        isActive: Boolean
    ): Result<Unit>

    suspend fun deleteReminder(reminder: com.example.moneymate.data.local.ReminderEntity): Result<Unit>

    // ✅ THÊM DÒNG NÀY: Cập nhật toàn bộ thông tin (Tiêu đề, ghi chú, thời gian...) khi người dùng sửa lời nhắc
    suspend fun updateReminder(reminder: com.example.moneymate.data.local.ReminderEntity): Result<Unit>
}