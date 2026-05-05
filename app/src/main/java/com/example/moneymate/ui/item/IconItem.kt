package com.example.moneymate.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun IconItem(
    iconName: String,
    isSelected: Boolean,
    currentColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resId = remember(iconName) {
        context.resources.getIdentifier(iconName, "drawable", context.packageName)
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .padding(6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) currentColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) currentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (resId != 0) resId else android.R.drawable.ic_menu_help),
            contentDescription = null,
            tint = if (isSelected) currentColor else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
    }
}