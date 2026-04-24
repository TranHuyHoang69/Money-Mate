package com.example.moneymate.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.SortType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedExpenseScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel(),
    categoryName: String,
    totalAmount: Double
) {
    val expenses by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    // --- 1. THÊM STATE CHO DATE RANGE PICKER ---
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = androidx.compose.material3.rememberDateRangePickerState()
    val themeColor = Color(0xFF4B8361)

    // --- 2. LOGIC DIALOG CHỌN NGÀY ---
    if (showDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        historyViewModel.loadCustomRange(start, end)
                    }
                    showDatePicker = false
                }) { Text("Xác nhận", color = themeColor) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            androidx.compose.material3.DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = { Text("Chọn khoảng thời gian", Modifier.padding(16.dp)) }
            )
        }
    }

    // Logic gộp nhóm (Giữ nguyên của bạn)
    val groupedExpenses = remember(expenses, categoryName, historyViewModel.currentSortType) {
        val filteredByCategory = if (categoryName == "Tất cả") {
            expenses
        } else {
            expenses.filter { it.category.title == categoryName }
        }
        filteredByCategory.groupBy {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.timestamp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = categoryName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tổng: ${String.format("%,.0f", totalAmount)} $",
                            fontSize = 14.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
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
            // --- 3. CẬP NHẬT TRUYỀN THAM SỐ VÀO ĐÂY ---
            CompactTimeNavigation(
                viewModel = historyViewModel,
                themeColor = themeColor,
                onRangeClick = {
                    showDatePicker = true // Mở Dialog khi nhấn vào "Khoảng"
                }
            )

            // Spinner Sắp xếp (Giữ nguyên logic của bạn)
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(0.5f)),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sortLabel = if (historyViewModel.currentSortType == SortType.TIME_DESC)
                            "Mới nhất" else "Giá tiền giảm"
                        Text(text = "Sắp xếp: $sortLabel", fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                    }
                }

                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Mới nhất (Thời gian)") },
                        onClick = {
                            historyViewModel.updateSortType(SortType.TIME_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Số tiền (Giảm dần)") },
                        onClick = {
                            historyViewModel.updateSortType(SortType.AMOUNT_DESC)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Danh sách hiển thị (Giữ nguyên logic Card của bạn)
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = themeColor)
                }
            } else if (groupedExpenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có dữ liệu cho $categoryName", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groupedExpenses.forEach { (date, items) ->
                        item {
                            Text(
                                text = date,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column {
                                    items.forEachIndexed { index, expense ->
                                        Box(modifier = Modifier.clickable {
                                            navController.navigate("detail_expense/${expense.id}")
                                        }) {
                                            ExpenseItem(
                                                title = expense.category.title,
                                                percent = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expense.timestamp)),
                                                amount = "${if (expense.type == TransactionType.SPEND) "-" else "+"} ${String.format("%,.0f", expense.amount)} $",
                                                color = Color(android.graphics.Color.parseColor(expense.category.colorHex))
                                            )
                                        }
                                        if (items.size > 1 && index < items.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                thickness = 0.5.dp,
                                                color = Color(0xFFF1F3F5)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}