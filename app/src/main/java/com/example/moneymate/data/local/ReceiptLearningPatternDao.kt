package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReceiptLearningPatternDao {
    @Query(
        """
        SELECT * FROM receipt_learning_patterns
        WHERE merchantName = :merchantName
        AND textFingerprint = :textFingerprint
        LIMIT 1
        """
    )
    suspend fun findByPattern(
        merchantName: String,
        textFingerprint: String
    ): ReceiptLearningPatternEntity?

    @Query(
        """
        SELECT * FROM receipt_learning_patterns
        WHERE textFingerprint = :textFingerprint
        ORDER BY usageCount DESC, updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun findBestByFingerprint(textFingerprint: String): ReceiptLearningPatternEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pattern: ReceiptLearningPatternEntity): Long

    @Update
    suspend fun update(pattern: ReceiptLearningPatternEntity)

    @Query(
        """
        DELETE FROM receipt_learning_patterns
        WHERE merchantName = :merchantName
        AND textFingerprint = :textFingerprint
        """
    )
    suspend fun deleteByPattern(
        merchantName: String,
        textFingerprint: String
    ): Int

    @Query(
        """
        DELETE FROM receipt_learning_patterns
        WHERE textFingerprint = :textFingerprint
        """
    )
    suspend fun deleteByFingerprint(textFingerprint: String): Int

    @Query("DELETE FROM receipt_learning_patterns")
    suspend fun clearAll()
}
