// domain/model/UserPreferences.kt
package com.example.moneymate.domain.model

data class UserPreferences(
    val language: String = "vi",
    val themeMode: String = "system" // "light" | "dark" | "system"
)

