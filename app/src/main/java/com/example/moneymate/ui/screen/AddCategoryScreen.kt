package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.StringRes
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.ui.item.IconItem
import com.example.moneymate.ui.theme.AppTopBarColor
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.ui.utils.IconUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    initialType: String,
    onBack: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedColor by remember { mutableStateOf("#4B8361") }

    val groupedIcons = remember { IconUtils.getGroupedIcons() }
    var selectedIcon by remember {
        mutableStateOf(groupedIcons.values.firstOrNull()?.firstOrNull() ?: "")
    }

    val colorPalette = remember {
        listOf(
            "#4B8361", "#2E5B8B", "#FFC107", "#E91E63", "#9C27B0", "#00BCD4",
            "#FF5722", "#795548", "#607D8B", "#8BC34A", "#3F51B5", "#FF9800"
        ).map { hex ->
            Pair(hex, Color(android.graphics.Color.parseColor(hex)))
        }
    }

    val themeColor = remember(selectedColor) {
        try { Color(android.graphics.Color.parseColor(selectedColor)) }
        catch (e: Exception) { Color(0xFF4B8361) }
    }

    // Tự động tính toán màu chữ tương phản dựa trên màu nền của TopAppBar (Tránh chữ trắng trên nền vàng nhạt)
    val appBarContentColor = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(StringRes.new_category), // ✅ Loại bỏ 'id =' để khớp signature mới
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = appBarContentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = appBarContentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTopBarColor) // ✅ Đồng bộ màu động theo danh mục
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                // --- Ô NHẬP TÊN DANH MỤC ---
                item(span = { GridItemSpan(6) }, key = "input_field") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = stringResource(StringRes.category_name_label)) }, // ✅ Fix compile
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedLabelColor = themeColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }

                // --- TAB CHỌN LOẠI THU / CHI ---
                item(span = { GridItemSpan(6) }, key = "type_radio_buttons") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == "SPEND",
                            onClick = { selectedType = "SPEND" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = themeColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = stringResource(StringRes.expenses), // ✅ Fix compile
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        RadioButton(
                            selected = selectedType == "INCOME",
                            onClick = { selectedType = "INCOME" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = themeColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = stringResource(StringRes.income), // ✅ Fix compile
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // --- TIÊU ĐỀ MÀU SẮC ---
                item(span = { GridItemSpan(6) }, key = "color_title") {
                    Text(
                        text = stringResource(StringRes.theme_settings), // ✅ Fix compile
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // --- BẢNG HIỂN THỊ MÀU (6 Cột) ---
                items(
                    items = colorPalette,
                    key = { pair -> pair.first }
                ) { (colorHex, composeColor) ->
                    val isColorSelected = selectedColor == colorHex
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                            .clickable { selectedColor = colorHex }
                            .border(
                                width = if (isColorSelected) 3.dp else 0.dp,
                                color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }

                // --- TIÊU ĐỀ BIỂU TƯỢNG ---
                item(span = { GridItemSpan(6) }, key = "icon_title") {
                    Text(
                        text = stringResource(StringRes.category_management), // ✅ Fix compile
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                    )
                }

                // Cấu trúc dữ liệu phân nhóm Icon phẳng DSL
                groupedIcons.forEach { (groupName, iconsInGroup) ->
                    item(
                        span = { GridItemSpan(6) },
                        key = "header_group_$groupName"
                    ) {
                        Text(
                            text = groupName,
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            fontSize = 12.sp
                        )
                    }

                    items(
                        items = iconsInGroup,
                        key = { iconName -> "icon_${groupName}_$iconName" }
                    ) { icon ->
                        Box(
                            modifier = Modifier.padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconItem(
                                iconName = icon,
                                isSelected = selectedIcon == icon,
                                currentColor = themeColor
                            ) {
                                selectedIcon = icon
                            }
                        }
                    }
                }
            }

            // --- NÚT LƯU DANH MỤC ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        onSave(
                            CategoryEntity(
                                categoryId = 0L,
                                userId = "guest",
                                title = name.trim(),
                                iconResName = selectedIcon,
                                colorHex = selectedColor,
                                type = selectedType,
                                isDefault = false
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    enabled = name.isNotBlank() && selectedIcon.isNotBlank()
                ) {
                    Text(
                        text = stringResource(StringRes.save_category), // ✅ Fix compile
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appBarContentColor
                    )
                }
            }
        }
    }
}

