package com.example.moneymate.util

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCategorySuggestionService @Inject constructor() {
    suspend fun suggestCategory(
        rawText: String,
        merchantName: String?,
        localCategory: String,
        availableCategories: List<String>
    ): String? {
        val categories = sanitizeCategories(availableCategories)
        if (rawText.isBlank() || categories.isEmpty()) return null

        return null
    }

    internal fun shouldAcceptSuggestedCategory(
        suggestedCategory: String,
        rawText: String,
        merchantName: String?,
        localCategory: String
    ): Boolean {
        val normalizedSuggested = normalize(suggestedCategory)
        val normalizedLocal = normalize(localCategory)
        val normalizedContext = normalize(
            listOfNotNull(merchantName, localCategory, rawText).joinToString(" ")
        )

        if (normalizedSuggested.contains("qua tang") &&
            !normalizedContext.containsAny(giftKeywords)
        ) {
            return false
        }

        if (normalizedContext.containsAny(foodReceiptSignals) &&
            normalizedLocal.containsAny(foodCategorySignals) &&
            normalizedSuggested.contains("qua tang")
        ) {
            return false
        }

        return true
    }

    private fun buildRequestBody(
        rawText: String,
        merchantName: String?,
        localCategory: String,
        availableCategories: List<String>
    ): JSONObject {
        val prompt = buildPrompt(
            rawText = rawText,
            merchantName = merchantName,
            localCategory = localCategory,
            availableCategories = availableCategories
        )

        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0)
                    .put("maxOutputTokens", 128)
            )
    }

    internal fun buildPrompt(
        rawText: String,
        merchantName: String?,
        localCategory: String,
        availableCategories: List<String>
    ): String {
        val ocrSummary = ReceiptOcrSummaryExtractor.extract(rawText)
        val ocrInputLabel = if (ocrSummary.isLongReceipt) {
            "Raw OCR đã rút gọn từ hóa đơn dài"
        } else {
            "Raw OCR"
        }

        return """
            Bạn là hệ thống phân loại danh mục chi tiêu cho ứng dụng MoneyMate.

            Nhiệm vụ:
            Chọn đúng MỘT danh mục phù hợp nhất cho hóa đơn.
            Chỉ được chọn category nằm trong availableCategories.
            Không được tự tạo category mới.
            Không được trả về category ngoài danh sách.

            Dữ liệu:
            merchantName:
            ${merchantName.orEmpty()}

            localSuggestion:
            $localCategory

            availableCategories:
            ${availableCategories.joinToString("\n") { "- $it" }}

            $ocrInputLabel:
            ${ocrSummary.text.take(MAX_RAW_TEXT_CHARS)}

            Quy tắc:
            1. Nếu OCR có tín hiệu nhà hàng, quán ăn, món ăn, đồ uống, bàn, món, bia, nước, hải sản, cơm, phở, bún, cafe, restaurant, food thì ưu tiên category thuộc nhóm ăn uống/cafe nếu có.
            2. Nếu OCR có tín hiệu siêu thị, WinMart, Coopmart, Bách Hóa, cửa hàng tiện lợi, shopping, supermarket thì ưu tiên nhóm mua sắm/siêu thị.
            3. Nếu OCR có tín hiệu taxi, Grab, xăng, bãi xe, vé xe, fuel, transport thì ưu tiên nhóm di chuyển/xăng xe.
            4. Nếu OCR có tín hiệu nhà thuốc, bệnh viện, phòng khám, thuốc, pharmacy, clinic thì ưu tiên nhóm sức khỏe/y tế.
            5. Chỉ chọn "Quà tặng" nếu OCR có tín hiệu rõ ràng như quà tặng, gift, sinh nhật, hoa, thiệp, đồ chơi, lưu niệm. Không chọn "Quà tặng" chỉ vì không chắc chắn.
            6. Không dùng số điện thoại, ngày tháng, mã hóa đơn, mã giao dịch hoặc số tiền để suy luận danh mục.
            7. Nếu localSuggestion hợp lý và không mâu thuẫn với OCR, hãy giữ localSuggestion.
            8. Nếu không chắc chắn, chọn localSuggestion nếu nó nằm trong availableCategories. Nếu không, chọn category phù hợp nhất trong availableCategories.

            Output:
            Chỉ trả về JSON hợp lệ, không giải thích ngoài JSON.

            Format:
            {
              "category": "category đúng y hệt trong availableCategories",
              "confidence": 0.0,
              "reason": "lý do ngắn"
            }
        """.trimIndent()
    }

    internal fun parseCategoryFromResponse(
        responseText: String,
        availableCategories: List<String>
    ): String? {
        val text = extractFirstTextPart(responseText)
        val categoryText = extractCategoryFromModelText(text)
        val normalizedText = normalize(categoryText)
        return sanitizeCategories(availableCategories).firstOrNull { category ->
            normalizedText == normalize(category)
        }
    }

    private fun extractFirstTextPart(responseText: String): String {
        val jsonText = runCatching {
            JSONObject(responseText)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        }.getOrNull().orEmpty()

        if (jsonText.isNotBlank()) return jsonText

        return textPartRegex.find(responseText)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJsonText()
            .orEmpty()
    }

    private fun extractCategoryFromModelText(text: String): String {
        val cleaned = text
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonCategory = categoryFieldRegex.find(cleaned)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJsonText()
            ?.trim()
            .orEmpty()

        return jsonCategory.ifBlank { cleaned }
    }

    private fun String.unescapeJsonText(): String {
        return replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
    }

    private fun sanitizeCategories(categories: List<String>): List<String> {
        return categories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
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

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private companion object {
        const val MAX_RAW_TEXT_CHARS = 3_000

        val textPartRegex = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val categoryFieldRegex = Regex(""""category"\s*:\s*"((?:\\.|[^"\\])*)"""")

        val giftKeywords = listOf(
            "qua tang",
            "gift",
            "sinh nhat",
            "birthday",
            "hoa tuoi",
            "shop hoa",
            "do choi",
            "luu niem"
        )

        val foodReceiptSignals = listOf(
            "nha hang",
            "quan an",
            "mon",
            "com",
            "bun",
            "pho",
            "banh mi",
            "hai san",
            "bia",
            "cafe",
            "coffee",
            "tra sua",
            "restaurant",
            "food"
        )

        val foodCategorySignals = listOf(
            "an uong",
            "an",
            "uong",
            "food",
            "meal",
            "nha hang",
            "cafe"
        )
    }
}
