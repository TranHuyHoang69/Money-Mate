package com.example.moneymate.ui.utils

import com.example.moneymate.R

object IconUtils {
    // Bản đồ dịch tên nhóm từ ID file sang Tiếng Việt
    private val groupNamesVn = mapOf(
        "food" to "Ăn uống",
        "transport" to "Di chuyển",
        "shop" to "Mua sắm",
        "finance" to "Tài chính",
        "health" to "Sức khỏe",
        "ent" to "Giải trí"
    )

    fun getGroupedIcons(): Map<String, List<String>> {
        val grouped = mutableMapOf<String, MutableList<String>>()
        val fields = R.drawable::class.java.fields

        for (field in fields) {
            val name = field.name
            if (name.startsWith("ic_cat_")) {
                val parts = name.split("_")
                if (parts.size >= 4) {
                    val groupKey = parts[2]
                    val displayName = groupNamesVn[groupKey] ?: groupKey.replaceFirstChar { it.uppercase() }

                    if (!grouped.containsKey(displayName)) {
                        grouped[displayName] = mutableListOf()
                    }
                    grouped[displayName]?.add(name)
                }
            }
        }
        return grouped.mapValues { it.value.sorted() }
    }
}