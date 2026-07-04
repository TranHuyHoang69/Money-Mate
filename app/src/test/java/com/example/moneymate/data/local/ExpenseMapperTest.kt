package com.example.moneymate.data.local

import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.ExpensePendingOperation
import com.example.moneymate.domain.model.ExpenseSyncStatus
import com.example.moneymate.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseMapperTest {

    @Test
    fun expenseToEntity_preservesSyncMetadata() {
        val entity = expense().copy(
            syncStatus = ExpenseSyncStatus.PENDING_UPDATE,
            isDeleted = true,
            localUpdatedAt = 123L,
            lastSyncError = "offline",
            pendingOperation = ExpensePendingOperation.UPDATE,
            remoteUpdatedAt = 321L,
            lastSyncAttemptAt = 456L,
            retryCount = 2
        ).toEntity()

        assertEquals(ExpenseSyncStatus.PENDING_UPDATE.name, entity.syncStatus)
        assertEquals(CategoryStableId.SPEND_FOOD, entity.categoryStableId)
        assertTrue(entity.isDeleted)
        assertEquals(123L, entity.localUpdatedAt)
        assertEquals("offline", entity.lastSyncError)
        assertEquals(ExpensePendingOperation.UPDATE.name, entity.pendingOperation)
        assertEquals(321L, entity.remoteUpdatedAt)
        assertEquals(456L, entity.lastSyncAttemptAt)
        assertEquals(2, entity.retryCount)
    }

    @Test
    fun expenseWithCategoryToDomain_preservesSyncMetadata() {
        val domain = ExpenseWithCategory(
            expense = ExpenseEntity(
                id = 7L,
                firestoreDocId = "expense-1",
                type = TransactionType.SPEND.name,
                amount = 1000.0,
                categoryId = 1L,
                timestamp = 1_700_000_000_000L,
                note = "Test",
                syncStatus = ExpenseSyncStatus.PENDING_CREATE.name,
                isDeleted = false,
                localUpdatedAt = 456L,
                lastSyncError = "network",
                pendingOperation = ExpensePendingOperation.CREATE.name,
                remoteUpdatedAt = 654L,
                lastSyncAttemptAt = 789L,
                retryCount = 3
            ),
            categoryEntity = categoryEntity()
        ).toDomain()

        assertEquals(ExpenseSyncStatus.PENDING_CREATE, domain.syncStatus)
        assertEquals(CategoryStableId.SPEND_FOOD, domain.category.stableId)
        assertFalse(domain.isDeleted)
        assertEquals(456L, domain.localUpdatedAt)
        assertEquals("network", domain.lastSyncError)
        assertEquals(ExpensePendingOperation.CREATE, domain.pendingOperation)
        assertEquals(654L, domain.remoteUpdatedAt)
        assertEquals(789L, domain.lastSyncAttemptAt)
        assertEquals(3, domain.retryCount)
    }

    @Test
    fun expenseWithCategoryToDomain_derivesPendingOperationFromLegacySyncStatus() {
        val domain = ExpenseWithCategory(
            expense = ExpenseEntity(
                id = 7L,
                firestoreDocId = "expense-1",
                type = TransactionType.SPEND.name,
                amount = 1000.0,
                categoryId = 1L,
                timestamp = 1_700_000_000_000L,
                note = "Test",
                syncStatus = ExpenseSyncStatus.PENDING_DELETE.name,
                pendingOperation = "UNKNOWN"
            ),
            categoryEntity = categoryEntity()
        ).toDomain()

        assertEquals(ExpensePendingOperation.DELETE, domain.pendingOperation)
    }

    private fun expense(): Expense {
        return Expense(
            firestoreDocId = "expense-1",
            type = TransactionType.SPEND,
            amount = 1000.0,
            category = Category(
                id = 1L,
                stableId = CategoryStableId.SPEND_FOOD,
                title = "Food",
                iconResName = "ic_food",
                colorHex = "#FF0000",
                type = TransactionType.SPEND
            ),
            timestamp = 1_700_000_000_000L,
            note = "Test"
        )
    }

    private fun categoryEntity(): CategoryEntity {
        return CategoryEntity(
            categoryId = 1L,
            stableId = CategoryStableId.SPEND_FOOD,
            userId = "user-1",
            title = "Food",
            iconResName = "ic_food",
            colorHex = "#FF0000",
            type = TransactionType.SPEND.name
        )
    }
}
