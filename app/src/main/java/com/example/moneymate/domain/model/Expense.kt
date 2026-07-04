package com.example.moneymate.domain.model

enum class TransactionType{
    SPEND,INCOME
}

enum class ExpenseSyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED
}

enum class ExpensePendingOperation {
    NONE,
    CREATE,
    UPDATE,
    DELETE
}

data class Category(
    val id: Long = 0,
    val stableId: String = "",
    val title: String,
    val iconResName: String,
    val colorHex: String,
    val type: TransactionType,
    val isDefault: Boolean = false
)

data class Expense(
    val id: Long = 0,
    val firestoreDocId: String = "",
    val type: TransactionType,
    val amount: Double,
    val category: Category,
    val timestamp: Long,
    val note: String,
    val syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = 0L,
    val lastSyncError: String? = null,
    val pendingOperation: ExpensePendingOperation = ExpensePendingOperation.NONE,
    val remoteUpdatedAt: Long = 0L,
    val lastSyncAttemptAt: Long = 0L,
    val retryCount: Int = 0
)

data class GroupedExpense(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val type: TransactionType
)
