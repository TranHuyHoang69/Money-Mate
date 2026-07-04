package com.example.moneymate.domain.repository

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Budget
import com.example.moneymate.domain.model.BudgetProgress
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetProgress(month: Int, year: Int): Flow<Result<List<BudgetProgress>>>
    suspend fun saveBudget(budget: Budget): Result<Unit>
    suspend fun deleteBudget(budget: Budget): Result<Unit>
}
