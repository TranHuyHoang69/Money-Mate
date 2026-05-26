package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    // Hàm này phải nhận vào Object Expense
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        return repository.updateExpense(expense)
    }
}