package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import jakarta.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        return expenseRepository.updateExpense(expense)
    }
}