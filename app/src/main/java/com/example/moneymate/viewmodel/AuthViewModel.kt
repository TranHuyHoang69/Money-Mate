package com.example.moneymate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.example.moneymate.domain.repository.ExpenseRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    private val expenseRepository: ExpenseRepository
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

    fun logout(context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.logout()
                expenseRepository.clearAllLocalData()

                if(context != null){
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("458037840211-8591f2k268qa9cfr5q0uefuss5tcd0qh.apps.googleusercontent.com")
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        android.util.Log.d("GoogleAuth","Google sign out completed")
                    }
                }

                // ✅ Delay để đủ thời gian clear data
                kotlinx.coroutines.delay(500)

                withContext(Dispatchers.Main){
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        user = null
                    )
                }
            }catch (e: Exception){
                android.util.Log.e("AuthViewModel", "Error logging out ${e.message}",e)
                _uiState.update { it.copy(isLoading = false, error = "Error logging out") }
            }
        }
    }

    // Trong AuthViewModel.kt
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                android.util.Log.d("GoogleAuth", "Starting Firebase GoogleSignIn...")

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("458037840211-8591f2k268qa9cfr5q0uefuss5tcd0qh.apps.googleusercontent.com")
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(context, gso)

                // Lấy last signed in account
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    android.util.Log.d("GoogleAuth", "Already signed in: ${account.email}")
                    val idToken = account.idToken
                    if (idToken != null) {
                        // Đã có token, gọi Firebase
                        val firebaseResult = withContext(Dispatchers.IO) {
                            authRepository.signInWithGoogle(idToken)
                        }

                        when (firebaseResult) {
                            is Result.Success -> {
                                android.util.Log.d("GoogleAuth", "Firebase sign-in successful")
                                _uiState.update { it.copy(isLoading = false) }
                            }
                            is Result.Error -> {
                                android.util.Log.e("GoogleAuth", "Firebase error: ${firebaseResult.message}")
                                _uiState.update { it.copy(isLoading = false, error = firebaseResult.message) }
                            }
                            else -> {
                                _uiState.update { it.copy(isLoading = false) }
                            }
                        }
                    } else {
                        android.util.Log.d("GoogleAuth", "No ID token, signing out and retrying...")
                        googleSignInClient.signOut()
                        _uiState.update { it.copy(isLoading = false, error = "Vui lòng thử lại") }
                    }
                } else {
                    android.util.Log.d("GoogleAuth", "No account signed in, show sign-in dialog")
                    _uiState.update { it.copy(isLoading = false, error = "Vui lòng ấn nút Google lần nữa để đăng nhập") }
                }

            } catch (e: ApiException) {
                android.util.Log.e("GoogleAuth", "ApiException: ${e.statusCode} - ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Google API Error: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("GoogleAuth", "Exception: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Lỗi đăng nhập Google")
                }
            }
        }
    }
    fun signInWithFirebase(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = "") }

            val firebaseResult = authRepository.signInWithGoogle(idToken)

            when (firebaseResult) {
                is Result.Success -> {
                    android.util.Log.d("GoogleAuth", "Firebase sign-in successful")
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    android.util.Log.e("GoogleAuth", "Firebase error: ${firebaseResult.message}")
                    _uiState.update { it.copy(isLoading = false, error = firebaseResult.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}