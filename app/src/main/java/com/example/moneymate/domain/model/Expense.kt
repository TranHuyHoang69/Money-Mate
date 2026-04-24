package com.example.moneymate.domain.model

enum class TransactionType{
    SPEND,INCOME
}

data class Category(
    val id: Long = 0,
    val title: String,
    val iconResName: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

data class Expense(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: Category,
    val timestamp: Long,
    val note: String
)

data class GroupedExpense(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val type: TransactionType
)