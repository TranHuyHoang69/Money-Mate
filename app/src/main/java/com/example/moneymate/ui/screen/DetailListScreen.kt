package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.viewmodel.CalendarMode
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.SortType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Khởi tạo một đối tượng tĩnh dùng chung duy nhất, tránh phân bổ rác bộ nhớ (GC Static Overhead)
private val detailDateFormatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

private val currencyFormatter = java.text.DecimalFormat("#,###").apply {
    decimalFormatSymbols = java.text.DecimalFormatSymbols.getInstance(Locale.getDefault())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailListScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel(),
    categoryId: Long = 0L,
    categoryName: String = "Lịch sử giao dịch",
    type: String = "CHI PHÍ",
    onOpenDrawer: () -> Unit
) {
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()

    var selectedType by remember(type) {
        mutableStateOf(if (type.contains("THU", ignoreCase = true)) "THU NHẬP" else "CHI PHÍ")
    }
    var currentSortType by remember { mutableStateOf("Thời gian") }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = System.currentTimeMillis()
    )

    val themeColor = remember(selectedType) {
        if (selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)
    }

    // Tối ưu 1: Lọc danh sách kết hợp map chuỗi thời gian được tính toán trước (Pre-computed Display Time)
    val filteredList = remember(expensesByPeriod, selectedType, categoryId) {
        expensesByPeriod.filter { expense ->
            val targetType = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
            val matchesType = expense.type == targetType
            val matchesCategory = if (categoryId == 0L) true else expense.category.id == categoryId
            matchesType && matchesCategory
        }.map { expense ->
            // Bọc dữ liệu kèm chuỗi thời gian đã được format sẵn, triệt tiêu gánh nặng khi cuộn LazyColumn
            Pair(expense, detailDateFormatter.format(Date(expense.timestamp)))
        }
    }

    val totalAmount = remember(filteredList) {
        filteredList.sumOf { it.first.amount }
    }

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
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = { Text("Chọn khoảng thời gian", modifier = Modifier.padding(16.dp)) },
                    headline = {
                        Text(
                            "Chọn ngày bắt đầu - kết thúc",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 14.sp
                        )
                    },
                    showModeToggle = false
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 3.dp) {
                TopAppBar(
                    title = { Text(categoryName, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Tabs Thu/Chi
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color(0xFFF1F3F5), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("CHI PHÍ", "THU NHẬP").forEach { title ->
                        val isSelected = selectedType == title
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeColor else Color.Transparent)
                                .clickable { selectedType = title }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Navigation Thời gian (Truyền giá trị thay vì đọc trực tiếp state từ ViewModel ở cha)
            CompactTimeNavigation(
                currentMode = historyViewModel.calendarMode,
                displayTime = historyViewModel.getDisplayTime(),
                isNextEnabled = historyViewModel.isNextEnabled(),
                themeColor = themeColor,
                onModeChange = { historyViewModel.changeMode(it) },
                onPrevious = { historyViewModel.movePrevious() },
                onNext = { historyViewModel.moveNext() },
                onRangeClick = { showDatePicker = true }
            )

            // Tổng cộng & Sắp xếp
            SummaryRow(
                totalAmount = totalAmount,
                themeColor = themeColor,
                currentSortType = currentSortType,
                onSortChange = {
                    currentSortType = it
                    historyViewModel.updateSortType(if(it == "Thời gian") SortType.TIME_DESC else SortType.AMOUNT_DESC)
                }
            )

            // Danh sách hiển thị
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = themeColor)
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Không có dữ liệu", color = Color.Gray)
                }
            } else {
                // Tối ưu 2: Thêm định danh Key cố định và tận dụng thời gian đã giải mã sẵn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = filteredList,
                        key = { index, pair -> "${pair.first.id}_$index" }, // Tạo chuỗi khóa độc nhất vô nhị
                        contentType = { _, _ -> "ExpenseItem" }
                    ) { index, (expense, formattedTime) ->
                        ExpenseItem(
                            title = expense.category.title,
                            percent = formattedTime,
                            amount = "${if (expense.type == TransactionType.SPEND) "-" else "+"} ${currencyFormatter.format(expense.amount)} đ",
                            color = expense.category.colorHex.toComposeColor(),
                            modifier = Modifier.clickable {
                                navController.navigate("detail_expense/${expense.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactTimeNavigation(
    currentMode: CalendarMode,
    displayTime: String,
    isNextEnabled: Boolean,
    themeColor: Color,
    onModeChange: (CalendarMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRangeClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color.White)
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val modes = listOf(
                CalendarMode.DAY to "Ngày",
                CalendarMode.WEEK to "Tuần",
                CalendarMode.MONTH to "Tháng",
                CalendarMode.YEAR to "Năm"
            )
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeChange(mode) }
                        .padding(8.dp),
                    color = if (isSelected) themeColor else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "Khoảng",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeClick() }
                    .padding(8.dp),
                color = if (currentMode == CalendarMode.CUSTOM) themeColor else Color.Gray,
                fontWeight = if (currentMode == CalendarMode.CUSTOM) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val isCustom = currentMode == CalendarMode.CUSTOM
            IconButton(onClick = onPrevious, enabled = !isCustom) {
                Icon(Icons.Default.KeyboardArrowLeft, null, tint = if(isCustom) Color.LightGray else themeColor)
            }
            Text(
                text = displayTime,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            val nextState = isNextEnabled && !isCustom
            IconButton(onClick = onNext, enabled = nextState) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    null,
                    tint = if (nextState) themeColor else Color.LightGray
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    totalAmount: Double,
    themeColor: Color,
    currentSortType: String,
    onSortChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Tổng cộng", fontSize = 13.sp, color = Color.Gray)
            Text(
                text = "${String.format("%,.0f", totalAmount)} đ",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
        }
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sắp xếp: $currentSortType", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Thời gian") },
                    onClick = { onSortChange("Thời gian"); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Số tiền") },
                    onClick = { onSortChange("Số tiền"); expanded = false }
                )
            }
        }
    }
}
// Thêm hàm tối ưu này ở ngoài cùng file UI hoặc file tiện ích
fun String.toComposeColor(): Color {
    return try {
        // Tránh parseColor bằng cách chuyển đổi trực tiếp chuỗi sang Long (Ví dụ: #4B8361 hoặc 4B8361)
        val cleanedHex = this.removePrefix("#")
        val longHex = cleanedHex.toLong(16)
        if (cleanedHex.length == 6) {
            Color(longHex or 0xFF000000) // Thêm Alpha mặc định nếu chỉ có 6 ký tự
        } else {
            Color(longHex)
        }
    } catch (e: Exception) {
        Color.Gray // Màu dự phòng nếu chuỗi hex lỗi
    }
}