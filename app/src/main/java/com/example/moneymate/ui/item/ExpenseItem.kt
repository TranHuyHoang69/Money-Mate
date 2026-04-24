package com.example.moneymate.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseItem(
    title: String,
    percent: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier // <-- THÊM DÒNG NÀY
) {
    Row(
        modifier = modifier // <-- GÁN MODIFIER VÀO ĐÂY
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bookmark, null, tint = color, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Text(percent, fontSize = 12.sp, color = Color.Gray)
        }

        Text(
            amount,
            fontWeight = FontWeight.ExtraBold,
            color = if (amount.contains("-")) Color.Red else Color(0xFF333333)
        )
    }
}