package com.example.moneymate.util

import java.text.Normalizer
import java.util.Locale

object ReceiptFingerprintUtil {
    private val noiseWords = setOf(
        "ngay",
        "date",
        "gio",
        "time",
        "thang",
        "thg",
        "nam",
        "ma",
        "don",
        "hang",
        "hoa",
        "don",
        "hotline",
        "dt",
        "sdt",
        "tong",
        "tien",
        "thanh",
        "toan",
        "phai",
        "tra",
        "khach",
        "dua",
        "thua",
        "giam",
        "total",
        "amount",
        "cash",
        "change",
        "vnd"
    )

    fun create(rawText: String, merchantName: String?): String {
        val merchantTokens = merchantName.orEmpty()
            .normalizeForFingerprint()
            .toTokens()

        val contentTokens = rawText
            .normalizeForFingerprint()
            .removeReceiptNoise()
            .toTokens()
            .filterNot { it in noiseWords }
            .filterNot { token -> token.all(Char::isDigit) }

        return (merchantTokens + contentTokens)
            .distinct()
            .take(MAX_TOKENS)
            .joinToString("|")
    }

    private fun String.normalizeForFingerprint(): String {
        val noVietnameseD = lowercase(Locale.ROOT)
            .replace('đ', 'd')
            .replace('Đ', 'd')
        return Normalizer.normalize(noVietnameseD, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }

    private fun String.removeReceiptNoise(): String {
        return this
            .replace("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b".toRegex(), " ")
            .replace("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b".toRegex(), " ")
            .replace("\\b\\d{1,2}\\s*(thang|thg|t)\\.?\\s*\\d{1,2}(\\s*nam\\s*\\d{2,4})?\\b".toRegex(), " ")
            .replace("\\b\\d{1,2}:\\d{2}(:\\d{2})?\\b".toRegex(), " ")
            .replace("\\b\\d{1,3}([., ]\\d{3})+(₫|d|vnd|dong)?\\b".toRegex(), " ")
            .replace("\\b\\d{5,}\\b".toRegex(), " ")
    }

    private fun String.toTokens(): List<String> {
        return replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.length >= MIN_TOKEN_LENGTH }
    }

    private const val MIN_TOKEN_LENGTH = 2
    private const val MAX_TOKENS = 32
}
