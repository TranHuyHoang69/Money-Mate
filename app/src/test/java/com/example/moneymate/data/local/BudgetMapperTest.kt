package com.example.moneymate.data.local

import com.example.moneymate.domain.model.Budget
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMapperTest {

    @Test
    fun budgetEntityToDomain_preservesCategoryStableId() {
        val domain = BudgetEntity(
            userId = "user-1",
            categoryId = 7L,
            categoryStableId = CategoryStableId.SPEND_FOOD,
            categoryTitle = "Food",
            categoryColorHex = "#4CB080",
            amount = 500000.0,
            month = 7,
            year = 2026
        ).toDomain()

        assertEquals(CategoryStableId.SPEND_FOOD, domain.categoryStableId)
    }

    @Test
    fun budgetToEntity_preservesCategoryStableId() {
        val entity = Budget(
            userId = "user-1",
            categoryId = 7L,
            categoryStableId = CategoryStableId.SPEND_FOOD,
            categoryTitle = "Food",
            categoryColorHex = "#4CB080",
            amount = 500000.0,
            month = 7,
            year = 2026
        ).toEntity()

        assertEquals(CategoryStableId.SPEND_FOOD, entity.categoryStableId)
    }
}
