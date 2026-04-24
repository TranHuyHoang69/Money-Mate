package com.example.moneymate.domain.usecase.category

import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllCategoriesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<Result<List<Category>>> {
        return repository.getAllCategories()
    }
}