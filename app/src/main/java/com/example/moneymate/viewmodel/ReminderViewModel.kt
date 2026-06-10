package com.example.moneymate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.repository.ReminderRepository
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
            // ✅ ĐÃ TỐI ƯU: repository.insertReminder ngầm định đã gọi AlarmScheduler.scheduleAlarm rồi
            repository.insertReminder(entity)
        }
    }

    fun toggleReminder(reminder: ReminderEntity, isActive : Boolean){
        viewModelScope.launch {
            // ✅ ĐÃ TỐI ƯU: Tầng repository.updateReminderStatus đã tự động phân phối
            // AlarmScheduler.scheduleAlarm / cancelAlarm dựa theo biến isActive.
            // Việc gỡ bỏ các dòng gọi Alarm trực tiếp tại đây giúp sửa lỗi truyền sai object cực kỳ sạch sẽ.
            repository.updateReminderStatus(reminder.id.toLong(), isActive)
        }
    }

    /**
     * Xóa nhắc nhở khỏi database đồng thời hủy báo thức trên hệ thống Android
     */
    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // ✅ ĐÃ TỐI ƯU: Tầng repository.deleteReminder đã đảm nhận hủy báo thức ngầm bằng ID thích ứng.
            repository.deleteReminder(reminder)
        }
    }

    /**
     * Cập nhật toàn bộ thông tin của một lời nhắc hiện có
     */
    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // ✅ ĐÃ TỐI ƯU: Tầng repository.updateReminder đã lo toàn bộ việc cập nhật DB local, remote
            // cũng như đồng bộ tái đặt lịch mốc thời gian mới trên AlarmManager.
            repository.updateReminder(reminder)
        }
    }

    fun getReminderById(id: Int): Flow<ReminderEntity?> {
        return repository.getAllRemindersLocal(firebaseAuth.currentUser?.uid ?: "")
            .map { result ->
                if (result is Result.Success) {
                    result.data.find { it.id == id }
                } else null
            }
    }
}