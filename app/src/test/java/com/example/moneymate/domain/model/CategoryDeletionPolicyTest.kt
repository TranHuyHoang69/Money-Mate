package com.example.moneymate.domain.model

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDeletionPolicyTest {

    @Test
    fun blockingMessage_returnsNullWhenCategoryIsUnused() {
        val message = CategoryDeletionPolicy.blockingMessage(CategoryUsage())

        assertNull(message)
    }

    @Test
    fun blockingMessage_mentionsFinancialUsageWhenCategoryIsUsed() {
        val message = CategoryDeletionPolicy.blockingMessage(
            CategoryUsage(
                expenseCount = 2,
                budgetCount = 1,
                recurringTransactionCount = 1
            )
        )

        requireNotNull(message)
        assertTrue(message.contains("giao dịch: 2"))
        assertTrue(message.contains("ngân sách: 1"))
        assertTrue(message.contains("giao dịch định kỳ: 1"))
    }
}
