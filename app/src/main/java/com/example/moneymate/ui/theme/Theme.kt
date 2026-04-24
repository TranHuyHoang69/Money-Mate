package com.example.moneymate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Import các màu bạn đã định nghĩa ở file Color.kt
private val LightColorScheme = lightColorScheme(
    primary = GreenBackground,      // Màu chủ đạo (Xanh lá)
    secondary = TealBackground,     // Màu phụ (Teal)
    tertiary = YellowColor,         // Màu nhấn (Vàng cho FAB)
    background = Color.White,       // Nền các thẻ Card
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = TextBlack,
    onSurface = TextBlack
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenBackground,
    secondary = TealBackground,
    tertiary = YellowColor
)

@Composable
fun MoneyMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Tắt dynamicColor để giữ đúng nhận diện thương hiệu MoneyMate
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Nếu bạn muốn app luôn xanh giống ảnh, hãy ép nó dùng LightColorScheme
        // hoặc thiết lập DarkColorScheme riêng.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}