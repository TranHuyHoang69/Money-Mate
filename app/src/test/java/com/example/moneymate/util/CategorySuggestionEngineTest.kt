package com.example.moneymate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySuggestionEngineTest {
    @Test
    fun `suggests health category from long chau keyword`() {
        assertEquals(
            "Sức khoẻ",
            CategorySuggestionEngine.suggestCategory("NHA THUOC LONG CHAU")
        )
    }

    @Test
    fun `suggests health category from medicine keyword`() {
        assertEquals(
            "Sức khoẻ",
            CategorySuggestionEngine.suggestCategory("Nhà thuốc An Khang mua thuốc cảm")
        )
    }

    @Test
    fun `suggests cafe category from highlands keyword`() {
        assertEquals("Cafe", CategorySuggestionEngine.suggestCategory("Highlands Coffee"))
    }

    @Test
    fun `suggests transport category from grab keyword`() {
        assertEquals("Di chuyển", CategorySuggestionEngine.suggestCategory("Grab Bike"))
    }

    @Test
    fun `suggests shopping category from winmart keyword`() {
        assertEquals("Mua sắm", CategorySuggestionEngine.suggestCategory("WinMart siêu thị"))
    }

    @Test
    fun `defaults to food category when no keyword matches`() {
        assertEquals("Ăn uống", CategorySuggestionEngine.suggestCategory("Quan com binh dan"))
    }

    @Test
    fun `suggests user medicine category from pharmacy receipt`() {
        val categories = listOf("Ăn uống", "Thuốc", "Gia đình")

        assertEquals(
            "Thuốc",
            CategorySuggestionEngine.suggestCategory(
                text = "NHA THUOC LONG CHAU\nParacetamol 25.000d",
                availableCategories = categories
            )
        )
    }

    @Test
    fun `suggests user fuel category from transport receipt`() {
        val categories = listOf("Ăn uống", "Xăng xe", "Mua sắm")

        assertEquals(
            "Xăng xe",
            CategorySuggestionEngine.suggestCategory(
                text = "PETROLIMEX\nXang RON 95\nThanh toan 100.000d",
                availableCategories = categories
            )
        )
    }

    @Test
    fun `suggests user supermarket category from shopping receipt`() {
        val categories = listOf("Ăn uống", "Siêu thị", "Đi lại")

        assertEquals(
            "Siêu thị",
            CategorySuggestionEngine.suggestCategory(
                text = "WinMart\nSua tuoi 36.000d",
                availableCategories = categories
            )
        )
    }

    @Test
    fun `falls back to first category when food category is unavailable`() {
        assertEquals(
            "Khác",
            CategorySuggestionEngine.suggestCategory(
                text = "No recognizable merchant",
                availableCategories = listOf("Khác", "Du lịch")
            )
        )
    }

    @Test
    fun `falls back to food when category list is empty`() {
        assertEquals(
            "Ăn uống",
            CategorySuggestionEngine.suggestCategory(
                text = "No recognizable merchant",
                availableCategories = emptyList()
            )
        )
    }

    @Test
    fun `suggests food instead of gift for restaurant receipt`() {
        assertEquals(
            "Ăn uống",
            CategorySuggestionEngine.suggestCategory(
                text = """
                    Nhà Hàng Let's Go
                    Món Cá chim
                    Bia Sài gòn
                    Tổng cộng 9.010.000
                """.trimIndent(),
                availableCategories = listOf("Ăn uống", "Quà tặng", "Giải trí")
            )
        )
    }
}
