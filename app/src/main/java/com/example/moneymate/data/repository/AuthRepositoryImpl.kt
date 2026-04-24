package com.example.moneymate.data.repository

import com.example.moneymate.domain.model.User
import com.example.moneymate.domain.repository.AuthRepository
import com.example.moneymate.domain.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
): AuthRepository{
    override val currentUser: User?
        get() = auth.currentUser?.let {
            User(uid = it.uid, email = it.email ?: "", userName = it.displayName ?: "")
        }

    override val isLoggedIn: Boolean
        get() =auth.currentUser != null

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email,password).await()
            val firebaseUser = authResult.user!!
            Result.Success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    userName = firebaseUser.displayName ?: ""
                )
            )
        }catch (e: Exception){
            Result.Error(e.message ?: "Lỗi đăng nhập")
        }
    }

    override suspend fun registerWithEmail(email: String, userName: String, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email,password).await()
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
        }catch (e: Exception){
            Result.Error(e.message ?: "Lỗi khi đăng ký")
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        }catch (e: Exception){
            Result.Error(e.message ?: "Lỗi đăng xuất")
        }
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener{auth ->
            trySend(
                auth.currentUser?.let {
                    User(uid = it.uid, email = it.email ?: "", userName = it.displayName ?: "")
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}