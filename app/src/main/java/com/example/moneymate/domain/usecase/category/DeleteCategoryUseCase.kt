package com.example.moneymate.domain.usecase.category

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.repository.ExpenseRepository
import jakarta.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        return expenseRepository.deleteCategory(category)
    }
}
