package com.example.moneymate.data.repository

import com.example.moneymate.data.local.SecurityLocalDataSource
import com.example.moneymate.data.remote.SecurityRemoteDataSource
import com.example.moneymate.domain.model.SecurityState
import com.example.moneymate.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first // Import thêm để lấy giá trị Flow một lần duy nhất
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
            localDataSource.pinHashFlow,
            localDataSource.biometricFlow
        ) { hash, biometric ->
            SecurityState(
                hasPin = !hash.isNullOrBlank(),
                pinHash = hash,
                biometricEnabled = biometric
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun createPin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val hash = localDataSource.hashPin(pin)
            localDataSource.savePin(hash)
            remoteDataSource.updateRemoteSettings(hash, false)
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            // 1. Lấy mã băm của PIN hiện tại đang được lưu trong máy
            val currentStoredHash = localDataSource.pinHashFlow.first()

            // 2. Băm mã PIN cũ do người dùng nhập vào để so sánh
            val oldHashInput = localDataSource.hashPin(oldPin)

            // 3. Kiểm tra xem mã cũ nhập vào có khớp với mã đang dùng không
            if (currentStoredHash != oldHashInput) {
                throw Exception("Mã PIN hiện tại không chính xác")
            }

            // 4. Nếu khớp thì mới tiến hành lưu mã PIN mới
            val newHash = localDataSource.hashPin(newPin)
            localDataSource.savePin(newHash)
            remoteDataSource.updateRemoteSettings(newHash, false)
        }
    }

    override suspend fun deletePin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            // 1. Lấy mã băm hiện tại trong máy
            val currentStoredHash = localDataSource.pinHashFlow.first()

            // 2. Băm mã PIN nhập vào để xác nhận xóa
            val hashInput = localDataSource.hashPin(pin)

            // 3. Nếu nhập sai mã PIN thì không cho gỡ bảo mật
            if (currentStoredHash != hashInput) {
                throw Exception("Mã PIN xác thực không chính xác")
            }

            // 4. Đúng mã PIN mới tiến hành xóa sạch dữ liệu bảo mật
            localDataSource.clearSecurityData()
            remoteDataSource.deleteRemoteSettings()
        }
    }

    override suspend fun updateBiometric(enabled: Boolean): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            localDataSource.saveBiometricStatus(enabled)
            remoteDataSource.updateRemoteSettings(null, enabled)
        }
    }

    // 🌟 ĐÃ HIỆN THỰC HÀM XÁC THỰC MÃ PIN CHO MÀN HÌNH KHÓA AUTHLOCK
    override suspend fun verifyPin(pin: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            // 1. Lấy mã băm hiện tại đang lưu trong DataStore/SharedPreferences của Local
            val currentStoredHash = localDataSource.pinHashFlow.first()

            // 2. Nếu trong máy chưa từng cài mã PIN nào mà lại gọi verify thì báo lỗi
            if (currentStoredHash.isNullOrBlank()) {
                throw Exception("Chưa thiết lập mã PIN ứng dụng")
            }

            // 3. Băm mã PIN do người dùng vừa nhập ngoài màn hình AuthLock
            val inputHash = localDataSource.hashPin(pin)

            // 4. So sánh hai chuỗi băm. Nếu không trùng khớp, throw Exception để luồng .onFailure nhận diện
            if (currentStoredHash != inputHash) {
                throw Exception("Mã PIN không chính xác. Vui lòng thử lại!")
            }

            // Nếu trùng khớp, khối runCatching sẽ tự trả về Result.success(Unit)
        }
    }
}