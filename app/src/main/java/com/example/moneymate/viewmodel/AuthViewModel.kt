@file:Suppress("DEPRECATION")

package com.example.moneymate.viewmodel

import android.content.Context
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.LocalDataCleaner
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val localDataCleaner: LocalDataCleaner
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    private var lastObservedUserId: String? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.observeAuthState().collectLatest { user ->
                val previousUserId = lastObservedUserId
                val newUserId = user?.uid

                if (previousUserId != null && previousUserId != newUserId) {
                    localDataCleaner.clearUserScopedData()
                }
                lastObservedUserId = newUserId

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoggedIn = user != null,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedEmail = email.trim()
            val validationError = validateLoginInput(normalizedEmail, password)
            if (validationError != null) {
                _uiState.update { it.copy(isLoading = false, error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = "") }

            when (val result = authRepository.loginWithEmail(normalizedEmail, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }
    }

    fun register(email: String, userName: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedEmail = email.trim()
            val normalizedUserName = userName.trim()
            val validationError = validateRegisterInput(
                email = normalizedEmail,
                userName = normalizedUserName,
                password = password
            )
            if (validationError != null) {
                _uiState.update { it.copy(isLoading = false, error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = "") }

            when (val result = authRepository.registerWithEmail(
                normalizedEmail,
                normalizedUserName,
                password
            )) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = result.data,
                        isRegisterSuccess = true
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }
    }

    fun logout(context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.logout()
                localDataCleaner.clearUserScopedData()

                if (context != null) {
                    CredentialManager.create(context).clearCredentialState(
                        ClearCredentialStateRequest()
                    )
                    GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .signOut()
                        .await()
                }

                kotlinx.coroutines.delay(500)

                withContext(Dispatchers.Main) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        user = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error logging out ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Không thể đăng xuất") }
            }
        }
    }

    fun signInWithFirebase(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = "") }

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    android.util.Log.d("GoogleAuth", "Firebase sign-in successful")
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    android.util.Log.e("GoogleAuth", "Firebase error: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setGoogleLoginLoading() {
        _uiState.update { it.copy(isLoading = true, error = "") }
    }

    fun onGoogleLoginError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    private fun validateLoginInput(email: String, password: String): String? {
        return when {
            email.isBlank() -> "Vui lòng nhập email"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không đúng định dạng"
            password.isBlank() -> "Vui lòng nhập mật khẩu"
            else -> null
        }
    }

    private fun validateRegisterInput(
        email: String,
        userName: String,
        password: String
    ): String? {
        return when {
            userName.isBlank() -> "Vui lòng nhập tên người dùng"
            email.isBlank() -> "Vui lòng nhập email"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không đúng định dạng"
            password.length < MIN_PASSWORD_LENGTH -> {
                "Mật khẩu phải có ít nhất $MIN_PASSWORD_LENGTH ký tự"
            }
            else -> null
        }
    }

    fun deleteUserAccount(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = "") }

            val result = authRepository.deleteAccount()
            handleDeleteAccountResult(result, onSuccess, onFailure)
        }
    }

    fun reauthenticateWithPasswordAndDelete(
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (password.isBlank()) {
                val exception = Exception("Vui lòng nhập mật khẩu")
                _uiState.update { it.copy(isLoading = false, error = exception.message.orEmpty()) }
                withContext(Dispatchers.Main) { onFailure(exception) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = "") }
            when (val reauthResult = authRepository.reauthenticateWithPassword(password)) {
                is Result.Success -> {
                    handleDeleteAccountResult(authRepository.deleteAccount(), onSuccess, onFailure)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = reauthResult.message) }
                    withContext(Dispatchers.Main) { onFailure(Exception(reauthResult.message)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun reauthenticateWithGoogleAndDelete(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = "") }
            when (val reauthResult = authRepository.reauthenticateWithGoogle(idToken)) {
                is Result.Success -> {
                    handleDeleteAccountResult(authRepository.deleteAccount(), onSuccess, onFailure)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = reauthResult.message) }
                    withContext(Dispatchers.Main) { onFailure(Exception(reauthResult.message)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private suspend fun handleDeleteAccountResult(
        result: Result<Unit>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        when (result) {
            is Result.Success -> {
                localDataCleaner.clearUserScopedData()
                withContext(Dispatchers.Main) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        user = null
                    )
                    onSuccess()
                }
            }
            is Result.Error -> {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    onFailure(Exception(result.message))
                }
            }
            Result.Loading -> _uiState.update { it.copy(isLoading = false) }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
