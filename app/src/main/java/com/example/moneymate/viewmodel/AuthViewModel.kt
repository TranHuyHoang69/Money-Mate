package com.example.moneymate.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String = "",
    val isLoggedIn: Boolean = false,
    val isRegisterSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val credentialManager: CredentialManager
): ViewModel(){
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch(Dispatchers.IO) {
            // Flow từ Firebase đã chạy trên worker thread của nó,
            // ta chỉ cần đảm bảo việc thu thập không chặn Main Thread.
            authRepository.observeAuthState().collectLatest { user ->
                withContext(Dispatchers.Main){
                    _uiState.update { it.copy(
                        user = user,
                        isLoggedIn = user != null,
                        isLoading = false
                    )}
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = "") }

            when (val result = authRepository.loginWithEmail(email, password)) {
                is Result.Success -> {
                    // Không cần gán isLoggedIn ở đây, observeAuthState sẽ tự cập nhật
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun register(email: String, userName: String, password: String){
        viewModelScope.launch(Dispatchers.IO) { // Chuyển sang IO
            _uiState.update { it.copy(isLoading = true, error = "") }

            when(val result = authRepository.registerWithEmail(email, userName, password)){
                is Result.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        user = result.data,
                        isRegisterSuccess = true
                    )}
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.logout() // Gọi hàm logout từ FirebaseRepository

                // Reset toàn bộ UI State về mặc định ngay lập tức
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    user = null
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Lỗi khi đăng xuất: ${e.message}") }
            }
        }
    }

    // Trong AuthViewModel.kt
    fun signInWithGoogle(context: Context) {
        // LƯU Ý: CredentialManager.getCredential() VẪN cần Activity Context
        // để hiển thị hộp thoại chọn tài khoản (Bottom Sheet).
        // Nhưng việc khởi tạo instance Manager đã được Hilt lo.

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("your_server_client_id")
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Sử dụng instance đã được inject
                val result = credentialManager.getCredential(context, request)

                // Xử lý tiếp token...
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                authRepository.signInWithGoogle(googleIdTokenCredential.idToken)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Lỗi đăng nhập") }
            }
        }
    }
}