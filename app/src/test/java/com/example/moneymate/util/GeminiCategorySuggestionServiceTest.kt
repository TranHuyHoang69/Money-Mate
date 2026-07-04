package com.example.moneymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class GeminiCategorySuggestionServiceTest {
    private val service = GeminiCategorySuggestionService()

    @Test
    fun `returns null when backend proxy is not configured`() = runTest {
        assertNull(
            service.suggestCategory(
                rawText = "Highlands Coffee Tong thanh toan 59000",
                merchantName = "Highlands Coffee",
                localCategory = "Cafe",
                availableCategories = listOf("Cafe", "An uong")
            )
        )
    }

    @Test
    fun `parses category only when response is in available categories`() {
        val response = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Xăng xe" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "Xăng xe",
            service.parseCategoryFromResponse(
                responseText = response,
                availableCategories = listOf("Ăn uống", "Xăng xe", "Siêu thị")
            )
        )
    }

    @Test
    fun `parses category from json model text`() {
        val response = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"category\":\"Xăng xe\",\"confidence\":0.91,\"reason\":\"OCR có tín hiệu xăng xe\"}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "Xăng xe",
            service.parseCategoryFromResponse(
                responseText = response,
                availableCategories = listOf("Ăn uống", "Xăng xe", "Siêu thị")
            )
        )
    }

    @Test
    fun `rejects category that is not in available categories`() {
        val response = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Du lịch" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertNull(
            service.parseCategoryFromResponse(
                responseText = response,
                availableCategories = listOf("Ăn uống", "Xăng xe", "Siêu thị")
            )
        )
    }

    @Test
    fun `builds prompt from summary for long receipt instead of only raw text prefix`() {
        val rawText = buildString {
            appendLine("WINMART")
            for (index in 1..100) {
                appendLine("San pham $index 10.000d")
            }
            appendLine("Tong thanh toan 999.000d")
        }

        val prompt = service.buildPrompt(
            rawText = rawText,
            merchantName = "WINMART",
            localCategory = "Siêu thị",
            availableCategories = listOf("Ăn uống", "Siêu thị")
        )

        assertTrue(prompt.contains("Raw OCR đã rút gọn từ hóa đơn dài"))
        assertTrue(prompt.contains("Tong thanh toan 999.000d"))
        assertFalse(prompt.contains("San pham 30 10.000d"))
    }

    @Test
    fun `rejects gift suggestion for restaurant receipt without gift signal`() {
        assertFalse(
            service.shouldAcceptSuggestedCategory(
                suggestedCategory = "Quà tặng",
                rawText = """
                    Nhà Hàng Let's Go
                    Món
                    Cá chim
                    Bia Sài gòn
                    Tổng cộng 9.010.000
                """.trimIndent(),
                merchantName = "Nhà Hàng Let's Go",
                localCategory = "Ăn uống"
            )
        )
    }

    @Test
    fun `accepts gift suggestion when receipt has gift signal`() {
        assertTrue(
            service.shouldAcceptSuggestedCategory(
                suggestedCategory = "Quà tặng",
                rawText = """
                    Shop quà tặng lưu niệm
                    Gói quà sinh nhật 250.000
                """.trimIndent(),
                merchantName = "Shop quà tặng lưu niệm",
                localCategory = "Quà tặng"
            )
        )
    }
}
