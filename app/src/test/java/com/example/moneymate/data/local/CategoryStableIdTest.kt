package com.example.moneymate.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryStableIdTest {

    @Test
    fun ensureStableId_assignsFixedKeyForDefaultFoodCategory() {
        val category = CategoryEntity(
            userId = "user-1",
            title = "Food",
            iconResName = "ic_food",
            colorHex = "#4CB080",
            type = "SPEND",
            isDefault = true
        )

        val result = CategoryStableId.ensureStableId(category)

        assertEquals(CategoryStableId.SPEND_FOOD, result.stableId)
    }

    @Test
    fun ensureStableId_assignsUuidForCustomCategory() {
        val category = CategoryEntity(
            userId = "user-1",
            title = "Custom",
            iconResName = "ic_custom",
            colorHex = "#123456",
            type = "SPEND",
            isDefault = false
        )

        val result = CategoryStableId.ensureStableId(category)

        assertTrue(result.stableId.startsWith("custom_"))
        assertNotEquals("Custom", result.stableId)
    }

    @Test
    fun ensureStableId_keepsExistingStableId() {
        val category = CategoryEntity(
            stableId = "custom_existing",
            userId = "user-1",
            title = "Custom",
            iconResName = "ic_custom",
            colorHex = "#123456",
            type = "SPEND"
        )

        val result = CategoryStableId.ensureStableId(category)

        assertEquals("custom_existing", result.stableId)
    }
}
