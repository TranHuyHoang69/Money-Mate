package com.example.moneymate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

class SecurityLocalDataSource @Inject constructor(private val context: Context) {

    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { it[PIN_HASH_KEY] }
    val biometricFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }

    suspend fun savePin(hash: String) {
        context.dataStore.edit { prefs -> prefs[PIN_HASH_KEY] = hash }
    }

    suspend fun saveBiometricStatus(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun clearSecurityData() {
        context.dataStore.edit { prefs ->
            prefs.remove(PIN_HASH_KEY)
            prefs[BIOMETRIC_ENABLED_KEY] = false
        }
    }

    fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}