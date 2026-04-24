package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(id: Long): Flow<Result<Expense?>> {
        return repository.getExpenseById(id)
    }
}