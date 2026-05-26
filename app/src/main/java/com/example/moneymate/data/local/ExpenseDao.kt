package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    // 2. Lọc theo thời gian
    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesWithCategoryByPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    // 3. Lấy 1 khoản chi theo ID nội bộ Room (Long)
    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseWithCategoryById(id: Long): Flow<ExpenseWithCategory?>

    // 🟢 5a. Lấy 1 khoản chi theo ID định danh từ Firestore (Dùng cho điều hướng màn hình chi tiết mới)
    @Transaction
    @Query("SELECT * FROM expenses WHERE firestoreDocId = :firestoreId")
    fun getExpenseWithCategoryByFirestoreId(firestoreId: String): Flow<ExpenseWithCategory?>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    // 🟢 5b. Chèn hoặc cập nhật đồng loạt dữ liệu từ đám mây đổ về máy (Offline-First Sync)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // 🟢 5c. Xóa nhanh một bản ghi dưới Local bằng ID Firestore khi nhận được tín hiệu xóa từ Cloud
    @Query("DELETE FROM expenses WHERE firestoreDocId = :firestoreId")
    suspend fun deleteExpenseByFirestoreId(firestoreId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}