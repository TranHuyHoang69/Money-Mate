package com.example.moneymate.util

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import java.util.Locale

object Localization {
    fun updateLocale(context: Context, languageCode: String): ContextWrapper {
        val locale = Locale.forLanguageTag(languageCode.ifBlank { DEFAULT_LANGUAGE })
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))

        return ContextWrapper(context.createConfigurationContext(configuration))
    }

    private const val DEFAULT_LANGUAGE = "vi"
}
