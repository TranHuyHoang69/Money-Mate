package com.example.moneymate.data.local

import com.example.moneymate.domain.model.Expense

enum class TransactionTypeUI { EXPENSE, INCOME }
enum class PeriodType { DAY, WEEK, MONTH, YEAR, RANGE }

data class HomeState(
    val selectedType: TransactionTypeUI = TransactionTypeUI.EXPENSE,
    val selectedPeriod: PeriodType = PeriodType.DAY,
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false
)