package com.example.moneymate.data.repository

import com.example.moneymate.data.local.PreferencesLocalDataSource
import com.example.moneymate.data.remote.PreferencesRemoteDataSource
import com.example.moneymate.domain.model.UserPreferences
import com.example.moneymate.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val localDataSource: PreferencesLocalDataSource,
    private val remoteDataSource: PreferencesRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : PreferencesRepository {

    override fun getUserPreferences(userId: String): Flow<UserPreferences> =
        localDataSource.userPreferencesFlow

    override suspend fun updateLanguage(userId: String, language: String) = withContext(ioDispatcher) {
        localDataSource.updateLanguage(language)
        val currentLocal = localDataSource.userPreferencesFlow.first()
        try {
            remoteDataSource.saveRemotePreferences(userId, currentLocal)
        } catch (e: Exception) {
            // Đã lưu trữ thành công cục bộ, bỏ qua lỗi mạng khi đồng bộ
        }
    }

    override suspend fun updateThemeMode(userId: String, themeMode: String) = withContext(ioDispatcher) {
        localDataSource.updateThemeMode(themeMode)
        val currentLocal = localDataSource.userPreferencesFlow.first()
        try {
            remoteDataSource.saveRemotePreferences(userId, currentLocal)
        } catch (e: Exception) {
            // Đã lưu trữ thành công cục bộ, bỏ qua lỗi mạng khi đồng bộ
        }
    }

    override suspend fun syncRemoteWithLocal(userId: String) = withContext(ioDispatcher) {
        val remoteData = remoteDataSource.getRemotePreferences(userId)
        if (remoteData != null) {
            localDataSource.updateLanguage(remoteData.language)
            localDataSource.updateThemeMode(remoteData.themeMode)
        }
    }
}