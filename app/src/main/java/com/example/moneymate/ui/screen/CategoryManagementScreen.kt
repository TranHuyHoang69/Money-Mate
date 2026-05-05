package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.viewmodel.CategoryViewModel

@Composable
fun CategoryManagementScreen(
    navController: NavController,
    type: String, // Đảm bảo truyền vào đúng "CHI PHÍ" hoặc "THU NHẬP"
    viewModel: CategoryViewModel = hiltViewModel()
) {
    // Lọc dữ liệu ngay từ ViewModel dựa trên type truyền vào
    val categories by viewModel.getCategoriesByType(type).collectAsState(initial = emptyList())

    val isSpend = type == "SPEND"
    val themeColor = if (isSpend) Color(0xFF4B8361) else Color(0xFF2E5B8B)
    val displayName = if (isSpend) "CHI PHÍ" else "THU NHẬP"
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Quản lý danh mục $type", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Chuyển hướng sang màn hình thêm mới thay vì bật Dialog
                    navController.navigate("add_category/$type")
                },
                containerColor = themeColor
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có danh mục nào cho $type")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(categories) { category ->
                    CategoryItemForEntity(
                        category = category,
                        themeColor = themeColor,
                        onClick = { /* Xử lý edit/delete nếu cần */ }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItemForEntity(
    category: CategoryEntity,
    themeColor: Color,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Tìm ID của icon từ tên String lưu trong DB
    val resId = remember(category.iconResName) {
        context.resources.getIdentifier(category.iconResName, "drawable", context.packageName)
    }

    val itemColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        themeColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp) // Kích thước chuẩn
                .clip(RoundedCornerShape(16.dp))
                .background(itemColor.copy(alpha = 0.1f)), // Màu nền nhạt
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    tint = itemColor, // Đổ màu icon theo màu người dùng chọn
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = itemColor)
            }
        }
        Text(
            text = category.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}