package com.example.moneymate.domain.repository

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository{
    val currentUser: User?
    val isLoggedIn: Boolean

    suspend fun loginWithEmail(email: String, password: String): com.example.moneymate.domain.Result<User>
    suspend fun registerWithEmail(email: String, userName: String, password: String): com.example.moneymate.domain.Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>
    suspend fun reauthenticateWithPassword(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    fun observeAuthState(): Flow<User?>
    suspend fun deleteAccount(): Result<Unit>

}
