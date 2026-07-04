package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.moneymate.domain.model.ExpensePendingOperation
import com.example.moneymate.domain.model.ExpenseSyncStatus

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index(value = ["firestoreDocId"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["categoryStableId"]),
        Index(value = ["pendingOperation"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreDocId: String = "",
    val type: String,
    val amount: Double,
    val categoryId: Long,
    val categoryStableId: String = "",
    val timestamp: Long,
    val note: String,
    val syncStatus: String = ExpenseSyncStatus.SYNCED.name,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = 0L,
    val lastSyncError: String? = null,
    val pendingOperation: String = ExpensePendingOperation.NONE.name,
    val remoteUpdatedAt: Long = 0L,
    val lastSyncAttemptAt: Long = 0L,
    val retryCount: Int = 0
)

enum class ExpenseType {
    SPEND,
    INCOME
}
