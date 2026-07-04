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
import androidx.compose.material3.MaterialTheme
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
import com.example.moneymate.domain.model.GroupedExpense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.chart.MorphingChartSection
import com.example.moneymate.ui.item.ExpenseItem
import com.example.moneymate.ui.navigation.HomeNavKeys
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.theme.AppTopBarColor
import com.example.moneymate.ui.theme.stringResource
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
    val expensesByPeriod by historyViewModel.uiState.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authUiState.isLoggedIn) {
        android.util.Log.d("HomeScreen", "Auth changed: ${authUiState.isLoggedIn}")
        historyViewModel.loadData()
        homeViewModel.reloadExpenses()
    }

    val scrollState = rememberLazyListState()
    val typeSpendLabel = stringResource(StringRes.type_spend_upper)
    val typeIncomeLabel = stringResource(StringRes.type_income_upper)
    var selectedType by remember { mutableStateOf(TransactionType.SPEND) }

    val spendColor = MaterialTheme.colorScheme.primary
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val themeColor = remember(selectedType, spendColor, incomeColor) {
        if (selectedType == TransactionType.SPEND) spendColor else incomeColor
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val savedTransactionDateState = savedStateHandle
        ?.getStateFlow<Long?>(HomeNavKeys.SAVED_TRANSACTION_DATE_MILLIS, null)
        ?.collectAsState()

    LaunchedEffect(savedTransactionDateState?.value) {
        val savedTimestamp = savedTransactionDateState?.value ?: return@LaunchedEffect
        historyViewModel.showDate(savedTimestamp)
        savedStateHandle?.remove<Long>(HomeNavKeys.SAVED_TRANSACTION_DATE_MILLIS)
    }

    val totalBalance by homeViewModel.totalBalance.collectAsState()
    var categoryGroupedList by remember { mutableStateOf<List<GroupedExpense>>(emptyList()) }
    var chartData by remember { mutableStateOf<List<ChartData>>(emptyList()) }

    LaunchedEffect(expensesByPeriod, selectedType) {
        withContext(Dispatchers.Default) {
            val filtered = expensesByPeriod.filter { it.type == selectedType }
            val totalAmountOfPeriod = filtered.sumOf { it.amount }

            val groups = filtered.groupBy { it.category.id }.map { (_, items) ->
                val firstItem = items.first()
                val groupSum = items.sumOf { it.amount }
                GroupedExpense(
                    category = firstItem.category,
                    totalAmount = groupSum,
                    transactionCount = items.size,
                    type = selectedType
                )
            }.sortedByDescending { it.totalAmount }

            val charts = groups.map {
                val percentage = if (totalAmountOfPeriod > 0) {
                    (it.totalAmount / totalAmountOfPeriod * 100).toFloat()
                } else {
                    0f
                }
                ChartData(it.category.title, it.totalAmount, percentage, it.category.colorHex)
            }
            Pair(groups, charts)
        }.let { (groups, charts) ->
            categoryGroupedList = groups
            chartData = charts
        }
    }

    val morphProgress by remember {
        derivedStateOf {
            val hasScrolled = scrollState.firstVisibleItemIndex > 0 ||
                scrollState.firstVisibleItemScrollOffset > 0
            when {
                scrollState.firstVisibleItemIndex > 0 -> 1f
                hasScrolled && !scrollState.canScrollForward -> 1f
                else -> (scrollState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
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
                    Text(
                        text = stringResource(StringRes.confirm_btn),
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = stringResource(StringRes.cancel_btn),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            Box(modifier = Modifier.height(450.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = {
                        Text(
                            text = stringResource(StringRes.date_picker_range_title),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                )
            }
        }
    }

    val currencyUnit = stringResource(StringRes.currency_unit)
    val transactionsCountTemplate = stringResource(StringRes.transactions_count_format)

    Box(modifier = Modifier.fillMaxSize().background(AppTopBarColor)) {
        HeaderSection(
            selectedType = selectedType,
            typeSpendLabel = typeSpendLabel,
            typeIncomeLabel = typeIncomeLabel,
            totalBalance = totalBalance,
            currencyUnit = currencyUnit,
            onTabSelected = { selectedType = it },
            onMenuClick = onOpenDrawer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.background)
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
                        items(
                            items = categoryGroupedList,
                            key = { it.category.id },
                            contentType = { "ExpenseItem" }
                        ) { group ->
                            ExpenseListItem(
                                group = group,
                                selectedType = selectedType,
                                currencyUnit = currencyUnit,
                                countTemplate = transactionsCountTemplate,
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
                                        val singleTransaction = expensesByPeriod.find {
                                            it.category.id == group.category.id && it.type == selectedType
                                        }
                                        val docId = singleTransaction?.firestoreDocId ?: ""
                                        if (docId.isNotEmpty()) {
                                            navController.navigate("detail_expense/$docId")
                                        } else {
                                            android.util.Log.e("HomeScreen", "Missing firestoreDocId")
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(StringRes.add_btn_desc),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun HeaderSection(
    selectedType: TransactionType,
    typeSpendLabel: String,
    typeIncomeLabel: String,
    totalBalance: Double,
    currencyUnit: String,
    onTabSelected: (TransactionType) -> Unit,
    onMenuClick: () -> Unit
) {
    val balanceFormatter = remember {
        java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols().apply {
            groupingSeparator = '.'
        })
    }
    val formattedBalance = if (totalBalance == 0.0) "0" else balanceFormatter.format(totalBalance)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp)) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, null, tint = Color.White)
        }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(StringRes.total_balance), color = Color.White, fontSize = 11.sp)
            Text(
                text = "$formattedBalance $currencyUnit",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onPrimary.copy(0.1f))
                .padding(4.dp)
        ) {
            listOf(
                TransactionType.SPEND to typeSpendLabel,
                TransactionType.INCOME to typeIncomeLabel
            ).forEach { (type, title) ->
                val isSelected = selectedType == type
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(0.2f)
                            else Color.Transparent
                        )
                        .clickable { onTabSelected(type) }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (mode == CalendarMode.CUSTOM) onCustomRangeClick() else viewModel.changeMode(mode)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = when (mode) {
                            CalendarMode.DAY -> stringResource(StringRes.day)
                            CalendarMode.WEEK -> stringResource(StringRes.week)
                            CalendarMode.MONTH -> stringResource(StringRes.month)
                            CalendarMode.YEAR -> stringResource(StringRes.year)
                            CalendarMode.CUSTOM -> stringResource(StringRes.period_custom_range)
                        },
                        color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (isSelected) {
                        Box(Modifier.padding(top = 2.dp).size(16.dp, 2.dp).background(themeColor, CircleShape))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val isCustom = viewModel.calendarMode == CalendarMode.CUSTOM
            IconButton(onClick = { viewModel.movePrevious() }, enabled = !isCustom) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = if (isCustom) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else themeColor
                )
            }
            Text(
                text = viewModel.getDisplayTime(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            val canNext = viewModel.isNextEnabled() && !isCustom
            IconButton(onClick = { viewModel.moveNext() }, enabled = canNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (canNext) themeColor else themeColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun ExpenseListItem(
    group: GroupedExpense,
    selectedType: TransactionType,
    currencyUnit: String,
    countTemplate: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        val formattedCount = remember(group.transactionCount, countTemplate) {
            String.format(countTemplate, group.transactionCount)
        }

        ExpenseItem(
            title = group.category.title,
            percent = formattedCount,
            amount = "${if (selectedType == TransactionType.SPEND) "-" else "+"} ${String.format("%,.0f", group.totalAmount)} $currencyUnit",
            color = Color(android.graphics.Color.parseColor(group.category.colorHex))
        )
    }
}

@Composable
fun LoadingUI(color: Color) {
    Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) {
        CircularProgressIndicator(color = color)
    }
}

@Composable
fun EmptyStateSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = stringResource(StringRes.no_transactions),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
