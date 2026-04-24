package com.example.moneymate.domain.repository

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<Result<List<Expense>>>
    fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>>
    suspend fun insertExpense(expense: Expense): Result<Unit>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expense: Expense): Result<Unit>
    fun getExpenseById(id: Long): Flow<Result<Expense?>>

    fun getAllCategories(): Flow<Result<List<Category>>>
    suspend fun insertCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(category: Category): Result<Unit>
}