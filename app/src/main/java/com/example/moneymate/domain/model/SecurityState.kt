package com.example.moneymate.domain.model


import com.google.firebase.Timestamp

data class SecurityState(
    val hasPin: Boolean = false,
    val biometricEnabled: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
