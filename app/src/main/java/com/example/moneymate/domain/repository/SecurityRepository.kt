package com.example.moneymate.domain.repository

import com.example.moneymate.domain.model.SecurityState
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun observeSecurityState(): Flow<SecurityState>
    suspend fun createPin(pin: String): Result<Unit>
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>
    suspend fun deletePin(pin: String): Result<Unit>
    suspend fun updateBiometric(enabled: Boolean): Result<Unit>
    suspend fun verifyPin(pin: String): Result<Unit>
}