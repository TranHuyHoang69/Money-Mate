package com.example.moneymate.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCredentialHasherTest {

    @Test
    fun `creates PBKDF2 credential that verifies correct pin only`() {
        val credential = PinCredentialHasher.create(
            pin = "1234",
            salt = ByteArray(16) { it.toByte() },
            iterations = 1_000
        )

        assertTrue(credential.saltBase64.isNotBlank())
        assertTrue(credential.verifierBase64.isNotBlank())
        assertFalse(credential.verifierBase64.contains("1234"))
        assertTrue(PinCredentialHasher.verify("1234", credential))
        assertFalse(PinCredentialHasher.verify("0000", credential))
    }

    @Test
    fun `same pin with different salt creates different verifier`() {
        val first = PinCredentialHasher.create(
            pin = "1234",
            salt = ByteArray(16) { 1 },
            iterations = 1_000
        )
        val second = PinCredentialHasher.create(
            pin = "1234",
            salt = ByteArray(16) { 2 },
            iterations = 1_000
        )

        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.verifierBase64, second.verifierBase64)
        assertTrue(PinCredentialHasher.verify("1234", first))
        assertTrue(PinCredentialHasher.verify("1234", second))
    }

    @Test
    fun `legacy SHA-256 verifier is accepted only for matching pin`() {
        val legacyHashFor1234 = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"

        assertTrue(PinCredentialHasher.verifyLegacySha256("1234", legacyHashFor1234))
        assertFalse(PinCredentialHasher.verifyLegacySha256("0000", legacyHashFor1234))
    }

    @Test
    fun `corrupt credential fails closed`() {
        val credential = PinCredential(
            saltBase64 = "not-base64",
            verifierBase64 = "also-not-base64",
            iterations = 1_000,
            version = PinCredentialHasher.CURRENT_VERSION
        )

        assertFalse(PinCredentialHasher.verify("1234", credential))
    }
}
