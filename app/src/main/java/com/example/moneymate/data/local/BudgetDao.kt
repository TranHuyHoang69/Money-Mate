package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query(
        "SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year ORDER BY categoryTitle ASC"
    )
    fun getBudgetsByPeriod(userId: String, month: Int, year: Int): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query(
        "DELETE FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND month = :month AND year = :year"
    )
    suspend fun deleteBudgetByKey(userId: String, categoryId: Long, month: Int, year: Int)

    @Query(
        """
        SELECT COUNT(*) FROM budgets
        WHERE userId = :userId
            AND (
                categoryId = :categoryId
                OR (:categoryStableId != '' AND categoryStableId = :categoryStableId)
            )
        """
    )
    suspend fun countBudgetsByCategory(
        userId: String,
        categoryId: Long,
        categoryStableId: String
    ): Int
}
