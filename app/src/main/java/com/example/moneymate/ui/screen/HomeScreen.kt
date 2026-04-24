package com.example.moneymate.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.viewmodel.CalendarMode
import com.example.moneymate.viewmodel.ChartData
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel
) {
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val allExpensesResult by homeViewModel.expensesState.collectAsState()

    val scrollState = rememberLazyListState()
    var selectedType by remember { mutableStateOf("CHI PHÍ") }
    val themeColor = if (selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)

    // --- 1. STATE CHO DATE RANGE PICKER ---
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // --- 2. DIALOG CHỌN KHOẢNG THỜI GIAN ---
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
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        ) {
            Box(modifier = Modifier.height(450.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text("Chọn khoảng thời gian", modifier = Modifier.padding(16.dp)) },
                    showModeToggle = false
                )
            }
        }
    }

    // --- 3. LOGIC TÍNH TOÁN DỮ LIỆU ---
    val totalBalance = remember(allExpensesResult) {
        if (allExpensesResult is com.example.moneymate.domain.Result.Success) {
            val data = (allExpensesResult as com.example.moneymate.domain.Result.Success).data
            val income = data.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val spend = data.filter { it.type == TransactionType.SPEND }.sumOf { it.amount }
            income - spend
        } else 0.0
    }

    val categoryGroupedList = remember(expensesByPeriod, selectedType) {
        val filtered = expensesByPeriod.filter { expense ->
            if (selectedType == "CHI PHÍ") expense.type == TransactionType.SPEND
            else expense.type == TransactionType.INCOME
        }
        val totalAmountOfPeriod = filtered.sumOf { it.amount }

        filtered.groupBy { it.category.id }
            .map { (_, items) ->
                val firstItem = items.first()
                val groupSum = items.sumOf { it.amount }
                object {
                    val category = firstItem.category
                    val totalAmount = groupSum
                    val transactionCount = items.size
                    val percentage = if (totalAmountOfPeriod > 0) (groupSum / totalAmountOfPeriod * 100).toFloat() else 0f
                    val singleId = firstItem.id
                }
            }.sortedByDescending { it.totalAmount }
    }

    val chartData = remember(categoryGroupedList) {
        categoryGroupedList.map {
            ChartData(it.category.title, it.totalAmount, it.percentage, it.category.colorHex)
        }
    }

    // --- 4. LOGIC MORPHING ---
    val morphProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset / 400f).coerceIn(0f, 1f)
        }
    }
    val dynamicChartHeight = (180 - (120 * morphProgress)).dp

    Box(modifier = Modifier.fillMaxSize().background(themeColor)) {
        HeaderSection(selectedType, totalBalance, { selectedType = it }, navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xFFF8F9FA))
        ) {
            // Thanh điều hướng thời gian
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

                    item {
                        Text(
                            "Phân tích chi tiêu",
                            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) {
                                CircularProgressIndicator(color = themeColor)
                            }
                        }
                    } else if (categoryGroupedList.isEmpty()) {
                        item { EmptyStateSection() }
                    } else {
                        items(categoryGroupedList) { group ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                onClick = {
                                    if (group.transactionCount > 1) {
                                        navController.navigate("grouped_expense/${group.category.title}/${group.totalAmount}")
                                    } else {
                                        navController.navigate("detail_expense/${group.singleId}")
                                    }
                                }
                            ) {
                                ExpenseItem(
                                    title = group.category.title,
                                    percent = "${group.transactionCount} giao dịch (${String.format("%.1f", group.percentage)}%)",
                                    amount = "${if (selectedType == "CHI PHÍ") "-" else "+"} ${String.format("%,.0f", group.totalAmount)} $",
                                    color = Color(android.graphics.Color.parseColor(group.category.colorHex))
                                )
                            }
                        }
                    }
                }

                // Lớp biểu đồ Morphing
                Surface(
                    modifier = Modifier.fillMaxWidth().height(dynamicChartHeight),
                    color = Color(0xFFF8F9FA),
                    shadowElevation = (morphProgress * 4).dp
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        if (morphProgress < 0.6f && chartData.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = 1f - morphProgress * 2.5f }) {
                                Text("Tổng cộng", fontSize = 11.sp, color = Color.Gray)
                                Text(text = "${String.format("%,.0f", chartData.sumOf { it.totalAmount })} $", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        if (morphProgress > 0.8f) {
                            Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Phân bổ chi tiêu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("${String.format("%,.0f", chartData.sumOf { it.totalAmount })} $", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (chartData.isNotEmpty()) { MorphingCanvas(chartData, morphProgress) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFFFFC107),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White)
        }
    }
}

@Composable
fun TimeNavigationHeader(
    viewModel: HistoryViewModel,
    themeColor: Color,
    onCustomRangeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CalendarMode.entries.forEach { mode ->
                val isSelected = viewModel.calendarMode == mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (mode == CalendarMode.CUSTOM) onCustomRangeClick()
                            else viewModel.changeMode(mode)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = when (mode) {
                            CalendarMode.DAY -> "Ngày"
                            CalendarMode.WEEK -> "Tuần"
                            CalendarMode.MONTH -> "Tháng"
                            CalendarMode.YEAR -> "Năm"
                            CalendarMode.CUSTOM -> "Khoảng"
                        },
                        color = if (isSelected) themeColor else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (isSelected) Box(modifier = Modifier.padding(top = 2.dp).size(width = 16.dp, height = 2.dp).background(themeColor, CircleShape))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val isCustom = viewModel.calendarMode == CalendarMode.CUSTOM
            IconButton(onClick = { viewModel.movePrevious() }, enabled = !isCustom) {
                Icon(Icons.Default.ChevronLeft, null, tint = if(isCustom) Color.LightGray else themeColor)
            }
            Text(text = viewModel.getDisplayTime(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
            val canNext = viewModel.isNextEnabled() && !isCustom
            IconButton(onClick = { viewModel.moveNext() }, enabled = canNext) {
                Icon(Icons.Default.ChevronRight, null, tint = if (canNext) themeColor else themeColor.copy(0.3f))
            }
        }
    }
}

@Composable
fun MorphingCanvas(chartData: List<ChartData>, progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val pieRadius = 70.dp.toPx()
        val pieStrokeWidth = 36.dp.toPx()
        val barHeight = 14.dp.toPx()
        val barY = height - 20.dp.toPx()

        if (progress < 0.8f) {
            drawCircle(
                color = Color.LightGray.copy(alpha = (1f - progress) * 0.2f),
                radius = pieRadius,
                center = Offset(width / 2, height / 2),
                style = Stroke(width = pieStrokeWidth)
            )
        }

        var currentStartAngle = -90f
        var currentBarX = 0f

        chartData.forEach { data ->
            val sweepAngle = (data.percentage / 100f) * 360f
            val sectionBarWidth = (data.percentage / 100f) * width
            val color = Color(android.graphics.Color.parseColor(data.color))

            if (progress < 0.99f) {
                drawArc(
                    color = color.copy(alpha = 1f - progress),
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(width / 2 - pieRadius, height / 2 - pieRadius),
                    size = Size(pieRadius * 2, pieRadius * 2),
                    style = Stroke(width = pieStrokeWidth * (1f - progress * 0.4f), cap = StrokeCap.Butt)
                )
            }

            if (progress > 0.05f) {
                drawRoundRect(
                    color = color.copy(alpha = progress),
                    topLeft = Offset(currentBarX, barY),
                    size = Size(sectionBarWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
            currentStartAngle += sweepAngle
            currentBarX += sectionBarWidth
        }
    }
}

@Composable
fun HeaderSection(
    selectedType: String,
    totalBalance: Double,
    onTabSelected: (String) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { /* Menu */ }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
            IconButton(onClick = { navController.navigate("history_all") }) { Icon(Icons.Default.List, null, tint = Color.White) }
        }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TỔNG SỐ DƯ", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "${String.format("%,.0f", totalBalance)} $", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(0.12f)).padding(4.dp)) {
            Row {
                listOf("CHI PHÍ", "THU NHẬP").forEach { title ->
                    val isSelected = selectedType == title
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color.White.copy(0.2f) else Color.Transparent).clickable { onTabSelected(title) }.padding(horizontal = 28.dp, vertical = 10.dp)) {
                        Text(text = title, color = if (isSelected) Color.White else Color.White.copy(0.5f), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
        Text("Không có giao dịch nào", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
    }
}