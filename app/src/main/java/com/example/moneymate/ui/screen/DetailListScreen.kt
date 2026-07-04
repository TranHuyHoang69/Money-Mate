package com.example.moneymate.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.moneymate.StringRes
import com.example.moneymate.domain.model.ExpenseSyncStatus
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.CalendarMode
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.SortType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    categoryName: String = "",
    type: String = TransactionType.SPEND.name,
    onOpenDrawer: () -> Unit
) {
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()

    val defaultTitle = stringResource(StringRes.history_transaction_default_title)
    val displayTitle = remember(categoryName, defaultTitle) {
        categoryName.ifEmpty { defaultTitle }
    }

    val typeSpendLabel = stringResource(StringRes.type_spend_upper)
    val typeIncomeLabel = stringResource(StringRes.type_income_upper)
    var selectedType by remember(type) {
        mutableStateOf(parseTransactionTypeRoute(type))
    }

    val sortTimeLabel = stringResource(StringRes.sort_by_time)
    val sortAmountLabel = stringResource(StringRes.sort_by_amount)
    var currentSortType by remember { mutableStateOf(SortType.TIME_DESC) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = System.currentTimeMillis()
    )

    val spendColor = MaterialTheme.colorScheme.primary
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val themeColor = remember(selectedType, spendColor, incomeColor) {
        if (selectedType == TransactionType.SPEND) spendColor else incomeColor
    }

    val filteredList = remember(expensesByPeriod, selectedType, categoryId) {
        expensesByPeriod.filter { expense ->
            val matchesType = expense.type == selectedType
            val matchesCategory = if (categoryId == 0L) true else expense.category.id == categoryId
            matchesType && matchesCategory
        }.map { expense ->
            Pair(expense, detailDateFormatter.format(Date(expense.timestamp)))
        }
    }

    val totalAmount = remember(filteredList) {
        filteredList.sumOf { it.first.amount }
    }

    val currencyUnit = stringResource(StringRes.currency_unit)

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
                    Text(text = stringResource(StringRes.confirm_btn), color = themeColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(StringRes.cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = {
                        Text(
                            text = stringResource(StringRes.date_picker_range_title),
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    headline = {
                        Text(
                            text = stringResource(StringRes.date_picker_range_headline),
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
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
                            contentDescription = stringResource(StringRes.menu_icon_desc),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = displayTitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(
                        TransactionType.SPEND to typeSpendLabel,
                        TransactionType.INCOME to typeIncomeLabel
                    ).forEach { (transactionType, label) ->
                        val isSelected = selectedType == transactionType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeColor else Color.Transparent)
                                .clickable { selectedType = transactionType }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

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

            SummaryRow(
                totalAmount = totalAmount,
                themeColor = themeColor,
                currentSortType = if (currentSortType == SortType.TIME_DESC) sortTimeLabel else sortAmountLabel,
                sortTimeLabel = sortTimeLabel,
                sortAmountLabel = sortAmountLabel,
                currencyUnit = currencyUnit,
                onSortChange = { _, isTime ->
                    currentSortType = if (isTime) SortType.TIME_DESC else SortType.AMOUNT_DESC
                    historyViewModel.updateSortType(currentSortType)
                }
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = themeColor)
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(text = stringResource(StringRes.no_data_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = filteredList,
                        key = { index, pair -> "${pair.first.id}_$index" },
                        contentType = { _, _ -> "ExpenseItem" }
                    ) { _, (expense, formattedTime) ->
                        ExpenseItem(
                            title = expense.category.title,
                            percent = formattedTime,
                            amount = "${if (expense.type == TransactionType.SPEND) "-" else "+"} ${currencyFormatter.format(expense.amount)} $currencyUnit",
                            color = expense.category.colorHex.toComposeColor(),
                            syncStatusLabel = expense.syncStatusLabel(),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    val docId = expense.firestoreDocId
                                    if (docId.isNotEmpty()) {
                                        navController.navigate("detail_expense/$docId")
                                    } else {
                                        android.util.Log.e("DetailListScreen", "Missing firestoreDocId")
                                    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val modes = listOf(
                CalendarMode.DAY to stringResource(StringRes.day),
                CalendarMode.WEEK to stringResource(StringRes.week),
                CalendarMode.MONTH to stringResource(StringRes.month),
                CalendarMode.YEAR to stringResource(StringRes.year)
            )
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeChange(mode) }
                        .padding(8.dp),
                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
            Text(
                text = stringResource(StringRes.period_custom_range),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeClick() }
                    .padding(8.dp),
                color = if (currentMode == CalendarMode.CUSTOM) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = if (isCustom) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else themeColor
                )
            }
            Text(
                text = displayTime,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            val nextState = isNextEnabled && !isCustom
            IconButton(onClick = onNext, enabled = nextState) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (nextState) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
    sortTimeLabel: String,
    sortAmountLabel: String,
    currencyUnit: String,
    onSortChange: (String, Boolean) -> Unit
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
            Text(
                text = stringResource(StringRes.total_summary_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${String.format("%,.0f", totalAmount)} $currencyUnit",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
        }
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(StringRes.sort_prefix)} $currentSortType",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text(text = sortTimeLabel, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSortChange(sortTimeLabel, true); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text(text = sortAmountLabel, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSortChange(sortAmountLabel, false); expanded = false }
                )
            }
        }
    }
}

fun String.toComposeColor(): Color {
    return try {
        val cleanedHex = this.removePrefix("#")
        val longHex = cleanedHex.toLong(16)
        if (cleanedHex.length == 6) {
            Color(longHex or 0xFF000000)
        } else {
            Color(longHex)
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

internal fun parseTransactionTypeRoute(value: String): TransactionType {
    return runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.SPEND)
}

private fun com.example.moneymate.domain.model.Expense.syncStatusLabel(): String? {
    return when {
        lastSyncError != null -> "Đồng bộ lỗi"
        syncStatus != ExpenseSyncStatus.SYNCED -> "Chờ đồng bộ"
        else -> null
    }
}
