package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.model.SecurityState
import com.example.moneymate.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val isUnlocked: Boolean = false // Chỉ dùng cho màn AuthLockScreen
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val repository: SecurityRepository
) : ViewModel() {

    // 🌟 1. LUỒNG ĐỌC DỮ LIỆU BẢO MẬT TỪ DATABASE (TÁCH BIỆT HOÀN TOÀN)
    // Luồng này luôn phản ánh trạng thái thực tế trong máy (Đang bật PIN hay tắt PIN)
    val securityState: StateFlow<SecurityState> = repository.observeSecurityState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Kích hoạt đọc ngay lập tức khi ViewModel được tạo
            initialValue = SecurityState()
        )

    // 🌟 2. LUỒNG TRẠNG THÁI HÀNH ĐỘNG BẤM NÚT (TÁCH BIỆT HOÀN TOÀN)
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    fun createPin(pin: String) = launchAction { repository.createPin(pin) }
    fun changePin(old: String, new: String) = launchAction { repository.changePin(old, new) }
    fun deletePin(pin: String) = launchAction { repository.deletePin(pin) }
    fun toggleBiometric(enabled: Boolean) = launchAction { repository.updateBiometric(enabled) }

    // Hàm xác thực mã PIN cho màn hình AuthLock
    fun verifyPin(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isUnlocked = false) }
            repository.verifyPin(pin)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.localizedMessage, isUnlocked = false) }
                }
        }
    }

    // Làm sạch trạng thái hành động
    fun resetState() {
        _uiState.update { SecurityUiState() } // Reset hoàn toàn về đối tượng trống sạch sẽ
    }

    private fun launchAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            action().onSuccess {
                _uiState.update { it.copy(isLoading = false, success = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}