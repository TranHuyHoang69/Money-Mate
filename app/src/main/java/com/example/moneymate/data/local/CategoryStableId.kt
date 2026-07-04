package com.example.moneymate.data.local

import java.util.Locale
import java.util.UUID

object CategoryStableId {
    const val SPEND_FOOD = "spend_food"
    const val SPEND_SHOPPING = "spend_shopping"
    const val SPEND_TRANSPORT = "spend_transport"
    const val INCOME_SALARY = "income_salary"
    const val SPEND_HEALTH = "spend_health"
    const val SPEND_ENTERTAINMENT = "spend_entertainment"
    const val SPEND_COFFEE = "spend_coffee"
    const val SPEND_GIFT = "spend_gift"

    fun ensureStableId(category: CategoryEntity): CategoryEntity {
        if (category.stableId.isNotBlank()) return category

        val stableId = if (category.isDefault) {
            defaultStableId(category.type, category.iconResName) ?: defaultFallbackStableId(
                type = category.type,
                iconResName = category.iconResName
            )
        } else {
            newCustomStableId()
        }

        return category.copy(stableId = stableId)
    }

    fun defaultStableId(type: String, iconResName: String): String? {
        val normalizedType = type.uppercase(Locale.US)
        return when (normalizedType to iconResName) {
            "SPEND" to "ic_food" -> SPEND_FOOD
            "SPEND" to "ic_shop" -> SPEND_SHOPPING
            "SPEND" to "ic_car" -> SPEND_TRANSPORT
            "INCOME" to "ic_money" -> INCOME_SALARY
            "SPEND" to "ic_cat_health_health" -> SPEND_HEALTH
            "SPEND" to "ic_cat_finance_wallet" -> SPEND_ENTERTAINMENT
            "SPEND" to "ic_cat_food_coffee" -> SPEND_COFFEE
            "SPEND" to "ic_cat_shop_gift" -> SPEND_GIFT
            else -> null
        }
    }

    fun newCustomStableId(): String = "custom_${UUID.randomUUID()}"

    fun legacyStableId(categoryId: Long): String = "legacy_category_$categoryId"

    private fun defaultFallbackStableId(type: String, iconResName: String): String {
        val normalizedType = type.lowercase(Locale.US).ifBlank { "category" }
        val normalizedIcon = iconResName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .ifBlank { "default" }
        return "default_${normalizedType}_$normalizedIcon"
    }
}
