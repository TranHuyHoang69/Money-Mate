package com.example.moneymate.domain.usecase.Preferences

import com.example.moneymate.domain.model.UserPreferences
import com.example.moneymate.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferencesUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    operator fun invoke(userId: String): Flow<UserPreferences> =
        repository.getUserPreferences(userId)
}