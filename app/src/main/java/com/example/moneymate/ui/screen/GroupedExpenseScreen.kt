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
import androidx.compose.material3.MaterialTheme
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
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Đồng bộ dải màu theo chuẩn token Material 3 động thay vì nạp cứng mã màu HEX
    val themeColor = MaterialTheme.colorScheme.primary

    var groupedExpensesState by remember { mutableStateOf<Map<String, List<Expense>>>(emptyMap()) }

    val allCategoriesLabel = stringResource(StringRes.category_all) // ✅ Sửa lỗi compile nhãn id =

    LaunchedEffect(expenses, categoryName, historyViewModel.currentSortType, allCategoriesLabel) {
        withContext(Dispatchers.Default) {
            val filteredByCategory = if (categoryName == allCategoriesLabel) {
                expenses
            } else {
                expenses.filter { it.category.title == categoryName }
            }
            filteredByCategory.groupBy { dateFormatter.format(Date(it.timestamp)) }
        }.let {
            groupedExpensesState = it
        }
    }

    val currencyUnit = stringResource(StringRes.currency_unit) // ✅ Sửa lỗi compile nhãn id =

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
                }) { Text(text = stringResource(StringRes.confirm_btn), color = themeColor) } // ✅ Sửa lỗi compile nhãn id =
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(StringRes.cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant) // ✅ Sửa lỗi compile nhãn id =
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = { Text(text = stringResource(StringRes.date_picker_range_title), modifier = Modifier.padding(16.dp)) } // ✅ Sửa lỗi compile nhãn id =
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = categoryName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // ✅ Sửa màu Text theo hệ thống thích ứng Dark Mode
                        )
                        Text(
                            text = "${stringResource(StringRes.total_prefix)} ${String.format("%,.0f", totalAmount)} $currencyUnit", // ✅ Sửa lỗi compile nhãn id =
                            fontSize = 14.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(StringRes.back_btn), // ✅ Sửa lỗi compile nhãn id =
                            tint = MaterialTheme.colorScheme.onSurface // ✅ Sửa màu Icon theo hệ thống thích ứng Dark Mode
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface) // ✅ Đổi màu nền thanh công cụ thích ứng hệ thống
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CompactTimeNavigation(
                currentMode = historyViewModel.calendarMode,
                displayTime = historyViewModel.getDisplayTime(),
                isNextEnabled = historyViewModel.isNextEnabled(),
                themeColor = themeColor,
                onModeChange = { newMode -> historyViewModel.changeMode(newMode) },
                onPrevious = { historyViewModel.movePrevious() },
                onNext = { historyViewModel.moveNext() },
                onRangeClick = { showDatePicker = true }
            )

            // Bộ Spinner Sắp xếp dữ liệu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ i18n: Nhãn trạng thái bộ lọc sắp xếp hiện tại (Sửa lỗi nhãn id =)
                        val sortLabel = if (historyViewModel.currentSortType == SortType.TIME_DESC)
                            stringResource(StringRes.sort_latest) else stringResource(StringRes.sort_amount_desc_label)
                        Text(
                            text = "${stringResource(StringRes.sort_prefix)} $sortLabel", // ✅ Sửa lỗi compile nhãn id =
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Thực đơn lựa chọn thả xuống
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(StringRes.sort_latest_with_desc), color = MaterialTheme.colorScheme.onSurface) }, // ✅ Sửa lỗi compile nhãn id =
                        onClick = {
                            historyViewModel.updateSortType(SortType.TIME_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(StringRes.sort_amount_desc_with_desc), color = MaterialTheme.colorScheme.onSurface) }, // ✅ Sửa lỗi compile nhãn id =
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
                    Text(
                        text = "${stringResource(StringRes.no_data_for_category_prefix)} $categoryName", // ✅ Sửa lỗi compile nhãn id =
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groupedExpensesState.forEach { (date, items) ->
                        item(key = "header_$date", contentType = "HeaderDate") {
                            Text(
                                text = date,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item(key = "card_$date", contentType = "GroupCard") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column {
                                    items.forEachIndexed { index, expense ->
                                        val formattedTime = remember(expense.timestamp) {
                                            timeFormatter.format(Date(expense.timestamp))
                                        }

                                        Box(modifier = Modifier.clickable {
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
                                                amount = "${if (expense.type == TransactionType.SPEND) "-" else "+"} ${String.format("%,.0f", expense.amount)} $currencyUnit",
                                                color = Color(android.graphics.Color.parseColor(expense.category.colorHex))
                                            )
                                        }

                                        if (items.size > 1 && index < items.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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