package com.example.moneymate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinPadComponent(
    currentInput: String,
    onInputChanged: (String) -> Unit
) {
    // Tự động đồng bộ màu sắc với Hệ thống Theme (MoneyMateTheme) thay vì fix cứng Color Hex
    val dotFilledColor = MaterialTheme.colorScheme.primary
    val dotEmptyColor = MaterialTheme.colorScheme.surfaceVariant
    val buttonBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val buttonTextColor = MaterialTheme.colorScheme.onSurface

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // --- HÀNG HIỂN THỊ CHẤM TRÒN MẬT MÃ (PIN DOTS) ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val isFilled = index < currentInput.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) dotFilledColor else dotEmptyColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- GRID PHÍM BẤM SỐ TỪ 1 ĐẾN 9 VÀ CÁC PHÍM CHỨC NĂNG ---
        // Lưu ý: Ký tự xóa "⌫" là ký hiệu biểu tượng phổ thông, không cần dịch đa ngôn ngữ
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (key.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else buttonBackgroundColor)
                            .clickable(enabled = key.isNotEmpty()) {
                                when (key) {
                                    "⌫" -> if (currentInput.isNotEmpty()) onInputChanged(currentInput.dropLast(1))
                                    else -> if (currentInput.length < 4) onInputChanged(currentInput + key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = buttonTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}