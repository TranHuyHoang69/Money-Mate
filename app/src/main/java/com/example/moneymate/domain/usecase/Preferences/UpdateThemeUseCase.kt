package com.example.moneymate.domain.usecase.Preferences

import com.example.moneymate.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateThemeUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    suspend operator fun invoke(userId: String, themeMode: String) =
        repository.updateThemeMode(userId, themeMode)
}