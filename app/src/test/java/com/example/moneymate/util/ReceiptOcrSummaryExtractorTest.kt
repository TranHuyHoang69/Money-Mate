package com.example.moneymate.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptOcrSummaryExtractorTest {
    @Test
    fun `extracts compact summary from long receipt and keeps important sections`() {
        val rawText = buildString {
            appendLine("WINMART")
            appendLine("Dia chi: 123 Nguyen Trai")
            for (index in 1..100) {
                appendLine("San pham $index 10.000d")
            }
            appendLine("Giam gia 20.000d")
            appendLine("Tien khach dua 500.000d")
            appendLine("Tien thoi 50.000d")
            appendLine("Tien phai tra 430.000d")
        }

        val summary = ReceiptOcrSummaryExtractor.extract(rawText)

        assertTrue(summary.isLongReceipt)
        assertTrue(summary.text.contains("WINMART"))
        assertTrue(summary.text.contains("Tien phai tra 430.000d"))
        assertTrue(summary.text.contains("San pham 100"))
        assertTrue(summary.summaryLineCount <= 80)
        assertTrue(summary.text.length <= 3_000)
        assertFalse(summary.text.contains("San pham 30"))
    }
}
