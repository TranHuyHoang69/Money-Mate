package com.example.moneymate.data.repository

import com.example.moneymate.data.local.PinVerificationResult
import com.example.moneymate.data.local.SecurityLocalDataSource
import com.example.moneymate.data.remote.SecurityRemoteDataSource
import com.example.moneymate.domain.model.SecurityState
import com.example.moneymate.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SecurityRepositoryImpl @Inject constructor(
    private val localDataSource: SecurityLocalDataSource,
    private val remoteDataSource: SecurityRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : SecurityRepository {

    override fun observeSecurityState(): Flow<SecurityState> {
        return combine(
            localDataSource.hasPinFlow,
            localDataSource.biometricFlow
        ) { hasPin, biometric ->
            SecurityState(
                hasPin = hasPin,
                biometricEnabled = biometric
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun createPin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            validatePin(pin)
            localDataSource.savePin(pin)
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            validatePin(oldPin)
            validatePin(newPin)
            when (localDataSource.verifyPin(oldPin)) {
                PinVerificationResult.Success,
                PinVerificationResult.LegacyMigrated -> localDataSource.savePin(newPin)
                PinVerificationResult.Missing -> throw Exception("Chua thiet lap ma PIN ung dung")
                PinVerificationResult.Corrupt -> throw Exception("Du lieu PIN khong hop le, vui long thiet lap lai PIN")
                PinVerificationResult.Failed -> throw Exception("Ma PIN hien tai khong chinh xac")
            }
        }
    }

    override suspend fun deletePin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            validatePin(pin)
            when (localDataSource.verifyPin(pin)) {
                PinVerificationResult.Success,
                PinVerificationResult.LegacyMigrated -> {
                    localDataSource.clearSecurityData()
                    remoteDataSource.deleteRemoteSettings()
                }
                PinVerificationResult.Missing -> throw Exception("Chua thiet lap ma PIN ung dung")
                PinVerificationResult.Corrupt -> throw Exception("Du lieu PIN khong hop le, vui long thiet lap lai PIN")
                PinVerificationResult.Failed -> throw Exception("Ma PIN xac thuc khong chinh xac")
            }
        }
    }

    override suspend fun updateBiometric(enabled: Boolean): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            localDataSource.saveBiometricStatus(enabled)
            remoteDataSource.updateRemoteSettings(enabled)
        }
    }

    override suspend fun verifyPin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            validatePin(pin)
            when (localDataSource.verifyPin(pin)) {
                PinVerificationResult.Success,
                PinVerificationResult.LegacyMigrated -> Unit
                PinVerificationResult.Missing -> throw Exception("Chua thiet lap ma PIN ung dung")
                PinVerificationResult.Corrupt -> throw Exception("Du lieu PIN khong hop le, vui long thiet lap lai PIN")
                PinVerificationResult.Failed -> throw Exception("Ma PIN khong chinh xac. Vui long thu lai!")
            }
        }
    }

    private fun validatePin(pin: String) {
        if (pin.length != 4 || pin.any { !it.isDigit() }) {
            throw IllegalArgumentException("Ma PIN phai gom 4 chu so")
        }
    }
}
