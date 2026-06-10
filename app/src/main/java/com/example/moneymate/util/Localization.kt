package com.example.moneymate.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object Localization {
    enum class Language(val code: String, val displayName: String) {
        VIETNAMESE("vi", "Tiếng Việt"),
        ENGLISH("en", "English");

        companion object {
            fun fromCode(code: String): Language {
                return entries.find { it.code == code } ?: VIETNAMESE
            }
        }
    }

    fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        configuration.setLayoutDirection(locale)

        // Trả về Context cục bộ mới đã được áp ngôn ngữ đã chọn
        return context.createConfigurationContext(configuration)
    }
}