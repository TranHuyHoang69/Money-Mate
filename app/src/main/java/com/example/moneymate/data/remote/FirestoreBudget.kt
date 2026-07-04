package com.example.moneymate.data.remote

data class FirestoreBudget(
    val userId: String = "",
    val categoryId: Long = 0L,
    val categoryStableId: String = "",
    val categoryTitle: String = "",
    val categoryColorHex: String = "",
    val amount: Double = 0.0,
    val month: Int = 1,
    val year: Int = 1970
)
