package com.example.moneymate.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinCredential(
    val saltBase64: String,
    val verifierBase64: String,
    val iterations: Int,
    val version: Int
)

object PinCredentialHasher {
    const val CURRENT_VERSION = 1
    const val DEFAULT_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val LEGACY_SHA_256_HEX_LENGTH = 64

    private val secureRandom = SecureRandom()

    fun create(pin: String): PinCredential {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        return create(pin = pin, salt = salt, iterations = DEFAULT_ITERATIONS)
    }

    fun create(pin: String, salt: ByteArray, iterations: Int): PinCredential {
        require(iterations > 0) { "Iterations must be positive" }
        require(salt.isNotEmpty()) { "Salt must not be empty" }

        val verifier = derive(pin = pin, salt = salt, iterations = iterations)
        return PinCredential(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            verifierBase64 = Base64.getEncoder().encodeToString(verifier),
            iterations = iterations,
            version = CURRENT_VERSION
        )
    }

    fun verify(pin: String, credential: PinCredential): Boolean {
        if (credential.version != CURRENT_VERSION || credential.iterations <= 0) return false

        return runCatching {
            val salt = Base64.getDecoder().decode(credential.saltBase64)
            val expected = Base64.getDecoder().decode(credential.verifierBase64)
            val actual = derive(pin = pin, salt = salt, iterations = credential.iterations)
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    fun verifyLegacySha256(pin: String, legacyHash: String?): Boolean {
        val normalized = legacyHash?.lowercase()?.trim()
        if (normalized.isNullOrBlank() || normalized.length != LEGACY_SHA_256_HEX_LENGTH) return false

        // Legacy compatibility only: old app versions stored unsalted SHA-256.
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        val actual = digest.joinToString(separator = "") { "%02x".format(it) }
        return MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), normalized.toByteArray(Charsets.UTF_8))
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
