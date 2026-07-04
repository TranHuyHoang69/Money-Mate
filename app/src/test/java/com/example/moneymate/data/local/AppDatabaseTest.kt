package com.example.moneymate.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDatabaseTest {

    @Test
    fun databaseName_matchesProductionDatabaseName() {
        assertEquals("expense-db", AppDatabase.DATABASE_NAME)
    }
}
