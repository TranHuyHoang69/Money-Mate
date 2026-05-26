package com.example.moneymate.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class CalendarMode { DAY, WEEK, MONTH, YEAR, CUSTOM } // Thêm CUSTOM
enum class SortType { TIME_DESC, AMOUNT_DESC }

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    var calendarMode by mutableStateOf(CalendarMode.DAY)
    var referenceDate by mutableStateOf(Calendar.getInstance())
    var currentSortType by mutableStateOf(SortType.TIME_DESC)

    // Lưu trữ khoảng thời gian tùy chỉnh để hiển thị text
    var customRangeText by mutableStateOf("")

    private val _uiState = MutableStateFlow<List<Expense>>(emptyList())
    val uiState: StateFlow<List<Expense>> = _uiState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var fetchJob: Job? = null
    var currentStartTimePeriod: Long = 0L
        private set

    var currentEndTimePeriod: Long = 0L
        private set

    init { loadData() }

    fun loadData() {
        if (calendarMode == CalendarMode.CUSTOM) return // Custom range dùng hàm riêng bên dưới
        fetchJob?.cancel()
        val (startTime, endTime) = getStartEndTimestamp()
        fetchData(startTime, endTime)
    }

    // Hàm mới để load theo khoảng ngày do người dùng chọn
    fun loadCustomRange(startMillis: Long, endMillis: Long) {
        calendarMode = CalendarMode.CUSTOM
        currentStartTimePeriod = startMillis
        currentEndTimePeriod = endMillis

        // Format text hiển thị: dd/MM - dd/MM
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        customRangeText = "${sdf.format(Date(startMillis))} - ${sdf.format(Date(endMillis))}"

        fetchJob?.cancel()
        fetchData(startMillis, endMillis)
    }

    private fun fetchData(startTime: Long, endTime: Long) {
        currentStartTimePeriod = startTime
        currentEndTimePeriod = endTime
        fetchJob = viewModelScope.launch {
            repository.getExpensesByPeriod(startTime, endTime).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _isLoading.value = false
                        _uiState.value = sortExpenses(result.data)
                    }
                    is Result.Loading -> _isLoading.value = true
                    is Result.Error -> {
                        _isLoading.value = false
                        _uiState.value = emptyList()
                    }
                }
            }
        }
    }

    private fun sortExpenses(list: List<Expense>): List<Expense> {
        return when (currentSortType) {
            SortType.TIME_DESC -> list.sortedByDescending { it.timestamp }
            SortType.AMOUNT_DESC -> list.sortedByDescending { it.amount }
        }
    }

    fun updateSortType(type: SortType) {
        currentSortType = type

        // Nếu là CUSTOM thì không gọi loadData() (vì bị return), mà tự sắp xếp lại data đang có luôn
        if (calendarMode == CalendarMode.CUSTOM) {
            _uiState.value = sortExpenses(_uiState.value)
        } else {
            loadData()
        }
    }

    fun changeMode(mode: CalendarMode) {
        calendarMode = mode
        if (mode != CalendarMode.CUSTOM) {
            referenceDate = Calendar.getInstance()
            loadData()
        }
    }

    fun moveNext() {
        if (!isNextEnabled() || calendarMode == CalendarMode.CUSTOM) return
        val newDate = referenceDate.clone() as Calendar
        when (calendarMode) {
            CalendarMode.DAY -> newDate.add(Calendar.DAY_OF_YEAR, 1)
            CalendarMode.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, 1)
            CalendarMode.MONTH -> newDate.add(Calendar.MONTH, 1)
            CalendarMode.YEAR -> newDate.add(Calendar.YEAR, 1)
            else -> {}
        }
        referenceDate = newDate
        loadData()
    }

    fun movePrevious() {
        if (calendarMode == CalendarMode.CUSTOM) return
        val newDate = referenceDate.clone() as Calendar
        when (calendarMode) {
            CalendarMode.DAY -> newDate.add(Calendar.DAY_OF_YEAR, -1)
            CalendarMode.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, -1)
            CalendarMode.MONTH -> newDate.add(Calendar.MONTH, -1)
            CalendarMode.YEAR -> newDate.add(Calendar.YEAR, -1)
            else -> {}
        }
        referenceDate = newDate
        loadData()
    }

    fun isNextEnabled(): Boolean {
        if (calendarMode == CalendarMode.CUSTOM) return false
        val now = Calendar.getInstance()
        return when (calendarMode) {
            CalendarMode.DAY -> referenceDate.timeInMillis < now.timeInMillis
            CalendarMode.WEEK -> referenceDate.get(Calendar.WEEK_OF_YEAR) < now.get(Calendar.WEEK_OF_YEAR) || referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
            CalendarMode.MONTH -> referenceDate.get(Calendar.MONTH) < now.get(Calendar.MONTH) || referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
            CalendarMode.YEAR -> referenceDate.get(Calendar.YEAR) < now.get(Calendar.YEAR)
            else -> false
        }
    }

    private fun getStartEndTimestamp(): Pair<Long, Long> {
        val start = referenceDate.clone() as Calendar
        val end = referenceDate.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0)
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)

        when (calendarMode) {
            CalendarMode.WEEK -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 6)
                end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59)
            }
            CalendarMode.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            CalendarMode.YEAR -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
            }
            else -> {}
        }
        return Pair(start.timeInMillis, end.timeInMillis)
    }

    fun getDisplayTime(): String {
        return when (calendarMode) {
            CalendarMode.DAY -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(referenceDate.time)
            CalendarMode.MONTH -> SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(referenceDate.time)
            CalendarMode.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(referenceDate.time)
            CalendarMode.WEEK -> getWeekRangeString(referenceDate)
            CalendarMode.CUSTOM -> customRangeText
        }
    }

    private fun getWeekRangeString(cal: Calendar): String {
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, 6)
        return "${SimpleDateFormat("dd/MM", Locale.getDefault()).format(start.time)} - ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(end.time)}"
    }
}