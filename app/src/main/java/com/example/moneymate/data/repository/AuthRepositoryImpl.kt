package com.example.moneymate.data.repository

import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.Lazy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authLazy: Lazy<FirebaseAuth>,
    private val firestoreDataSource: FirestoreDataSource
) : AuthRepository {

    private val auth: FirebaseAuth
        get() = authLazy.get()

    private val recentLoginRequiredMessage = "Vui lòng đăng nhập lại trước khi xóa tài khoản"

    override val currentUser: User?
        get() = auth.currentUser?.let {
            User(uid = it.uid, email = it.email.orEmpty(), userName = it.displayName.orEmpty())
        }

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val firebaseUser = auth.signInWithEmailAndPassword(email, password).await().user
                ?: return Result.Error("Không tìm thấy thông tin người dùng")

            Result.Success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email.orEmpty(),
                    userName = firebaseUser.displayName.orEmpty()
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi đăng nhập")
        }
    }

    override suspend fun registerWithEmail(
        email: String,
        userName: String,
        password: String
    ): Result<User> {
        return try {
            val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: return Result.Error("Không thể tạo tài khoản")

            val profileUpdates = userProfileChangeRequest {
                displayName = userName
            }
            firebaseUser.updateProfile(profileUpdates).await()

            Result.Success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email.orEmpty(),
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
                    User(
                        uid = it.uid,
                        email = it.email.orEmpty(),
                        userName = it.displayName.orEmpty()
                    )
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

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.Error("Không tìm thấy thông tin người dùng đang đăng nhập")
            val email = firebaseUser.email
                ?: return Result.Error("Tài khoản hiện tại không có email để xác thực lại")

            val credential = EmailAuthProvider.getCredential(email, password)
            firebaseUser.reauthenticate(credential).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể xác thực lại tài khoản")
        }
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.Error("Không tìm thấy thông tin người dùng đang đăng nhập")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseUser.reauthenticate(credential).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể xác thực lại Google")
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.Error("Không tìm thấy thông tin người dùng đang đăng nhập")
            val userId = firebaseUser.uid
            val lastSignInTimestamp = firebaseUser.metadata?.lastSignInTimestamp ?: 0L

            if (System.currentTimeMillis() - lastSignInTimestamp > RECENT_LOGIN_MAX_AGE_MS) {
                return Result.Error(recentLoginRequiredMessage)
            }

            when (val deleteDataResult = firestoreDataSource.deleteUserData(userId)) {
                is Result.Error -> {
                    return Result.Error("Không thể xóa dữ liệu người dùng: ${deleteDataResult.message}")
                }

                is Result.Success -> Unit
                Result.Loading -> Unit
            }

            firebaseUser.delete().await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.Error(recentLoginRequiredMessage)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xóa tài khoản")
        }
    }

    private companion object {
        const val RECENT_LOGIN_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
