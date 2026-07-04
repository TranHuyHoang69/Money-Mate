package com.example.moneymate.util

import java.text.Normalizer
import java.util.Locale

object CategorySuggestionEngine {
    fun suggestCategory(
        text: String?,
        availableCategories: List<String> = emptyList()
    ): String {
        val categories = availableCategories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
            .ifEmpty { defaultFallbackCategories }
        val normalized = normalize(text)

        if (normalized.isBlank()) return fallbackCategory(categories)

        val bestMatch = categories
            .map { category ->
                CategoryScore(
                    title = category,
                    score = calculateScore(
                        normalizedText = normalized,
                        normalizedCategory = normalize(category)
                    )
                )
            }
            .maxWithOrNull(compareBy<CategoryScore> { it.score }.thenBy { it.title.length })

        return if (bestMatch != null && bestMatch.score >= MIN_MATCH_SCORE) {
            bestMatch.title
        } else {
            fallbackCategory(categories)
        }
    }

    private fun calculateScore(
        normalizedText: String,
        normalizedCategory: String
    ): Int {
        var score = 0

        if (normalizedCategory.contains("qua tang") &&
            !normalizedText.containsAny(giftReceiptKeywords)
        ) {
            return 0
        }

        if (normalizedCategory.contains("an uong") &&
            normalizedText.containsAny(foodReceiptKeywords)
        ) {
            score += FOOD_CATEGORY_STRONG_SCORE
        }

        if (normalizedCategory.isNotBlank() && normalizedText.contains(normalizedCategory)) {
            score += DIRECT_CATEGORY_MATCH_SCORE
        }

        val categoryTokens = normalizedCategory
            .split(" ")
            .filter { it.length >= MIN_TOKEN_LENGTH }
        val textTokens = normalizedText
            .split(" ")
            .filter { it.isNotBlank() }

        score += categoryTokens.count { it in textTokens } * CATEGORY_TOKEN_MATCH_SCORE

        semanticGroups.forEach { group ->
            val categoryMatchesGroup = normalizedCategory.containsAny(group.categoryHints)
            if (categoryMatchesGroup) {
                score += normalizedText.countMatches(group.receiptKeywords) * SEMANTIC_KEYWORD_SCORE
                if (normalizedText.containsAny(group.strongReceiptKeywords)) {
                    score += STRONG_KEYWORD_BONUS
                }
            }
        }

        return score
    }

    private fun fallbackCategory(categories: List<String>): String {
        return categories.firstOrNull { category ->
            val normalized = normalize(category)
            val tokens = normalized.split(" ")
            normalized.contains("an uong") ||
                tokens.any { it in foodFallbackTokens }
        } ?: categories.firstOrNull() ?: DEFAULT_FALLBACK_CATEGORY
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it) }
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private fun String.countMatches(keywords: List<String>): Int {
        return keywords.count { contains(it) }
    }

    private fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val noVietnameseD = value
            .lowercase(Locale.ROOT)
            .replace('đ', 'd')
        return Normalizer.normalize(noVietnameseD, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private const val DEFAULT_FALLBACK_CATEGORY = "Ăn uống"
    private const val MIN_MATCH_SCORE = 30
    private const val DIRECT_CATEGORY_MATCH_SCORE = 100
    private const val CATEGORY_TOKEN_MATCH_SCORE = 20
    private const val SEMANTIC_KEYWORD_SCORE = 25
    private const val STRONG_KEYWORD_BONUS = 30
    private const val FOOD_CATEGORY_STRONG_SCORE = 120
    private const val MIN_TOKEN_LENGTH = 3

    private val defaultFallbackCategories = listOf(
        "Ăn uống",
        "Cafe",
        "Di chuyển",
        "Mua sắm",
        "Sức khoẻ"
    )

    private val foodFallbackTokens = setOf("an", "uong", "food", "meal")

    private val foodReceiptKeywords = listOf(
        "nha hang",
        "quan an",
        "mon",
        "com",
        "bun",
        "pho",
        "banh mi",
        "hai san",
        "bia",
        "restaurant",
        "food"
    )

    private val giftReceiptKeywords = listOf(
        "qua tang",
        "gift",
        "sinh nhat",
        "birthday",
        "hoa tuoi",
        "shop hoa",
        "do choi",
        "luu niem"
    )

    private val semanticGroups = listOf(
        SemanticKeywordGroup(
            categoryHints = listOf(
                "suc khoe",
                "y te",
                "thuoc",
                "benh vien",
                "phong kham",
                "duoc"
            ),
            receiptKeywords = listOf(
                "nha thuoc",
                "long chau",
                "pharmacity",
                "pharmacy",
                "drugstore",
                "thuoc",
                "benh vien",
                "phong kham",
                "clinic",
                "y te",
                "duoc"
            ),
            strongReceiptKeywords = listOf("nha thuoc", "long chau", "pharmacity")
        ),
        SemanticKeywordGroup(
            categoryHints = listOf(
                "an uong",
                "an",
                "uong",
                "nha hang",
                "quan an",
                "com",
                "food",
                "meal",
                "bua"
            ),
            receiptKeywords = listOf(
                "restaurant",
                "nha hang",
                "quan an",
                "quan com",
                "com",
                "bun",
                "pho",
                "banh mi",
                "food",
                "meal",
                "milk tea",
                "tra sua",
                "pizza",
                "kfc",
                "lotteria",
                "jollibee"
            ),
            strongReceiptKeywords = listOf("nha hang", "quan an", "quan com")
        ),
        SemanticKeywordGroup(
            categoryHints = listOf(
                "cafe",
                "coffee",
                "ca phe",
                "tra",
                "do uong"
            ),
            receiptKeywords = listOf(
                "cafe",
                "coffee",
                "ca phe",
                "highlands",
                "phuc long",
                "starbucks",
                "the coffee house",
                "cong ca phe",
                "tra sua"
            ),
            strongReceiptKeywords = listOf("highlands", "phuc long", "the coffee house")
        ),
        SemanticKeywordGroup(
            categoryHints = listOf(
                "di chuyen",
                "taxi",
                "xang",
                "xe",
                "grab",
                "gojek",
                "be",
                "transport"
            ),
            receiptKeywords = listOf(
                "grab",
                "taxi",
                "xang",
                "petrolimex",
                "be bike",
                "be car",
                "gojek",
                "go-viet",
                "bus",
                "xe buyt",
                "giu xe",
                "parking"
            ),
            strongReceiptKeywords = listOf("grab", "taxi", "petrolimex")
        ),
        SemanticKeywordGroup(
            categoryHints = listOf(
                "mua sam",
                "sieu thi",
                "shopping",
                "bach hoa",
                "tap hoa",
                "market"
            ),
            receiptKeywords = listOf(
                "winmart",
                "coopmart",
                "co opmart",
                "sieu thi",
                "bach hoa",
                "big c",
                "go!",
                "lotte mart",
                "aeon",
                "emart",
                "gs25",
                "circle k",
                "familymart",
                "ministop"
            ),
            strongReceiptKeywords = listOf("winmart", "coopmart", "co opmart", "sieu thi")
        ),
        SemanticKeywordGroup(
            categoryHints = listOf(
                "giai tri",
                "phim",
                "cinema",
                "game",
                "music"
            ),
            receiptKeywords = listOf(
                "cinema",
                "cgv",
                "lotte cinema",
                "galaxy cinema",
                "bhd",
                "movie",
                "game",
                "karaoke",
                "spotify",
                "netflix"
            ),
            strongReceiptKeywords = listOf("cgv", "lotte cinema", "galaxy cinema")
        )
    )

    private data class SemanticKeywordGroup(
        val categoryHints: List<String>,
        val receiptKeywords: List<String>,
        val strongReceiptKeywords: List<String>
    )

    private data class CategoryScore(
        val title: String,
        val score: Int
    )
}
