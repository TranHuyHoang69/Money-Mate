package com.example.moneymate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.repository.ReminderRepository
import com.example.moneymate.util.AlarmScheduler // 👈 Thêm import này để quản lý Alarm ngầm
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
): ViewModel(){

    private val _currentUserId = MutableStateFlow(firebaseAuth.currentUser?.uid)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<ReminderEntity>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf<Result<List<ReminderEntity>>>(Result.Success(emptyList()))
            } else {
                repository.getAllRemindersLocal(userId)
            }
        }
        .map { result ->
            if (result is Result.Success) result.data else emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshUser() {
        _currentUserId.value = firebaseAuth.currentUser?.uid
    }

    fun createReminder(title: String, note: String, dateTime: Long, repeat: String){
        val userId = firebaseAuth.currentUser?.uid ?: "guest_user"

        viewModelScope.launch {
            val entity = ReminderEntity(
                userId = userId,
                title = title,
                note = note,
                reminderDateTime = dateTime,
                repeatInterval = repeat,
                isActive = true
            )
            repository.insertReminder(entity)
            // Lưu ý: Nếu Repository của bạn chưa tự động gọi AlarmScheduler.scheduleAlarm ngầm bên trong,
            // bạn có thể lấy kết quả trả về (ID) của bản ghi vừa tạo để kích hoạt đặt lịch tại đây.
        }
    }

    fun toggleReminder(reminder: ReminderEntity, isActive : Boolean){
        viewModelScope.launch {
            repository.updateReminderStatus(reminder.id.toLong(), isActive)

            // Xử lý bật/tắt báo thức hệ thống tương ứng với trạng thái Switch thay đổi
            val updatedReminder = reminder.copy(isActive = isActive)
            if (isActive) {
                AlarmScheduler.scheduleAlarm(context, updatedReminder)
            } else {
                AlarmScheduler.cancelAlarm(context, updatedReminder)
            }
        }
    }

    // ==================== 🛠️ THÊM CÁC CHỨC NĂNG MỚI TẠI ĐÂY ====================

    /**
     * Xóa nhắc nhở khỏi database đồng thời hủy báo thức trên hệ thống Android
     */
    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // 1. Hủy lịch hẹn giờ chạy ngầm của Android trước
            AlarmScheduler.cancelAlarm(context, reminder)

            // 2. Tiến hành xóa bản ghi dưới Database cục bộ (Room)
            // ⚠️ Lưu ý: Hãy đảm bảo Interface Repository của bạn đã viết hàm deleteReminder(reminder) này rồi nhé
            repository.deleteReminder(reminder)
        }
    }

    /**
     * Cập nhật toàn bộ thông tin của một lời nhắc hiện có
     */
    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // 1. Cập nhật vào cơ sở dữ liệu
            repository.updateReminder(reminder)

            // 2. Đồng bộ lại lịch hẹn giờ hệ thống: Nếu đang bật (Active) thì cập nhật giờ mới, ngược lại thì hủy lịch cũ đi
            if (reminder.isActive) {
                AlarmScheduler.scheduleAlarm(context, reminder)
            } else {
                AlarmScheduler.cancelAlarm(context, reminder)
            }
        }
    }
    fun getReminderById(id: Int): Flow<ReminderEntity?> {
        // Lưu ý: Đảm bảo Repository của bạn đã có hàm getReminderByIdLocal(id)
        return repository.getAllRemindersLocal(firebaseAuth.currentUser?.uid ?: "")
            .map { result ->
                if (result is Result.Success) {
                    result.data.find { it.id == id }
                } else null
            }
    }
}