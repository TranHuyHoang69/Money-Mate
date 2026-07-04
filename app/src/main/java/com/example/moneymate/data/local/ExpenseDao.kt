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
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    // 2. Lọc theo thời gian
    @Transaction
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesWithCategoryByPeriod(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    // 3. Lấy 1 khoản chi theo ID nội bộ Room (Long)
    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id AND isDeleted = 0")
    fun getExpenseWithCategoryById(id: Long): Flow<ExpenseWithCategory?>

    // 🟢 5a. Lấy 1 khoản chi theo ID định danh từ Firestore (Dùng cho điều hướng màn hình chi tiết mới)
    @Transaction
    @Query("SELECT * FROM expenses WHERE firestoreDocId = :firestoreId AND isDeleted = 0")
    fun getExpenseWithCategoryByFirestoreId(firestoreId: String): Flow<ExpenseWithCategory?>

    @Transaction
    @Query("SELECT * FROM expenses WHERE syncStatus IN (:statuses) ORDER BY localUpdatedAt ASC")
    suspend fun getExpensesWithCategoryBySyncStatuses(statuses: List<String>): List<ExpenseWithCategory>

    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE (
            pendingOperation != :noneOperation
            AND syncStatus != :failedStatus
        ) OR syncStatus IN (:legacyPendingStatuses)
        ORDER BY localUpdatedAt ASC
        """
    )
    suspend fun getPendingExpensesForSync(
        noneOperation: String,
        failedStatus: String,
        legacyPendingStatuses: List<String>
    ): List<ExpenseWithCategory>

    @Transaction
    @Query("SELECT * FROM expenses WHERE firestoreDocId = :firestoreId LIMIT 1")
    suspend fun getExpenseWithCategoryByFirestoreIdOnce(firestoreId: String): ExpenseWithCategory?

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

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query(
        """
        SELECT COUNT(*) FROM expenses
        WHERE categoryId = :categoryId
            OR (:categoryStableId != '' AND categoryStableId = :categoryStableId)
        """
    )
    suspend fun countExpensesByCategory(categoryId: Long, categoryStableId: String): Int

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}
