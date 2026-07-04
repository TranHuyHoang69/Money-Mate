package com.example.moneymate.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

enum class FirestoreSyncErrorType(
    val code: String,
    val retryable: Boolean,
    val userMessage: String
) {
    NETWORK("NETWORK", true, "Mat ket noi mang, giao dich se duoc dong bo lai"),
    AUTH("AUTH", false, "Phien dang nhap het han, vui long dang nhap lai"),
    PERMISSION("PERMISSION", false, "Khong co quyen dong bo giao dich nay"),
    NOT_FOUND("NOT_FOUND", false, "Khong tim thay giao dich tren may chu"),
    VALIDATION("VALIDATION", false, "Du lieu giao dich khong hop le"),
    UNKNOWN("UNKNOWN", false, "Khong the dong bo giao dich")
}

object FirestoreSyncError {
    fun safeMessage(throwable: Throwable, fallback: String): String {
        val type = classify(throwable)
        val detail = fallback.takeIf { it.isNotBlank() } ?: type.userMessage
        return "${type.code}: $detail"
    }

    fun isRetryable(message: String?): Boolean {
        return typeFromMessage(message).retryable
    }

    fun typeFromMessage(message: String?): FirestoreSyncErrorType {
        val code = message?.substringBefore(':')?.trim()
        return FirestoreSyncErrorType.values().firstOrNull { it.code == code }
            ?: FirestoreSyncErrorType.UNKNOWN
    }

    private fun classify(throwable: Throwable): FirestoreSyncErrorType {
        if (throwable is FirebaseNetworkException || throwable is IOException) {
            return FirestoreSyncErrorType.NETWORK
        }

        val firestoreException = throwable as? FirebaseFirestoreException
            ?: return FirestoreSyncErrorType.UNKNOWN

        return when (firestoreException.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> FirestoreSyncErrorType.NETWORK
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> FirestoreSyncErrorType.AUTH
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirestoreSyncErrorType.PERMISSION
            FirebaseFirestoreException.Code.NOT_FOUND -> FirestoreSyncErrorType.NOT_FOUND
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            FirebaseFirestoreException.Code.OUT_OF_RANGE -> FirestoreSyncErrorType.VALIDATION
            else -> FirestoreSyncErrorType.UNKNOWN
        }
    }
}
