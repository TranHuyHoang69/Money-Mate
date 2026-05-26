package com.example.moneymate.data.repository

import android.content.Context
import android.util.Log
import com.example.moneymate.data.local.ReminderDao
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.repository.ReminderRepository
import com.example.moneymate.util.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val firestoreDataSource: FirestoreDataSource,
    @ApplicationContext private val context: Context // 🟢 GIẢI PHÁP: Inject Context hệ thống vào đây, các hàm dưới không cần nhận Context nữa
): ReminderRepository {

    // 🟢 Đồng bộ hàm lấy danh sách bọc Result theo cấu trúc sạch của luồng
    override fun getAllRemindersLocal(userId: String): Flow<Result<List<ReminderEntity>>> {
        return reminderDao.getRemindersByUserId(userId)
            .map { localList ->
                Result.Success(localList) as Result<List<ReminderEntity>>
            }
            .flowOn(Dispatchers.IO)
            .catch { emit(Result.Error(it.message ?: "Lỗi truy vấn lời nhắc local")) }
    }

    // 🟢 Bỏ tham số Context ở hàm, dùng trực tiếp biến `context` toàn cục ở trên
    override suspend fun insertReminder(reminder: ReminderEntity): Result<Unit> {
        return try {
            val generatedId = reminderDao.insertReminder(reminder).toInt()
            val finalReminder = reminder.copy(id = generatedId)

            if (finalReminder.isActive) {
                AlarmScheduler.scheduleAlarm(context, finalReminder)
            }
            firestoreDataSource.saveReminderToRemote(finalReminder)

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MoneyMateError", "Lỗi tạo lời nhắc rồi bạn ơi: ${e.message}", e)
            Result.Error(e.message ?: "Lỗi khi thêm lời nhắc")
        }
    }

    // 🟢 Bỏ tham số Context, thay tham số truyền vào sang ID và Trạng thái cho đúng Interface
    override suspend fun updateReminderStatus(
        reminderId: Long,
        isActive: Boolean
    ): Result<Unit> {
        return try {
            // Lấy thực thể cũ từ Room trước khi cập nhật (hoặc xử lý trực tiếp tùy logic của bạn)
            val currentReminder = reminderDao.getReminderById(reminderId)
                ?: return Result.Error("Không tìm thấy lời nhắc cần cập nhật")

            val updateReminder = currentReminder.copy(isActive = isActive)
            reminderDao.updateReminder(updateReminder)

            if (isActive) {
                AlarmScheduler.scheduleAlarm(context, updateReminder)
            } else {
                AlarmScheduler.cancelAlarm(context, updateReminder)
            }

            firestoreDataSource.saveReminderToRemote(updateReminder)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật lời nhắc")
        }
    }

    // 🟢 Bỏ tham số Context, dùng biến `context` tiêm từ Hilt
    override suspend fun deleteReminder(reminder: ReminderEntity): Result<Unit> {
        return try {
            AlarmScheduler.cancelAlarm(context, reminder)

            reminderDao.deleteReminderById(reminder.id)

            firestoreDataSource.deleteReminderFromRemote(reminder.id)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xoá lời nhắc")
        }
    }
    override suspend fun updateReminder(reminder: ReminderEntity): Result<Unit> {
        return try {
            reminderDao.updateReminder(reminder) // Hoặc tên hàm update trong Room DAO của bạn
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật lời nhắc")
        }
    }
}