package com.example.moneymate.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.ui.chart.MorphingChartSection
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.viewmodel.AuthUiState
import com.example.moneymate.viewmodel.AuthViewModel
import com.example.moneymate.viewmodel.CalendarMode
import com.example.moneymate.viewmodel.ChartData
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

data class CategoryGroup(
    val category: com.example.moneymate.domain.model.Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float,
    val singleId: Long
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // --- Data State ---
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val allExpensesResult by homeViewModel.expensesState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    // --- UI State ---
    val scrollState = rememberLazyListState()
    var selectedType by remember { mutableStateOf("CHI PHÍ") }
    val themeColor = if (selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // --- Date Picker State ---
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // --- Logic Tính Toán Số Dư (Nên đưa vào ViewModel nhưng tối ưu tạm bằng remember) ---
    val totalBalance = remember(allExpensesResult) {
        if (allExpensesResult is Result.Success) {
            homeViewModel.calculateTotalBalance((allExpensesResult as Result.Success<List<Expense>>).data)
        } else 0.0
    }

    // --- Logic Xử lý Danh sách (Loại bỏ Reflection) ---
    val categoryGroupedList = remember(expensesByPeriod, selectedType) {
        val typeEnum = if (selectedType == "CHI PHÍ")
            com.example.moneymate.domain.model.TransactionType.SPEND
        else
            com.example.moneymate.domain.model.TransactionType.INCOME

        val filtered = expensesByPeriod.filter { it.type == typeEnum }
        val totalAmountOfPeriod = filtered.sumOf { it.amount }

        filtered.groupBy { it.category.id }.map { (_, items) ->
            val firstItem = items.first()
            val groupSum = items.sumOf { it.amount }
            CategoryGroup(
                category = firstItem.category,
                totalAmount = groupSum,
                transactionCount = items.size,
                percentage = if (totalAmountOfPeriod > 0) (groupSum / totalAmountOfPeriod * 100).toFloat() else 0f,
                singleId = firstItem.id
            )
        }.sortedByDescending { it.totalAmount }
    }

    // Dữ liệu cho Chart lấy trực tiếp từ CategoryGroup
    val chartData = remember(categoryGroupedList) {
        categoryGroupedList.map {
            ChartData(it.category.title, it.totalAmount, it.percentage, it.category.colorHex)
        }
    }

    // --- Hiệu ứng Morphing ---
    val morphProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset / 400f).coerceIn(0f, 1f)
        }
    }
    val dynamicChartHeight = (180 - (120 * morphProgress)).dp

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        historyViewModel.loadCustomRange(start, end)
                    }
                    showDatePicker = false
                }) {
                    Text("Xác nhận", color = themeColor, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Box(modifier = Modifier.height(450.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = { Text("Chọn khoảng thời gian", Modifier.padding(16.dp)) }
                )
            }
        }
    }

    // --- UI STRUCTURE ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                authUiState = authUiState,
                totalBalance = totalBalance,
                themeColor = themeColor,
                onLogout = { authViewModel.logout() },
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(themeColor)) {
            HeaderSection(
                selectedType = selectedType,
                totalBalance = totalBalance,
                onTabSelected = { selectedType = it },
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 250.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFFF8F9FA))
            ) {
                TimeNavigationHeader(
                    viewModel = historyViewModel,
                    themeColor = themeColor,
                    onCustomRangeClick = { showDatePicker = true }
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(180.dp)) }

                        if (isLoading) {
                            item { LoadingUI(themeColor) }
                        } else if (categoryGroupedList.isEmpty()) {
                            item { EmptyStateSection() }
                        } else {
                            items(categoryGroupedList, key = { it.category.id }) { group ->
                                ExpenseListItem(
                                    group = group,
                                    selectedType = selectedType,
                                    onClick = {
                                        val route = if (group.transactionCount > 1)
                                            "grouped_expense/${group.category.title}/${group.totalAmount}"
                                        else
                                            "detail_expense/${group.singleId}"
                                        navController.navigate(route)
                                    }
                                )
                            }
                        }
                    }

                    MorphingChartSection(
                        chartData = chartData,
                        morphProgress = morphProgress,
                        dynamicHeight = dynamicChartHeight
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    if (authUiState.isLoggedIn) navController.navigate("add")
                    else navController.navigate(Screen.Login.route)
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFFFFC107),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun HomeDrawerContent(
    authUiState: AuthUiState,
    totalBalance: Double,
    themeColor: Color,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val isLoggedIn = authUiState.isLoggedIn
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(24.dp)) {
            Box(Modifier.size(64.dp).background(themeColor.copy(0.1f), CircleShape), Alignment.Center) {
                val initial = if(isLoggedIn) authUiState.user?.userName?.take(1) ?: "U" else "?"
                Text(initial, fontWeight = FontWeight.Bold, color = themeColor)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if(isLoggedIn) authUiState.user?.userName ?: "Người dùng" else "Chế độ khách",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if(isLoggedIn){
                Text("${String.format("%,.0f",totalBalance)} đ", color = if(totalBalance > 0) themeColor else Color.Red)
            }else{
                Text("Đăng nhập để đồng bộ dữ liệu", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Lịch sử") },
            selected = false,
            icon = { Icon(Icons.Default.History, null) },
            onClick = {
                if(authUiState.isLoggedIn) onNavigate("history-all")
                else onNavigate(Screen.Login.route)
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        if(isLoggedIn){
            NavigationDrawerItem(
                label = { Text("Đăng xuất") },
                selected = false,
                icon = { Icon(Icons.Default.Logout, null) },
                onClick = onLogout,
                colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = Color.Red, unselectedTextColor = Color.Red),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }else{
            Column(modifier = Modifier.padding(16.dp)){
                NavigationDrawerItem(
                    label = {Text("Đăng nhập")},
                    selected = false,
                    onClick = {onNavigate(Screen.Login.route)},
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = {Text("Đăng ký")},
                    selected = false,
                    onClick = {onNavigate(Screen.Register.route)},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    }
}

@Composable
fun HeaderSection(selectedType: String, totalBalance: Double, onTabSelected: (String) -> Unit, onMenuClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp)) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null, tint = Color.White) }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TỔNG SỐ DƯ", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("${String.format("%,.0f", totalBalance)} đ", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(0.1f)).padding(4.dp)) {
            listOf("CHI PHÍ", "THU NHẬP").forEach { title ->
                val isSelected = selectedType == title
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.2f) else Color.Transparent).clickable { onTabSelected(title) }.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TimeNavigationHeader(viewModel: HistoryViewModel, themeColor: Color, onCustomRangeClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CalendarMode.entries.forEach { mode ->
                val isSelected = viewModel.calendarMode == mode
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { if (mode == CalendarMode.CUSTOM) onCustomRangeClick() else viewModel.changeMode(mode) }.padding(vertical = 4.dp)) {
                    Text(text = when (mode) {
                        CalendarMode.DAY -> "Ngày"
                        CalendarMode.WEEK -> "Tuần"
                        CalendarMode.MONTH -> "Tháng"
                        CalendarMode.YEAR -> "Năm"
                        CalendarMode.CUSTOM -> "Khoảng"
                    }, color = if (isSelected) themeColor else Color.LightGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    if (isSelected) Box(Modifier.padding(top = 2.dp).size(16.dp, 2.dp).background(themeColor, CircleShape))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val isCustom = viewModel.calendarMode == CalendarMode.CUSTOM
            IconButton(onClick = { viewModel.movePrevious() }, enabled = !isCustom) { Icon(Icons.Default.ChevronLeft, null, tint = if(isCustom) Color.LightGray else themeColor) }
            Text(text = viewModel.getDisplayTime(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
            val canNext = viewModel.isNextEnabled() && !isCustom
            IconButton(onClick = { viewModel.moveNext() }, enabled = canNext) { Icon(Icons.Default.ChevronRight, null, tint = if (canNext) themeColor else themeColor.copy(0.3f)) }
        }
    }
}

@Composable
fun ExpenseListItem(group: CategoryGroup, selectedType: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        ExpenseItem(
            title = group.category.title,
            percent = "${group.transactionCount} giao dịch (${String.format("%.1f", group.percentage)}%)",
            amount = "${if (selectedType == "CHI PHÍ") "-" else "+"} ${String.format("%,.0f", group.totalAmount)} đ",
            color = Color(android.graphics.Color.parseColor(group.category.colorHex))
        )
    }
}

@Composable
fun LoadingUI(color: Color) { Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) { CircularProgressIndicator(color = color) } }

@Composable
fun EmptyStateSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
        Text("Không có giao dịch nào", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
    }
}