package com.example.moneymate.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import rememberCategoryIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    navController: NavController,
    initialType: String = "SPEND",
    viewModel: CategoryViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit
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

    // ✅ i18n: Chuyển đổi mảng Tab cứng sang danh sách ID chuỗi để hỗ trợ đa ngôn ngữ linh hoạt
    val tabs = remember { listOf(StringRes.expenses, StringRes.income) }
    val pagerState = rememberPagerState(initialPage = if (initialType == "INCOME") 1 else 0) { tabs.size }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.primaryContainer) // ✅ Thay đổi sang màu hệ thống
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(StringRes.menu_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(StringRes.category_management), // ✅ Sửa lỗi compile nhãn id =
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // --- THANH CHUYỂN TAB ---
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceContainer, // ✅ Đồng bộ màu Container của Tab theo chuẩn M3
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary // ✅ Thanh chỉ báo chạy theo màu primary hệ thống
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, resId ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = stringResource(resId).uppercase(), // ✅ Sửa lỗi compile nhãn id =
                                color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thích ứng DarkMode tốt hơn Gray cố định
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
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle["selected_category_id"] = selectedCategory.id
                            handle["selected_category_title"] = selectedCategory.title
                            handle["selected_category_icon"] = selectedCategory.iconResName
                            handle["selected_category_color"] = selectedCategory.colorHex
                        }
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

        // Nút bấm "Tạo" danh mục mới
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAddClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), // ✅ Đồng bộ Container nút Add theo M3 thay vì màu vàng cứng
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(StringRes.add_category), // ✅ Sửa lỗi compile nhãn id =
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(StringRes.create), // ✅ Sửa lỗi compile nhãn id =
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun CategoryItemView(
    category: Category,
    modifier: Modifier = Modifier
) {
    val defaultCategoryColor = MaterialTheme.colorScheme.primary

    val categoryColor = remember(category.colorHex, defaultCategoryColor) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (e: Exception) { defaultCategoryColor }
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
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}