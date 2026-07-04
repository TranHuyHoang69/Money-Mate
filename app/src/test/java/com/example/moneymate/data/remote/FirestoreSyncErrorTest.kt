package com.example.moneymate.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FirestoreSyncErrorTest {

    @Test
    fun safeMessage_mapsUnavailableAsRetryableNetworkError() {
        val message = FirestoreSyncError.safeMessage(
            IOException("offline"),
            "Error adding expense"
        )

        assertEquals(FirestoreSyncErrorType.NETWORK, FirestoreSyncError.typeFromMessage(message))
        assertTrue(FirestoreSyncError.isRetryable(message))
    }

    @Test
    fun permissionMessage_isNonRetryableError() {
        val message = "PERMISSION: Error updating expense"
        assertEquals(FirestoreSyncErrorType.PERMISSION, FirestoreSyncError.typeFromMessage(message))
        assertFalse(FirestoreSyncError.isRetryable(message))
    }

    @Test
    fun unknownMessage_isNonRetryable() {
        assertEquals(FirestoreSyncErrorType.UNKNOWN, FirestoreSyncError.typeFromMessage("plain error"))
        assertFalse(FirestoreSyncError.isRetryable("plain error"))
    }
}
