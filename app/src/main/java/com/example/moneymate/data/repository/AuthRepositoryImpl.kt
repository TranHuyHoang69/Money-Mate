package com.example.moneymate.data.repository

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.Lazy // THÊM IMPORT NÀY
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authLazy: Lazy<FirebaseAuth> // ĐỔI Ở ĐÂY: Sử dụng Lazy để tránh block Main Thread lúc app khởi động
): AuthRepository {

    // Tạo một thuộc tính helper để lấy FirebaseAuth instance khi thực sự cần dùng
    private val auth: FirebaseAuth
        get() = authLazy.get()

    override val currentUser: User?
        get() = auth.currentUser?.let {
            User(uid = it.uid, email = it.email ?: "", userName = it.displayName ?: "")
        }

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!
            Result.Success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    userName = firebaseUser.displayName ?: ""
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi đăng nhập")
        }
    }

    override suspend fun registerWithEmail(email: String, userName: String, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!
            val profileUpdates = userProfileChangeRequest {
                this.displayName = userName
            }
            firebaseUser.updateProfile(profileUpdates).await()
            Result.Success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    userName = userName
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi đăng ký")
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi đăng xuất")
        }
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { authInstance ->
            trySend(
                authInstance.currentUser?.let {
                    User(uid = it.uid, email = it.email ?: "", userName = it.displayName ?: "")
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi xác thực Google")
        }
    }
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                firebaseUser.delete().await()
                Result.Success(Unit)
            } else {
                Result.Error("Không tìm thấy thông tin người dùng đang đăng nhập")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xóa tài khoản")
        }
    }
}