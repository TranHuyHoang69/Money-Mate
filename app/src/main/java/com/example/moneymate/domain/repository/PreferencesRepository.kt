package com.example.moneymate.domain.repository

import com.example.moneymate.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getUserPreferences(userId: String): Flow<UserPreferences>
    suspend fun updateLanguage(userId: String, language: String)
    suspend fun updateThemeMode(userId: String, themeMode: String)
    suspend fun syncRemoteWithLocal(userId: String)
}