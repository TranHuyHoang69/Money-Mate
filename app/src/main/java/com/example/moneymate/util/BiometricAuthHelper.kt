package com.example.moneymate.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {
    const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return current as? FragmentActivity
    }

    fun canAuthenticate(context: Context): Int {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
    }

    fun isAvailable(status: Int): Boolean = status == BiometricManager.BIOMETRIC_SUCCESS

    fun statusMessage(status: Int): String {
        return when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric san sang"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiet bi khong ho tro sinh trac hoc. Vui long dung PIN."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Sinh trac hoc dang khong kha dung. Vui long dung PIN."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Thiet bi chua thiet lap van tay/khuon mat. Vui long thiet lap trong Cai dat hoac dung PIN."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Can cap nhat bao mat he thong truoc khi dung sinh trac hoc."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Sinh trac hoc khong duoc ho tro tren thiet bi nay. Vui long dung PIN."
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Khong xac dinh duoc trang thai sinh trac hoc. Vui long dung PIN."
            else -> "Sinh trac hoc khong kha dung. Vui long dung PIN."
        }
    }

    fun isUserCancelError(errorCode: Int): Boolean {
        return errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
            errorCode == BiometricPrompt.ERROR_CANCELED
    }
}
