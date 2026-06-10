package com.example.moneymate.domain.usecase.Preferences

import com.example.moneymate.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateLanguageUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    suspend operator fun invoke(userId: String, language: String) =
        repository.updateLanguage(userId, language)
}