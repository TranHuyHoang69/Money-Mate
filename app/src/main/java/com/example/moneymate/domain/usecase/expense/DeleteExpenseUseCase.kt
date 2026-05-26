package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    // ✅ ĐÃ SỬA: Thay đổi tham số truyền vào từ `expense: Expense` thành `firestoreId: String`
    suspend operator fun invoke(firestoreId: String): Result<Unit> {
        return repository.deleteExpense(firestoreId)
    }
}