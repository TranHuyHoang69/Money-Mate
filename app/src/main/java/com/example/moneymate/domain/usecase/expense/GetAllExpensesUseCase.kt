package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetAllExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<Result<List<Expense>>> {
        return repository.getAllExpenses()
    }
}


