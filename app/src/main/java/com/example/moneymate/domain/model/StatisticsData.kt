package com.example.moneymate.domain.model

// Dữ liệu cho tab Chung
data class OverviewStatistics(
    val income: Double,
    val expense: Double,
    val profit: Double,
    val loss: Double
)

// Dữ liệu cho từng danh mục
data class CategoryStatistics(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float
)

// Dữ liệu chi tiết giao dịch của danh mục
data class TransactionDetail(
    val expense: Expense,
    val displayDate: String
)