package com.example.moneymate.data.remote

data class FirestoreExpense(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val categoryId: Long = 0,
    val categoryStableId: String = "",
    val categoryTitle: String = "",
    val categoryColorHex: String = "",
    val timestamp: Long = 0,
    val note: String = ""
)
