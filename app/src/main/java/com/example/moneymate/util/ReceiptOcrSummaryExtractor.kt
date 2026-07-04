package com.example.moneymate.util

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ReceiptOcrSummaryExtractor {
    fun extract(rawText: String): ReceiptOcrSummary {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val isLongReceipt = lines.size >= LONG_RECEIPT_LINE_THRESHOLD ||
            rawText.length >= LONG_RECEIPT_CHAR_THRESHOLD

        if (!isLongReceipt) {
            return ReceiptOcrSummary(
                text = rawText.trim(),
                isLongReceipt = false,
                originalLineCount = lines.size,
                summaryLineCount = lines.size
            )
        }

        val selectedIndices = linkedSetOf<Int>()
        repeat(min(HEAD_LINE_COUNT, lines.size)) { index ->
            selectedIndices.add(index)
        }

        var keywordContextLines = 0
        lines.forEachIndexed { index, line ->
            if (keywordContextLines >= MAX_KEYWORD_CONTEXT_LINES) return@forEachIndexed
            if (normalize(line).containsAny(summaryKeywords)) {
                val fromIndex = max(0, index - KEYWORD_CONTEXT_RADIUS)
                val toIndex = min(lines.lastIndex, index + KEYWORD_CONTEXT_RADIUS)
                for (contextIndex in fromIndex..toIndex) {
                    if (selectedIndices.add(contextIndex)) {
                        keywordContextLines += 1
                    }
                }
            }
        }

        val tailStartIndex = max(0, lines.size - TAIL_LINE_COUNT)
        for (index in tailStartIndex..lines.lastIndex) {
            selectedIndices.add(index)
        }

        val summaryLines = selectedIndices
            .sorted()
            .map { lines[it] }
            .trimToLimits()

        return ReceiptOcrSummary(
            text = summaryLines.joinToString("\n"),
            isLongReceipt = true,
            originalLineCount = lines.size,
            summaryLineCount = summaryLines.size
        )
    }

    private fun List<String>.trimToLimits(): List<String> {
        val result = mutableListOf<String>()
        var charCount = 0

        for (line in this) {
            if (result.size >= MAX_SUMMARY_LINES) break
            val nextCharCount = charCount + line.length + 1
            if (nextCharCount > MAX_SUMMARY_CHARS) break
            result.add(line)
            charCount = nextCharCount
        }

        return result
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private fun normalize(value: String): String {
        val noVietnameseD = value
            .lowercase(Locale.ROOT)
            .replace('đ', 'd')
        return Normalizer.normalize(noVietnameseD, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private const val LONG_RECEIPT_LINE_THRESHOLD = 60
    private const val LONG_RECEIPT_CHAR_THRESHOLD = 3_000
    private const val HEAD_LINE_COUNT = 8
    private const val TAIL_LINE_COUNT = 45
    private const val MAX_SUMMARY_LINES = 80
    private const val MAX_SUMMARY_CHARS = 3_000
    private const val KEYWORD_CONTEXT_RADIUS = 2
    private const val MAX_KEYWORD_CONTEXT_LINES = 24

    private val summaryKeywords = listOf(
        "tong cong",
        "tong thanh toan",
        "can thanh toan",
        "tien phai tra",
        "phai tra",
        "thanh toan",
        "thanh tien",
        "tong tien",
        "grand total",
        "total",
        "amount",
        "payment",
        "vat",
        "thue",
        "giam gia",
        "khuyen mai",
        "voucher",
        "discount",
        "tien khach dua",
        "khach dua",
        "tien thoi",
        "tien thua",
        "change",
        "cash",
        "received"
    )
}

data class ReceiptOcrSummary(
    val text: String,
    val isLongReceipt: Boolean,
    val originalLineCount: Int,
    val summaryLineCount: Int
)
