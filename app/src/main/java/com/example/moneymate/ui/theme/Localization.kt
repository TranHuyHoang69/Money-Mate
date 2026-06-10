package com.example.moneymate.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.moneymate.util.Localization

// 🌐 CompositionLocal lưu trữ mã ngôn ngữ hiện tại (mặc định là "vi")
val LocalLanguage = compositionLocalOf { "vi" }

/**
 * ✅ Custom stringResource() không có tham số truyền vào.
 * Giúp lấy tài nguyên chuỗi theo ngôn ngữ động mà không thay đổi LocalContext gốc.
 */
@Composable
fun stringResource(@StringRes resId: Int): String {
    val context = LocalContext.current
    val languageCode = LocalLanguage.current

    // Tạo context tạm thời (Localized Context) để bóc tách dữ liệu ngôn ngữ đúng
    val localizedContext = Localization.updateLocale(context, languageCode)
    return try {
        localizedContext.getString(resId)
    } catch (e: Exception) {
        context.getString(resId) // Fallback về cấu hình mặc định của hệ thống nếu lỗi
    }
}

/**
 * ✅ Custom stringResource() Nạp chồng (Overload) hỗ trợ chuỗi định dạng (Format Arguments).
 * Ví dụ sử dụng: stringResource(R.string.welcome_user, username) -> "Xin chào, Nam!"
 */
@Composable
fun stringResource(@StringRes resId: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val languageCode = LocalLanguage.current

    val localizedContext = Localization.updateLocale(context, languageCode)
    return try {
        localizedContext.getString(resId, *formatArgs)
    } catch (e: Exception) {
        try {
            context.getString(resId, *formatArgs)
        } catch (innerException: Exception) {
            context.getString(resId) // Cứu cánh cuối cùng: Trả về chuỗi gốc không format
        }
    }
}

/**
 * ✅ MoneyMateLocalizationProvider
 * Chiến thuật cốt lõi: CHỈ cung cấp cấu hình LocalLanguage cho cây giao diện (UI Tree).
 * TUYỆT ĐỐI KHÔNG ghi đè LocalContext, giữ Activity Context nguyên bản để Hilt làm việc ổn định.
 */
@Composable
fun MoneyMateLocalizationProvider(
    languageCode: String,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLanguage provides languageCode
    ) {
        content()
    }
}