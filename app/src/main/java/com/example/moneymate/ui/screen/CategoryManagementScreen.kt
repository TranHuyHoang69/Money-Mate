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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.viewmodel.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CategoryManagementScreen(
    navController: NavController,
    type: String, // Nhận vào: "SPEND" hoặc "INCOME" (hoặc "CHI PHÍ" / "THU NHẬP")
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.getCategoriesByType(type).collectAsState(initial = emptyList())
    val context = LocalContext.current

    // TỐI ƯU 1: Đồng bộ chuẩn hóa dữ liệu đầu vào tránh lỗi lệch màu
    val isSpend = remember(type) {
        type.contains("SPEND", ignoreCase = true) || type.contains("CHI", ignoreCase = true)
    }
    val themeColor = remember(isSpend) { if (isSpend) Color(0xFF4B8361) else Color(0xFF2E5B8B) }
    val displayName = remember(isSpend) { if (isSpend) "CHI PHÍ" else "THU NHẬP" }

    // State lưu danh sách đã được phân giải sẵn ID của Icon ở luồng nền
    var optimizedCategories by remember { mutableStateOf<List<Pair<CategoryEntity, Int>>>(emptyList()) }

    // TỐI ƯU 2: Phân giải tên Icon (String) sang Res ID (Int) ở Background Thread
    LaunchedEffect(categories) {
        withContext(Dispatchers.Default) {
            categories.map { category ->
                val resId = context.resources.getIdentifier(
                    category.iconResName,
                    "drawable",
                    context.packageName
                )
                Pair(category, resId)
            }
        }.let {
            optimizedCategories = it
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Quản lý danh mục $displayName", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_category/$type") },
                containerColor = themeColor
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        if (optimizedCategories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có danh mục nào cho $displayName", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // TỐI ƯU 3: Thêm key cố định từ ID thực tế của Database
                items(
                    items = optimizedCategories,
                    key = { pair -> pair.first.categoryId }
                ) { (category, resId) ->
                    CategoryItemForEntity(
                        category = category,
                        resId = resId,
                        themeColor = themeColor,
                        onClick = { /* Xử lý edit/delete tại đây */ }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItemForEntity(
    category: CategoryEntity,
    resId: Int,
    themeColor: Color,
    onClick: () -> Unit
) {
    // TỐI ƯU 4: Cache mã màu Hex bằng remember để loại bỏ việc parse chuỗi liên tục khi cuộn
    val itemColor = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex))
        } catch (e: Exception) {
            themeColor
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(itemColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    tint = itemColor,
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