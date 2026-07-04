package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.moneymate.domain.model.RecurringTransaction
import com.example.moneymate.domain.model.TransactionType

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["firestoreDocId"], unique = true)]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreDocId: String = "",
    val userId: String = "",
    val type: String = TransactionType.SPEND.name,
    val amount: Double = 0.0,
    val categoryId: Long = 0,
    val categoryStableId: String = "",
    val categoryTitle: String = "",
    val categoryColorHex: String = "#006C4C",
    val note: String = "",
    val repeatInterval: String = "Hàng tháng",
    val startDate: Long = System.currentTimeMillis(),
    val nextRunAt: Long = System.currentTimeMillis(),
    val lastGeneratedAt: Long? = null,
    val isActive: Boolean = true
)

fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    val parsedType = try {
        TransactionType.valueOf(type)
    } catch (e: Exception) {
        TransactionType.SPEND
    }

    return RecurringTransaction(
        id = id,
        firestoreDocId = firestoreDocId,
        userId = userId,
        type = parsedType,
        amount = amount,
        categoryId = categoryId,
        categoryStableId = categoryStableId,
        categoryTitle = categoryTitle,
        categoryColorHex = categoryColorHex,
        note = note,
        repeatInterval = repeatInterval,
        startDate = startDate,
        nextRunAt = nextRunAt,
        lastGeneratedAt = lastGeneratedAt,
        isActive = isActive
    )
}

fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        firestoreDocId = firestoreDocId,
        userId = userId,
        type = type.name,
        amount = amount,
        categoryId = categoryId,
        categoryStableId = categoryStableId,
        categoryTitle = categoryTitle,
        categoryColorHex = categoryColorHex,
        note = note,
        repeatInterval = repeatInterval,
        startDate = startDate,
        nextRunAt = nextRunAt,
        lastGeneratedAt = lastGeneratedAt,
        isActive = isActive
    )
}
