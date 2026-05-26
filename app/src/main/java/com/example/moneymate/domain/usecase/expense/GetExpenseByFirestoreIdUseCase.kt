package com.example.moneymate.domain.usecase.expense


import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository // Thay bằng tên Repository của bạn
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseByFirestoreIdUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(id: String): Flow<Result<Expense?>> {
        return repository.getExpenseByFirestoreId(id)
    }
}