package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextRunAt ASC")
    fun observeByUser(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 AND nextRunAt <= :now ORDER BY nextRunAt ASC")
    suspend fun getDueTransactions(userId: String, now: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<RecurringTransactionEntity>)

    @Update
    suspend fun update(transaction: RecurringTransactionEntity)

    @Delete
    suspend fun delete(transaction: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE firestoreDocId = :firestoreDocId")
    suspend fun deleteByFirestoreDocId(firestoreDocId: String)

    @Query(
        """
        SELECT COUNT(*) FROM recurring_transactions
        WHERE userId = :userId
            AND (
                categoryId = :categoryId
                OR (:categoryStableId != '' AND categoryStableId = :categoryStableId)
            )
        """
    )
    suspend fun countRecurringTransactionsByCategory(
        userId: String,
        categoryId: Long,
        categoryStableId: String
    ): Int
}
