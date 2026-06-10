package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.model.UserPreferences

import com.example.moneymate.domain.repository.PreferencesRepository
import com.example.moneymate.domain.usecase.Preferences.GetPreferencesUseCase
import com.example.moneymate.domain.usecase.Preferences.UpdateLanguageUseCase
import com.example.moneymate.domain.usecase.Preferences.UpdateThemeUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CustomizationUiState {
    object Loading : CustomizationUiState
    data class Success(val preferences: UserPreferences) : CustomizationUiState
    data class Error(val message: String) : CustomizationUiState
}

sealed interface CustomizationEvent {
    data class ChangeLanguage(val langCode: String) : CustomizationEvent
    data class ChangeTheme(val themeMode: String) : CustomizationEvent
    object RefreshSync : CustomizationEvent
}

@HiltViewModel
class CustomizationViewModel @Inject constructor(
    getPreferencesUseCase: GetPreferencesUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val repository: PreferencesRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: "guest_user"

    val uiState: StateFlow<CustomizationUiState> = getPreferencesUseCase(userId)
        .map { preferences -> CustomizationUiState.Success(preferences) as CustomizationUiState }
        .catch { emit(CustomizationUiState.Error(it.localizedMessage ?: "Đã xảy ra lỗi")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CustomizationUiState.Loading
        )

    init {
        // Đồng bộ hóa dữ liệu từ đám mây Firestore về máy ngay khi vào màn hình cấu hình
        viewModelScope.launch {
            try {
                repository.syncRemoteWithLocal(userId)
            } catch (e: Exception) {
                // Đảm bảo không crash nếu chạy offline không có mạng
            }
        }
    }

    fun onEvent(event: CustomizationEvent) {
        viewModelScope.launch {
            when (event) {
                is CustomizationEvent.ChangeLanguage -> updateLanguageUseCase(userId, event.langCode)
                is CustomizationEvent.ChangeTheme -> updateThemeUseCase(userId, event.themeMode)
                is CustomizationEvent.RefreshSync -> repository.syncRemoteWithLocal(userId)
            }
        }
    }
}