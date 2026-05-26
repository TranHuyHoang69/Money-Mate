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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Khởi tạo một bản duy nhất để dùng lại, né việc new liên tục trong vòng lặp gây rác bộ nhớ (GC)
private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

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
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val themeColor = Color(0xFF4B8361)

    // State lưu trữ dữ liệu sau khi đã gộp nhóm ở background
    var groupedExpensesState by remember { mutableStateOf<Map<String, List<Expense>>>(emptyMap()) }

    // Tối ưu 1: Chuyển toàn bộ logic Filter, Group, Format Date xuống Background Thread
    LaunchedEffect(expenses, categoryName, historyViewModel.currentSortType) {
        withContext(Dispatchers.Default) {
            val filteredByCategory = if (categoryName == "Tất cả") {
                expenses
            } else {
                expenses.filter { it.category.title == categoryName }
            }
            filteredByCategory.groupBy { dateFormatter.format(Date(it.timestamp)) }
        }.let {
            groupedExpensesState = it
        }
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
                }) { Text("Xác nhận", color = themeColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = { Text("Chọn khoảng thời gian", Modifier.padding(16.dp)) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = categoryName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tổng: ${String.format("%,.0f", totalAmount)} đ",
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
// ✅ ĐOẠN CODE ĐÃ SỬA ĐỔI TOÀN DIỆN TẠI DÒNG 115 ĐỂ ĐỒNG BỘ VỚI VIEWMODEL
            CompactTimeNavigation(
                currentMode = historyViewModel.calendarMode, // Đồng bộ biến Mode
                displayTime = historyViewModel.getDisplayTime(), // Gọi hàm lấy chuỗi thời gian hiển thị
                isNextEnabled = historyViewModel.isNextEnabled(), // Kiểm tra chặn nút tiến tương lai
                onModeChange = { newMode ->
                    // Ép kiểu hoặc truyền trực tiếp CalendarMode từ enum của bạn
                    historyViewModel.changeMode(newMode)
                },
                onPrevious = { historyViewModel.movePrevious() }, // Gọi đúng hàm lùi thời gian
                onNext = { historyViewModel.moveNext() },         // Gọi đúng hàm tiến thời gian
                themeColor = themeColor,
                onRangeClick = { showDatePicker = true }
            )

            // Spinner Sắp xếp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = themeColor)
                }
            } else if (groupedExpensesState.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có dữ liệu cho $categoryName", color = Color.Gray)
                }
            } else {
                // Tối ưu 2: Trải phẳng cấu trúc danh sách để tái trưng dụng View chuẩn chỉ
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groupedExpensesState.forEach { (date, items) ->
                        // Header hiển thị Ngày tháng
                        item(key = "header_$date", contentType = "HeaderDate") {
                            Text(
                                text = date,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        // Bao bọc danh sách các Item bên trong Card bằng việc dùng itemsIndexed lồng an toàn
                        item(key = "card_$date", contentType = "GroupCard") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column {
                                    items.forEachIndexed { index, expense ->
                                        // Ghi nhận thời gian định dạng mượt mà
                                        val formattedTime = remember(expense.timestamp) {
                                            timeFormatter.format(Date(expense.timestamp))
                                        }

                                        Box(modifier = Modifier.clickable {
                                            // ✅ ĐÃ SỬA: Lấy chuỗi firestoreDocId thay thế cho id kiểu Long
                                            val docId = expense.firestoreDocId
                                            if (docId.isNotEmpty()) {
                                                navController.navigate("detail_expense/$docId")
                                            } else {
                                                android.util.Log.e("GroupedScreen", "Giao dịch này không có firestoreDocId hợp lệ!")
                                            }
                                        }) {
                                            ExpenseItem(
                                                title = expense.category.title,
                                                percent = formattedTime,
                                                amount = "${if (expense.type == TransactionType.SPEND) "-" else "+"} ${String.format("%,.0f", expense.amount)} đ",
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