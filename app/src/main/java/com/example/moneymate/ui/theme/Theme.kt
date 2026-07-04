// ui/theme/Theme.kt
package com.example.moneymate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.moneymate.domain.model.UserPreferences

// GIAO DIỆN SÁNG
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CB080),          //  Xanh lá thương hiệu (cho Button, FAB, các điểm nhấn)
    primaryContainer = AppTopBarColor,
    onPrimaryContainer = Color.White,
    background = Color(0xFFF6F6F6),       // Nền màn hình sáng (màu xám trắng dịu mắt hơn trắng tinh)
    surface = Color.White,                // Nền các thẻ Card sáng
    onBackground = Color(0xFF1C1B1F),     // Chữ đen trên nền sáng
    onSurface = Color(0xFF1C1B1F),        // Chữ đen trên Card sáng
    onSurfaceVariant = Color(0xFF757575)  // Chữ phụ màu xám
)

// GIAO DIỆN TỐI (Bỏ biến ngoài, viết cứng mã màu trực tiếp)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CB080),          //  Giữ nguyên màu xanh lá thương hiệu
    primaryContainer = AppTopBarColor,
    onPrimaryContainer = Color.White,
    background = Color(0xFF262620),       // Nền tối xám trầm
    surface = Color(0xFF32322C),          // Nền Card tối
    onBackground = Color(0xFFFFFFFF),     // Chữ trắng trên nền tối
    onSurface = Color(0xFFFFFFFF),        // Chữ trắng trên Card tối
    surfaceVariant = Color(0xFF32322C),   // Bề mặt phụ tối
    onSurfaceVariant = Color(0xFFB3B3B3)  // Chữ phụ màu xám nhạt
)

val LocalUserPreferences = compositionLocalOf { UserPreferences(language = "vi", themeMode = "system") }

@Composable
fun MoneyMateTheme(
    userPreferences: UserPreferences = LocalUserPreferences.current,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (userPreferences.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
