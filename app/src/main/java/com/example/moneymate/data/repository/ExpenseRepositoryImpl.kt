package com.example.moneymate.data.repository

import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.ExpenseDao
import com.example.moneymate.data.local.toDomain
import com.example.moneymate.data.local.toEntity
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : ExpenseRepository {

    // 1. Phải khớp tên "getAllExpenses"
    override fun getAllExpenses(): Flow<Result<List<Expense>>> =
        expenseDao.getAllExpensesWithCategory().map { list ->
            val domainList = list.map { it.toDomain() }
            Result.Success(domainList) as Result<List<Expense>>
        }.onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    // 2. Phải khớp tên "getExpensesByPeriod"
    override fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>> =
        expenseDao.getExpensesWithCategoryByPeriod(start, end).map { list ->
            val domainList = list.map { it.toDomain() }
            Result.Success(domainList) as Result<List<Expense>>
        }.onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    // 3. Implement getExpenseById
    override fun getExpenseById(id: Long): Flow<Result<Expense?>> =
        expenseDao.getExpenseWithCategoryById(id)
            .map{entity ->
                Result.Success(entity?.toDomain()) as Result<Expense?>
            }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    // 4. Phải khớp tên "getAllCategories"
    override fun getAllCategories(): Flow<Result<List<Category>>> =
        categoryDao.getAllCategories().map { list ->
            val domainList = list.map { it.toDomain() }
            Result.Success(domainList) as Result<List<Category>>
        }.onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }
    override fun getCategoriesByType(type: String): Flow<Result<List<Category>>> =
        categoryDao.getCategoriesByType(type).map { list ->
            Result.Success(list.map { it.toDomain() }) as Result<List<Category>>
        }.onStart { emit(Result.Loading) }.catch { emit(Result.Error(it.message ?: "Error")) }

    // --- Các hàm suspend khác giữ nguyên logic try-catch như hôm trước ---
    override suspend fun insertExpense(expense: Expense) = try {
        expenseDao.insertExpense(expense.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

    override suspend fun updateExpense(expense: Expense) = try {
        expenseDao.updateExpense(expense.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

    override suspend fun deleteExpense(expense: Expense) = try {
        expenseDao.deleteExpense(expense.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

    override suspend fun insertCategory(category: Category) = try {
        categoryDao.insertCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

    override suspend fun updateCategory(category: Category) = try {
        categoryDao.updateCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

    override suspend fun deleteCategory(category: Category) = try {
        categoryDao.deleteCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error") }

}