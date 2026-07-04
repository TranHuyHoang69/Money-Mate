package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_learning_patterns",
    indices = [
        Index(
            value = ["merchantName", "textFingerprint"],
            unique = true
        )
    ]
)
data class ReceiptLearningPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val textFingerprint: String,
    val selectedAmount: Double,
    val selectedCategoryTitle: String,
    val usageCount: Int = 1,
    val updatedAt: Long
)
