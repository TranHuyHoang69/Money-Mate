package com.example.moneymate.data.remote

import com.example.moneymate.domain.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getDocRef(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection("settings")
        .document("preferences")

    suspend fun getRemotePreferences(userId: String): UserPreferences? {
        return try {
            val snapshot = getDocRef(userId).get().await()
            if (snapshot.exists()) {
                UserPreferences(
                    language = snapshot.getString("language") ?: "vi",
                    themeMode = snapshot.getString("themeMode") ?: "system"
                )
            } else null
        } catch (e: Exception) {
            null // Trả về null khi mất kết nối mạng để hệ thống ưu tiên chạy offline
        }
    }

    suspend fun saveRemotePreferences(userId: String, preferences: UserPreferences) {
        val data = mapOf(
            "language" to preferences.language,
            "themeMode" to preferences.themeMode
        )
        getDocRef(userId).set(data).await()
    }
}