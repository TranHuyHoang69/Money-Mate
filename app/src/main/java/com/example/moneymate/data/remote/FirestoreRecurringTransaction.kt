package com.example.moneymate.data.remote

data class FirestoreRecurringTransaction(
    val userId: String = "",
    val type: String = "SPEND",
    val amount: Double = 0.0,
    val categoryId: Long = 0,
    val categoryStableId: String = "",
    val categoryTitle: String = "",
    val categoryColorHex: String = "#006C4C",
    val note: String = "",
    val repeatInterval: String = "Hàng tháng",
    val startDate: Long = 0,
    val nextRunAt: Long = 0,
    val lastGeneratedAt: Long? = null,
    val isActive: Boolean = true
)
