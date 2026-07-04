package com.example.moneymate.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SecurityRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String get() = auth.currentUser?.uid ?: "unknown_user"

    private val docRef get() = firestore.collection("users").document(userId)
        .collection("security").document("settings")

    suspend fun updateRemoteSettings(biometricEnabled: Boolean) {
        val data = hashMapOf(
            "biometricEnabled" to biometricEnabled,
            "updatedAt" to Timestamp.now()
        )
        docRef.set(data, SetOptions.merge()).await()
    }

    suspend fun deleteRemoteSettings() {
        docRef.delete().await()
    }
}
