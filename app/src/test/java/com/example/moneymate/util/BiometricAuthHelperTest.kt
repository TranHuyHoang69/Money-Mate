package com.example.moneymate.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthHelperTest {

    @Test
    fun `biometric success is available`() {
        assertTrue(BiometricAuthHelper.isAvailable(BiometricManager.BIOMETRIC_SUCCESS))
        assertFalse(BiometricAuthHelper.isAvailable(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE))
    }

    @Test
    fun `biometric status messages are safe fallback messages`() {
        val noHardwareMessage = BiometricAuthHelper.statusMessage(
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        )
        val notEnrolledMessage = BiometricAuthHelper.statusMessage(
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )

        assertTrue(noHardwareMessage.contains("PIN"))
        assertTrue(notEnrolledMessage.contains("PIN"))
    }

    @Test
    fun `user cancel errors are recognized`() {
        assertTrue(BiometricAuthHelper.isUserCancelError(BiometricPrompt.ERROR_NEGATIVE_BUTTON))
        assertTrue(BiometricAuthHelper.isUserCancelError(BiometricPrompt.ERROR_USER_CANCELED))
        assertTrue(BiometricAuthHelper.isUserCancelError(BiometricPrompt.ERROR_CANCELED))
        assertFalse(BiometricAuthHelper.isUserCancelError(BiometricPrompt.ERROR_HW_UNAVAILABLE))
    }
}
