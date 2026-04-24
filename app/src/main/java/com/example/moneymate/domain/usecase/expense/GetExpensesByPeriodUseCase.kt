package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import com.example.moneymate.domain.Result
import javax.inject.Inject

class GetExpensesByPeriodUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(start: Long, end: Long): Flow<Result<List<Expense>>> {
        return repository.getExpensesByPeriod(start, end)
    }
}