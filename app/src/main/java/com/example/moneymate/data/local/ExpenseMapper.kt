package com.example.moneymate.data.local

import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.ExpensePendingOperation
import com.example.moneymate.domain.model.ExpenseSyncStatus
import com.example.moneymate.domain.model.TransactionType

/**
 * Mappers cho Category
 */
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = this.categoryId,
        stableId = this.stableId,
        title = this.title,
        iconResName = this.iconResName,
        colorHex = this.colorHex,
        type = try {
            TransactionType.valueOf(this.type)
        } catch (e: Exception) {
            TransactionType.SPEND
        },
        isDefault = this.isDefault
    )
}

// 🟢 NHẬN THÊM THAM SỐ userId do đối tượng Category (Domain) của bạn không có trường này
fun Category.toEntity(userId: String): CategoryEntity {
    return CategoryEntity(
        categoryId = this.id,
        stableId = this.stableId,
        userId = userId,
        title = this.title,
        iconResName = this.iconResName,
        colorHex = this.colorHex,
        type = this.type.name,
        isDefault = this.isDefault
    )
}

/**
 * Mappers cho Expense
 */
fun ExpenseWithCategory.toDomain(): Expense {
    return Expense(
        id = this.expense.id,
        firestoreDocId = this.expense.firestoreDocId,
        type = try {
            TransactionType.valueOf(this.expense.type)
        } catch (e: Exception) {
            TransactionType.SPEND
        },
        amount = this.expense.amount,
        timestamp = this.expense.timestamp,
        note = this.expense.note,
        category = this.categoryEntity.toDomain(),
        syncStatus = try {
            ExpenseSyncStatus.valueOf(this.expense.syncStatus)
        } catch (e: Exception) {
            ExpenseSyncStatus.SYNCED
        },
        isDeleted = this.expense.isDeleted,
        localUpdatedAt = this.expense.localUpdatedAt,
        lastSyncError = this.expense.lastSyncError,
        pendingOperation = try {
            ExpensePendingOperation.valueOf(this.expense.pendingOperation)
        } catch (e: Exception) {
            pendingOperationFromStatus(this.expense.syncStatus)
        },
        remoteUpdatedAt = this.expense.remoteUpdatedAt,
        lastSyncAttemptAt = this.expense.lastSyncAttemptAt,
        retryCount = this.expense.retryCount
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = this.id,
        firestoreDocId = this.firestoreDocId,
        type = this.type.name,
        amount = this.amount,
        categoryId = this.category.id,
        categoryStableId = this.category.stableId,
        timestamp = this.timestamp,
        note = this.note,
        syncStatus = this.syncStatus.name,
        isDeleted = this.isDeleted,
        localUpdatedAt = this.localUpdatedAt,
        lastSyncError = this.lastSyncError,
        pendingOperation = this.pendingOperation.name,
        remoteUpdatedAt = this.remoteUpdatedAt,
        lastSyncAttemptAt = this.lastSyncAttemptAt,
        retryCount = this.retryCount
    )
}

private fun pendingOperationFromStatus(syncStatus: String): ExpensePendingOperation {
    return when (syncStatus) {
        ExpenseSyncStatus.PENDING_CREATE.name -> ExpensePendingOperation.CREATE
        ExpenseSyncStatus.PENDING_UPDATE.name -> ExpensePendingOperation.UPDATE
        ExpenseSyncStatus.PENDING_DELETE.name -> ExpensePendingOperation.DELETE
        else -> ExpensePendingOperation.NONE
    }
}
