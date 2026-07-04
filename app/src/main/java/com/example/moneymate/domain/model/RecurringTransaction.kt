package com.example.moneymate.domain.model

data class RecurringTransaction(
    val id: Long = 0,
    val firestoreDocId: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.SPEND,
    val amount: Double = 0.0,
    val categoryId: Long = 0,
    val categoryStableId: String = "",
    val categoryTitle: String = "",
    val categoryColorHex: String = "#006C4C",
    val note: String = "",
    val repeatInterval: String = "Hàng tháng",
    val startDate: Long = System.currentTimeMillis(),
    val nextRunAt: Long = System.currentTimeMillis(),
    val lastGeneratedAt: Long? = null,
    val isActive: Boolean = true
) {
    val category: Category
        get() = Category(
            id = categoryId,
            stableId = categoryStableId,
            title = categoryTitle,
            iconResName = "",
            colorHex = categoryColorHex,
            type = type
        )
}
