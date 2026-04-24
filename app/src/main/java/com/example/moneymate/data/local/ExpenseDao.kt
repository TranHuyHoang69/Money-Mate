package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // 1. Lấy tất cả (Có kèm Category)
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    // 2. Lọc theo thời gian (BẮT BUỘC đổi sang ExpenseWithCategory để không lỗi Mapper)
    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesWithCategoryByPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    // 3. Lấy 1 khoản chi theo ID (Cần cho màn hình chỉnh sửa)
    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseWithCategoryById(id: Long): Flow<ExpenseWithCategory?>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)


}