package com.example.moneymate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.moneymate.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class PreferencesLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("language_key")
    private val themeKey = stringPreferencesKey("theme_key")

    val userPreferencesFlow: Flow<UserPreferences> = context.userPrefsDataStore.data.map { preferences ->
        UserPreferences(
            language = preferences[languageKey] ?: "vi",
            themeMode = preferences[themeKey] ?: "system"
        )
    }

    suspend fun updateLanguage(language: String) {
        context.userPrefsDataStore.edit { preferences ->
            preferences[languageKey] = language
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.userPrefsDataStore.edit { preferences ->
            preferences[themeKey] = themeMode
        }
    }
}
