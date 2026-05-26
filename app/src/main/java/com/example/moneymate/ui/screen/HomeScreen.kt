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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.model.GroupedExpense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.chart.MorphingChartSection
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.viewmodel.AuthViewModel
import com.example.moneymate.viewmodel.CalendarMode
import com.example.moneymate.viewmodel.ChartData
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit
) {
    // --- Data State ---
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val allExpensesResult by homeViewModel.expensesState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(allExpensesResult, authUiState.isLoggedIn) {
        if (authUiState.isLoggedIn) {
            homeViewModel.reloadExpenses()
        }
    }

    LaunchedEffect(authUiState.isLoggedIn) {
        android.util.Log.d("HomeScreen", "Auth changed: ${authUiState.isLoggedIn}")
        historyViewModel.loadData()
        homeViewModel.reloadExpenses()
    }

    // --- UI State ---
    val scrollState = rememberLazyListState()
    var selectedType by remember { mutableStateOf("CHI PHÍ") }
    val themeColor = remember(selectedType) {
        if (selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)
    }
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // --- State sau tối ưu (Chạy dưới nền) ---
    val totalBalance by homeViewModel.totalBalance.collectAsState()
    var categoryGroupedList by remember { mutableStateOf<List<GroupedExpense>>(emptyList()) }
    var chartData by remember { mutableStateOf<List<ChartData>>(emptyList()) }

    // 1. Xử lý tính toán tổng số dư dưới luồng nền
    LaunchedEffect(expensesByPeriod, selectedType) {
        withContext(Dispatchers.Default) {
            val typeEnum = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
            val filtered = expensesByPeriod.filter { it.type == typeEnum }
            val totalAmountOfPeriod = filtered.sumOf { it.amount }

            val groups = filtered.groupBy { it.category.id }.map { (_, items) ->
                val firstItem = items.first()
                val groupSum = items.sumOf { it.amount }
                GroupedExpense(
                    category = firstItem.category,
                    totalAmount = groupSum,
                    transactionCount = items.size,
                    type = typeEnum
                )
            }.sortedByDescending { it.totalAmount }

            val charts = groups.map {
                val percentage = if (totalAmountOfPeriod > 0) (it.totalAmount / totalAmountOfPeriod * 100).toFloat() else 0f
                ChartData(it.category.title, it.totalAmount, percentage, it.category.colorHex)
            }
            Pair(groups, charts)
        }.let { (groups, charts) ->
            categoryGroupedList = groups
            chartData = charts
        }
    }

    // 2. Xử lý thuật toán map/group dữ liệu danh mục dưới luồng nền dựa trên GroupedExpense
    LaunchedEffect(expensesByPeriod, selectedType) {
        withContext(Dispatchers.Default) {
            val typeEnum = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
            val filtered = expensesByPeriod.filter { it.type == typeEnum }
            val totalAmountOfPeriod = filtered.sumOf { it.amount }

            val groups = filtered.groupBy { it.category.id }.map { (_, items) ->
                val firstItem = items.first()
                val groupSum = items.sumOf { it.amount }
                GroupedExpense(
                    category = firstItem.category,
                    totalAmount = groupSum,
                    transactionCount = items.size,
                    type = typeEnum
                )
            }.sortedByDescending { it.totalAmount }

            val charts = groups.map {
                val percentage = if (totalAmountOfPeriod > 0) (it.totalAmount / totalAmountOfPeriod * 100).toFloat() else 0f
                ChartData(it.category.title, it.totalAmount, percentage, it.category.colorHex)
            }
            Pair(groups, charts)
        }.let { (groups, charts) ->
            categoryGroupedList = groups
            chartData = charts
        }
    }

    // 3. Cô lập tính toán tiến trình Morph động khi cuộn
    val morphProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (scrollState.firstVisibleItemScrollOffset / 400f).coerceIn(0f, 1f)
            }
        }
    }
    val dynamicChartHeight = remember(morphProgress) { (180 - (120 * morphProgress)).dp }

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

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(themeColor)) {
        HeaderSection(
            selectedType = selectedType,
            totalBalance = totalBalance,
            onTabSelected = { selectedType = it },
            onMenuClick = onOpenDrawer
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
                    item(contentType = "Spacer") { Spacer(modifier = Modifier.height(180.dp)) }

                    if (isLoading) {
                        item(contentType = "Loading") { LoadingUI(themeColor) }
                    } else if (categoryGroupedList.isEmpty()) {
                        item(contentType = "Empty") { EmptyStateSection() }
                    } else {
                        // ✅ ĐÃ CHỈNH SỬA TOÀN BỘ KHỐI ITEMS SẠCH SẼ, KHÔNG CÒN NGOẶC THỪA
                        items(
                            items = categoryGroupedList,
                            key = { it.category.id },
                            contentType = { "ExpenseItem" }
                        ) { group ->
                            ExpenseListItem(
                                group = group,
                                selectedType = selectedType,
                                onClick = {
                                    if (group.transactionCount > 1) {
                                        val currentModeName = historyViewModel.calendarMode.name
                                        val currentStartTimestamp = historyViewModel.currentStartTimePeriod
                                        val currentEndTimestamp = historyViewModel.currentEndTimePeriod
                                        val encodedAmount = group.totalAmount.toFloat()

                                        navController.navigate(
                                            "grouped_expense/${group.category.title}/$encodedAmount/$currentModeName/$currentStartTimestamp/$currentEndTimestamp"
                                        )
                                    } else {
                                        // Hoàn thiện nhánh click xem chi tiết khi chỉ có đúng 1 giao dịch độc nhất
                                        val currentTypeEnum = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
                                        val singleTransaction = expensesByPeriod.find {
                                            it.category.id == group.category.id && it.type == currentTypeEnum
                                        }
                                        val docId = singleTransaction?.firestoreDocId ?: ""
                                        if (docId.isNotEmpty()) {
                                            navController.navigate("detail_expense/$docId")
                                        } else {
                                            android.util.Log.e("HomeScreen", "Không tìm thấy mã chuỗi firestoreDocId!")
                                        }
                                    }
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

@Composable
fun HeaderSection(selectedType: String, totalBalance: Double, onTabSelected: (String) -> Unit, onMenuClick: () -> Unit) {
    // Khởi tạo bộ format dấu chấm chuẩn Việt Nam
    val balanceFormatter = remember {
        java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols().apply {
            groupingSeparator = '.'
        })
    }
    val formattedBalance = if (totalBalance == 0.0) "0" else balanceFormatter.format(totalBalance)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp)) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null, tint = Color.White) }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TỔNG SỐ DƯ", color = Color.White.copy(0.6f), fontSize = 11.sp)
            // ✅ ĐÃ SỬA: Hiển thị chuỗi số dư mượt mà theo cấu trúc phân tách hàng nghìn
            Text("$formattedBalance đ", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        }
        // ... các phần Tab CHI PHÍ / THU NHẬP bên dưới giữ nguyên trạng thái cũ ...
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
fun ExpenseListItem(group: GroupedExpense, selectedType: String, onClick: () -> Unit) {
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
            percent = "${group.transactionCount} giao dịch",
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