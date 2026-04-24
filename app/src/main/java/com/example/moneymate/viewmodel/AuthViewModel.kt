package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.example.moneymate.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String = "",
    val isLoggedIn: Boolean = false,
    val isRegisterSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel(){
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        observeAuthState()
    }

    private fun observeAuthState(){
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoggedIn = user != null
                )
            }
        }
    }

    fun login(email: String, password: String){
        if (_uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = ""
            )
            when(val result = authRepository.loginWithEmail(email,password)){
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isLoggedIn = true
                    )
                }
                is Result.Error ->{
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun register(email: String,userName: String,password: String){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = ""
            )
            when(val result = authRepository.registerWithEmail(email,userName,password)){
                is Result.Success ->{
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isRegisterSuccess = true
                    )
                }
                is Result.Error ->{
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun logout(){
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}