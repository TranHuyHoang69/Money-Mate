package com.example.moneymate.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class StatisticsTimeMode { DAY, WEEK, MONTH, YEAR }
enum class StatisticsTab { OVERVIEW, EXPENSE, INCOME }

data class OverviewStatistics(
    val income: Double,
    val expense: Double,
    val profit: Double,
    val loss: Double
)

data class CategoryStatistics(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float
)

/**
 * ✅ CẢI THIỆN: Thêm trạng thái Refreshing để giữ data cũ khi load mới
 */
sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    data class Success(
        val overview: OverviewStatistics,
        val expenseByCategory: List<CategoryStatistics>,
        val incomeByCategory: List<CategoryStatistics>,
        val allExpenses: List<Expense> = emptyList(),
        val timeMode: StatisticsTimeMode = StatisticsTimeMode.DAY,
        val displayTimeText: String = "",
        val referenceDate: Calendar = Calendar.getInstance(),
        val isRefreshing: Boolean = false  // ✅ THÊM: Flag để hiển thị loading indicator nhỏ
    ) : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}

data class TransactionDetail(
    val expense: Expense,
    val displayDate: String
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState

    var selectedTab by mutableStateOf(StatisticsTab.OVERVIEW)
        private set

    var timeMode by mutableStateOf(StatisticsTimeMode.DAY)
        private set

    var referenceDate by mutableStateOf(Calendar.getInstance())
        private set

    private var fetchJob: Job? = null
    private val cachedStatistics = mutableMapOf<String, StatisticsUiState>()

    init {
        loadStatistics()
    }

    fun changeTab(tab: StatisticsTab) {
        selectedTab = tab
    }

    fun changeTimeMode(mode: StatisticsTimeMode) {
        timeMode = mode
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(150) // ✅ GIẢM debounce từ 300ms xuống 150ms cho mượt hơn
            loadStatistics(isUserAction = true) // ✅ ĐÁNH DẤU là user action
        }
    }

    fun movePrevious() {
        val newDate = referenceDate.clone() as Calendar
        when (timeMode) {
            StatisticsTimeMode.DAY -> newDate.add(Calendar.DAY_OF_YEAR, -1)
            StatisticsTimeMode.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, -1)
            StatisticsTimeMode.MONTH -> newDate.add(Calendar.MONTH, -1)
            StatisticsTimeMode.YEAR -> newDate.add(Calendar.YEAR, -1)
        }
        referenceDate = newDate
        loadStatistics(isUserAction = true) // ✅ ĐÁNH DẤU là user action
    }

    fun moveNext() {
        if (!isNextEnabled()) return
        val newDate = referenceDate.clone() as Calendar
        when (timeMode) {
            StatisticsTimeMode.DAY -> newDate.add(Calendar.DAY_OF_YEAR, 1)
            StatisticsTimeMode.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, 1)
            StatisticsTimeMode.MONTH -> newDate.add(Calendar.MONTH, 1)
            StatisticsTimeMode.YEAR -> newDate.add(Calendar.YEAR, 1)
        }
        referenceDate = newDate
        loadStatistics(isUserAction = true) // ✅ ĐÁNH DẤU là user action
    }

    fun isNextEnabled(): Boolean {
        val now = Calendar.getInstance()
        return when (timeMode) {
            StatisticsTimeMode.DAY -> referenceDate.timeInMillis < now.timeInMillis
            StatisticsTimeMode.WEEK -> referenceDate.get(Calendar.WEEK_OF_YEAR) < now.get(Calendar.WEEK_OF_YEAR) || referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
            StatisticsTimeMode.MONTH -> referenceDate.get(Calendar.MONTH) < now.get(Calendar.MONTH) || referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
            StatisticsTimeMode.YEAR -> referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
        }
    }

    fun refreshStatistics() {
        cachedStatistics.clear()
        loadStatistics()
    }

    fun getCurrentPeriodRange(): Pair<Long, Long> = getStartEndTimestamp()

    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>> {
        return uiState.map { state ->
            if (state is StatisticsUiState.Success) {
                val (start, end) = getStartEndTimestamp()
                state.allExpenses
                    .filter {
                        it.category.id == categoryId &&
                                it.timestamp >= start &&
                                it.timestamp <= end
                    }
                    .map { expense ->
                        TransactionDetail(
                            expense = expense,
                            displayDate = formatDate(expense.timestamp)
                        )
                    }
                    .sortedByDescending { it.expense.timestamp }
            } else {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * ✅ CẢI THIỆN: Thêm param isUserAction để xử lý khác biệt
     */
    private fun loadStatistics(isUserAction: Boolean = false) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            // ✅ NẾU là user action (chuyển tab/thời gian) VÀ đã có data cũ
            // -> Giữ data cũ và chỉ set isRefreshing = true
            val currentState = _uiState.value
            if (isUserAction && currentState is StatisticsUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            } else {
                // Lần đầu load hoặc không có data -> hiển thị Loading toàn màn hình
                _uiState.value = StatisticsUiState.Loading
            }

            val cacheKey = "${timeMode}_${referenceDate.timeInMillis}"
            cachedStatistics[cacheKey]?.let { cachedState ->
                _uiState.value = cachedState
                return@launch
            }

            try {
                val (start, end) = getStartEndTimestamp()
                repository.getExpensesByPeriod(start, end).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val statistics = calculateStatistics(result.data)
                            val successState = StatisticsUiState.Success(
                                overview = statistics.first,
                                expenseByCategory = statistics.second,
                                incomeByCategory = statistics.third,
                                allExpenses = result.data,
                                timeMode = timeMode,
                                displayTimeText = getDisplayTimeText(),
                                referenceDate = referenceDate,
                                isRefreshing = false // ✅ Tắt loading indicator
                            )
                            cachedStatistics[cacheKey] = successState
                            _uiState.value = successState
                        }
                        is Result.Loading -> {
                            // ✅ KHÔNG SET Loading nữa vì đã xử lý ở trên
                        }
                        is Result.Error -> {
                            // ✅ NẾU đang có data cũ -> giữ nguyên, chỉ tắt isRefreshing
                            if (currentState is StatisticsUiState.Success) {
                                _uiState.value = currentState.copy(isRefreshing = false)
                            } else {
                                _uiState.value = StatisticsUiState.Error(result.message ?: "Unknown error")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // ✅ NẾU đang có data cũ -> giữ nguyên
                if (currentState is StatisticsUiState.Success) {
                    _uiState.value = currentState.copy(isRefreshing = false)
                } else {
                    _uiState.value = StatisticsUiState.Error(e.message ?: "Error loading statistics")
                }
            }
        }
    }

    private suspend fun calculateStatistics(expenses: List<Expense>): Triple<OverviewStatistics, List<CategoryStatistics>, List<CategoryStatistics>> {
        return withContext(Dispatchers.Default) {
            val (start, end) = getStartEndTimestamp()
            val filteredExpenses = expenses.filter { it.timestamp in start..end }

            val totalIncome = filteredExpenses
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val totalExpense = filteredExpenses
                .filter { it.type == TransactionType.SPEND }
                .sumOf { it.amount }

            val profit = if (totalIncome > totalExpense) totalIncome - totalExpense else 0.0
            val loss = if (totalExpense > totalIncome) totalExpense - totalIncome else 0.0

            val overview = OverviewStatistics(
                income = totalIncome,
                expense = totalExpense,
                profit = profit,
                loss = loss
            )

            val expenseExpenses = filteredExpenses.filter { it.type == TransactionType.SPEND }
            val expenseTotal = expenseExpenses.sumOf { it.amount }
            val expenseByCategory = expenseExpenses
                .groupBy { it.category }
                .map { (category, items) ->
                    val totalAmount = items.sumOf { it.amount }
                    CategoryStatistics(
                        category = category,
                        totalAmount = totalAmount,
                        transactionCount = items.size,
                        percentage = if (expenseTotal > 0) ((totalAmount / expenseTotal) * 100).toFloat() else 0f
                    )
                }
                .sortedByDescending { it.totalAmount }

            val incomeExpenses = filteredExpenses.filter { it.type == TransactionType.INCOME }
            val incomeTotal = incomeExpenses.sumOf { it.amount }
            val incomeByCategory = incomeExpenses
                .groupBy { it.category }
                .map { (category, items) ->
                    val totalAmount = items.sumOf { it.amount }
                    CategoryStatistics(
                        category = category,
                        totalAmount = totalAmount,
                        transactionCount = items.size,
                        percentage = if (incomeTotal > 0) ((totalAmount / incomeTotal) * 100).toFloat() else 0f
                    )
                }
                .sortedByDescending { it.totalAmount }

            Triple(overview, expenseByCategory, incomeByCategory)
        }
    }

    private fun getStartEndTimestamp(): Pair<Long, Long> {
        val start = referenceDate.clone() as Calendar
        val end = referenceDate.clone() as Calendar

        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        when (timeMode) {
            StatisticsTimeMode.WEEK -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 6)
            }
            StatisticsTimeMode.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            StatisticsTimeMode.YEAR -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
            }
            else -> {}
        }

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    private fun getDisplayTimeText(): String {
        return when (timeMode) {
            StatisticsTimeMode.DAY ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(referenceDate.time)
            StatisticsTimeMode.WEEK ->
                getWeekRangeString(referenceDate)
            StatisticsTimeMode.MONTH ->
                SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(referenceDate.time)
            StatisticsTimeMode.YEAR ->
                SimpleDateFormat("yyyy", Locale.getDefault()).format(referenceDate.time)
        }
    }

    private fun getWeekRangeString(cal: Calendar): String {
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, 6)
        return "${SimpleDateFormat("dd/MM", Locale.getDefault()).format(start.time)} - ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(end.time)}"
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
