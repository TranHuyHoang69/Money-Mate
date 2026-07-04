package com.example.moneymate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.moneymate.util.PinCredential
import com.example.moneymate.util.PinCredentialHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

class SecurityLocalDataSource @Inject constructor(private val context: Context) {

    companion object {
        private val LEGACY_PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        private val PIN_VERIFIER_KEY = stringPreferencesKey("pin_verifier")
        private val PIN_KDF_VERSION_KEY = intPreferencesKey("pin_kdf_version")
        private val PIN_ITERATIONS_KEY = intPreferencesKey("pin_iterations")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    val hasPinFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        hasCurrentCredential(prefs) || !prefs[LEGACY_PIN_HASH_KEY].isNullOrBlank()
    }
    val biometricFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }

    suspend fun savePin(pin: String) {
        val credential = PinCredentialHasher.create(pin)
        context.dataStore.edit { prefs ->
            prefs[PIN_SALT_KEY] = credential.saltBase64
            prefs[PIN_VERIFIER_KEY] = credential.verifierBase64
            prefs[PIN_KDF_VERSION_KEY] = credential.version
            prefs[PIN_ITERATIONS_KEY] = credential.iterations
            prefs.remove(LEGACY_PIN_HASH_KEY)
        }
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        val prefs = context.dataStore.data.first()
        val credential = currentCredentialOrNull(prefs)
        if (credential != null) {
            return if (PinCredentialHasher.verify(pin, credential)) {
                PinVerificationResult.Success
            } else {
                PinVerificationResult.Failed
            }
        }

        val legacyHash = prefs[LEGACY_PIN_HASH_KEY]
        if (!legacyHash.isNullOrBlank()) {
            return if (PinCredentialHasher.verifyLegacySha256(pin, legacyHash)) {
                savePin(pin)
                PinVerificationResult.LegacyMigrated
            } else {
                PinVerificationResult.Failed
            }
        }

        return if (hasPartialCurrentCredential(prefs)) {
            PinVerificationResult.Corrupt
        } else {
            PinVerificationResult.Missing
        }
    }

    suspend fun saveBiometricStatus(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun clearSecurityData() {
        context.dataStore.edit { prefs ->
            prefs.remove(LEGACY_PIN_HASH_KEY)
            prefs.remove(PIN_SALT_KEY)
            prefs.remove(PIN_VERIFIER_KEY)
            prefs.remove(PIN_KDF_VERSION_KEY)
            prefs.remove(PIN_ITERATIONS_KEY)
            prefs[BIOMETRIC_ENABLED_KEY] = false
        }
    }

    private fun currentCredentialOrNull(prefs: Preferences): PinCredential? {
        val salt = prefs[PIN_SALT_KEY]
        val verifier = prefs[PIN_VERIFIER_KEY]
        val version = prefs[PIN_KDF_VERSION_KEY]
        val iterations = prefs[PIN_ITERATIONS_KEY]

        return if (
            salt.isNullOrBlank() ||
            verifier.isNullOrBlank() ||
            version == null ||
            iterations == null
        ) {
            null
        } else {
            PinCredential(
                saltBase64 = salt,
                verifierBase64 = verifier,
                iterations = iterations,
                version = version
            )
        }
    }

    private fun hasCurrentCredential(prefs: Preferences): Boolean = currentCredentialOrNull(prefs) != null

    private fun hasPartialCurrentCredential(prefs: Preferences): Boolean {
        return prefs[PIN_SALT_KEY] != null ||
            prefs[PIN_VERIFIER_KEY] != null ||
            prefs[PIN_KDF_VERSION_KEY] != null ||
            prefs[PIN_ITERATIONS_KEY] != null
    }
}

enum class PinVerificationResult {
    Success,
    LegacyMigrated,
    Failed,
    Missing,
    Corrupt
}
