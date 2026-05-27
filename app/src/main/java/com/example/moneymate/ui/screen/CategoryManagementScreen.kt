package com.example.moneymate.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import rememberCategoryIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    navController: NavController,
    initialType: String = "SPEND",
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    // Lấy dữ liệu từ database Room thông qua ViewModel
    val allEntities by viewModel.allCategories.collectAsState(initial = emptyList())

    // Chuyển đổi Entity sang Domain Model để vẽ UI
    val allCategories by remember(allEntities) {
        derivedStateOf {
            allEntities.map { entity ->
                Category(
                    id = entity.categoryId,
                    title = entity.title,
                    iconResName = entity.iconResName,
                    colorHex = entity.colorHex,
                    type = if (entity.type.toString().uppercase() == "INCOME") TransactionType.INCOME else TransactionType.SPEND,
                    isDefault = entity.isDefault
                )
            }
        }
    }

    val spendList = remember(allCategories) { allCategories.filter { it.type == TransactionType.SPEND } }
    val incomeList = remember(allCategories) { allCategories.filter { it.type == TransactionType.INCOME } }

    val tabs = listOf("CHI PHÍ", "THU NHẬP")
    val pagerState = rememberPagerState(initialPage = if (initialType == "INCOME") 1 else 0) { tabs.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh mục", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E352F))
            )
        },
        containerColor = Color(0xFF141F1B)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // --- THANH CHUYỂN TAB ---
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF1E352F),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = Color(0xFF4CB080)
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                color = if (pagerState.currentPage == index) Color.White else Color.Gray,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // --- NỘI DUNG VUỐT PAGER ---
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val currentList = if (page == 0) spendList else incomeList
                val currentType = if (page == 0) "SPEND" else "INCOME"

                CategoryGrid(
                    categories = currentList,
                    onCategoryClick = { selectedCategory ->
                        // 🟢 ĐÃ SỬA: Đẩy trực tiếp thông tin phần tử được chọn vào SavedStateHandle của màn AddScreen trước đó
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle["selected_category_id"] = selectedCategory.id
                            handle["selected_category_title"] = selectedCategory.title
                            handle["selected_category_icon"] = selectedCategory.iconResName
                            handle["selected_category_color"] = selectedCategory.colorHex
                        }

                        // Quay trở lại màn hình AddScreen
                        navController.popBackStack()
                    },
                    onAddClick = {
                        navController.navigate("add_category/$currentType")
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    onAddClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            CategoryItemView(
                category = category,
                modifier = Modifier.clickable { onCategoryClick(category) }
            )
        }

        // Nút bấm "Tạo" danh mục mới hình tròn màu vàng cuối lưới
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAddClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFFBC02D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tạo", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CategoryItemView(
    category: Category,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (e: Exception) { Color(0xFF4CB080) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(categoryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = rememberCategoryIcon(category.iconResName),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}