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
    fun observeAuthState(): Flow<User?>
}