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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.ui.item.IconItem
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

    // TỐI ƯU 1: Định nghĩa danh sách Màu kèm mã Color đã parse sẵn, triệt tiêu việc lặp parse chuỗi bậy bạ
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh mục mới", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // TỐI ƯU 2: Hợp nhất toàn bộ màn hình vào một LazyVerticalGrid duy nhất (Né triệt để lỗi Crash lồng nhau)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6), // Quy chuẩn hàng dọc 6 cột (Bảng màu vừa khít, Icon chiếm span hợp lý)
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
                        label = { Text("Tên danh mục") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                            colors = RadioButtonDefaults.colors(selectedColor = themeColor)
                        )
                        Text("Chi phí", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                        Spacer(modifier = Modifier.width(24.dp))

                        RadioButton(
                            selected = selectedType == "INCOME",
                            onClick = { selectedType = "INCOME" },
                            colors = RadioButtonDefaults.colors(selectedColor = themeColor)
                        )
                        Text("Thu nhập", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }

                // --- TIÊU ĐỀ MÀU SẮC ---
                item(span = { GridItemSpan(6) }, key = "color_title") {
                    Text("Màu sắc chủ đạo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
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
                                color = if (isColorSelected) Color.Black else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }

                // --- TIÊU ĐỀ BIỂU TƯỢNG ---
                item(span = { GridItemSpan(6) }, key = "icon_title") {
                    Text(
                        text = "Biểu tượng danh mục",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                    )
                }

                // TỐI ƯU 3: Triển khai phẳng cấu trúc dữ liệu Icon lồng nhau thông qua cơ chế DSL chuẩn của Compose
                groupedIcons.forEach { (groupName, iconsInGroup) ->
                    // Vẽ Header phân nhóm Icon trải dài hết 6 cột
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

                    // Vẽ các Icon item chiếm tỷ lệ cột tương ứng (Ở đây ta gán mỗi icon chiếm 1 cột trên tổng 6 cột)
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
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        onSave(
                            CategoryEntity(
                                categoryId = 0L,
                                userId = "guest", // 🟢 ĐÃ SỬA: Gán chuỗi tạm để thoả mãn constructor. ViewModel/Repository sẽ tự động override bằng UID thật từ FirebaseAuth
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
                    Text("LƯU DANH MỤC", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}