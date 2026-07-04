package com.example.moneymate.domain.model

data class Budget(
    val id: Long = 0,
    val firestoreDocId: String = "",
    val userId: String = "",
    val categoryId: Long,
    val categoryStableId: String = "",
    val categoryTitle: String,
    val categoryColorHex: String,
    val amount: Double,
    val month: Int,
    val year: Int
)

data class BudgetProgress(
    val budget: Budget,
    val spentAmount: Double
) {
    val progress: Float
        get() = if (budget.amount > 0) (spentAmount / budget.amount).toFloat() else 0f

    val remainingAmount: Double
        get() = budget.amount - spentAmount

    val isExceeded: Boolean
        get() = spentAmount > budget.amount
}
