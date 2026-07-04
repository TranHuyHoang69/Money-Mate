package com.example.moneymate.ui.screen

import com.example.moneymate.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailListScreenRouteTest {

    @Test
    fun parseTransactionTypeRoute_usesStableEnumName() {
        assertEquals(TransactionType.SPEND, parseTransactionTypeRoute("SPEND"))
        assertEquals(TransactionType.INCOME, parseTransactionTypeRoute("INCOME"))
    }

    @Test
    fun parseTransactionTypeRoute_unknownValueFallsBackToSpend() {
        assertEquals(TransactionType.SPEND, parseTransactionTypeRoute("legacy-display-value"))
        assertEquals(TransactionType.SPEND, parseTransactionTypeRoute("unexpected"))
    }
}
