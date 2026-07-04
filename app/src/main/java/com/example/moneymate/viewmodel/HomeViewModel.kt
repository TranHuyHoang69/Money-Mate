package com.example.moneymate.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class ChartData(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val color: String
)

enum class HomePeriod { DAY, WEEK, MONTH, YEAR }
enum class HomeDetailSortType { TIME, AMOUNT }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val recurringRepository: RecurringTransactionRepository
) : ViewModel() {

    var selectedPeriod by mutableStateOf(HomePeriod.DAY)
    private val _expensesState = MutableStateFlow<Result<List<Expense>>>(Result.Loading)
    val expensesState: StateFlow<Result<List<Expense>>> = _expensesState
    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance
    var detailSortType by mutableStateOf(HomeDetailSortType.TIME)
    var currentCalendar by mutableStateOf(Calendar.getInstance())
    private var loadExpensesJob: Job? = null
    private var processDueJob: Job? = null

    init {
        processDueTransactions()

        viewModelScope.launch(Dispatchers.Main) {
            delay(300)
            loadAllExpenses()
        }
    }

    fun reloadExpenses() {
        android.util.Log.d("HomeViewModel", "Reloading expenses...")
        processDueTransactions()
        loadAllExpenses()
    }

    fun loadAllExpenses() {
        loadExpensesJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncPendingExpenses()
        }
        loadExpensesJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getAllExpenses().collect { result ->
                _expensesState.value = result

                if (result is Result.Success) {
                    val total = result.data.sumOf {
                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                    }
                    _totalBalance.value = total
                }
            }
        }
    }

    fun getTimeRange(period: HomePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis

        when (period) {
            HomePeriod.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            HomePeriod.WEEK -> calendar.add(Calendar.DAY_OF_WEEK, -7)
            HomePeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
            HomePeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
        }
        return Pair(calendar.timeInMillis, end)
    }

    fun getChartData(selectedType: TransactionType): Flow<List<ChartData>> = expensesState
        .map { result ->
            if (result is Result.Success) {
                withContext(Dispatchers.Default) {
                    val range = getTimeRange(selectedPeriod)
                    val filtered = result.data.filter {
                        it.type == selectedType &&
                            it.timestamp >= range.first &&
                            it.timestamp <= range.second
                    }

                    val total = filtered.sumOf { it.amount }
                    if (total == 0.0) return@withContext emptyList<ChartData>()

                    filtered.groupBy { it.category.id }
                        .map { (_, items) ->
                            val catTotal = items.sumOf { it.amount }
                            val category = items.first().category
                            ChartData(
                                categoryName = category.title,
                                totalAmount = catTotal,
                                percentage = ((catTotal / total) * 100).toFloat(),
                                color = category.colorHex
                            )
                        }.sortedByDescending { it.totalAmount }
                }
            } else {
                emptyList()
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    fun deleteExpense(expense: Expense, onSuccess: () -> Unit) {
        if (expense.firestoreDocId.isEmpty()) return

        viewModelScope.launch {
            if (repository.deleteExpense(expense.firestoreDocId) is Result.Success) {
                onSuccess()
            }
        }
    }

    fun getExpensesByCategory(categoryId: Long, type: TransactionType): Flow<List<Expense>> {
        return repository.getAllExpenses().map { result ->
            if (result is Result.Success) {
                result.data.filter {
                    it.category.id == categoryId && it.type == type
                }
            } else {
                emptyList()
            }
        }
    }

    fun getFilteredExpenses(
        type: TransactionType,
        period: HomePeriod,
        categoryId: Long = 0
    ): Flow<List<Expense>> = _expensesState.map { result ->
        if (result is Result.Success) {
            val range = getTimeRange(period)

            val filtered = result.data.filter {
                val matchType = it.type == type
                val matchTime = it.timestamp >= range.first && it.timestamp <= range.second
                val matchCategory = if (categoryId == 0L) true else it.category.id == categoryId
                matchType && matchTime && matchCategory
            }

            when (detailSortType) {
                HomeDetailSortType.AMOUNT -> filtered.sortedByDescending { it.amount }
                HomeDetailSortType.TIME -> filtered.sortedByDescending { it.timestamp }
            }
        } else {
            emptyList()
        }
    }.flowOn(Dispatchers.Default)

    fun moveTimeRange(delta: Int) {
        val newCalendar = currentCalendar.clone() as Calendar
        when (selectedPeriod) {
            HomePeriod.DAY -> newCalendar.add(Calendar.DAY_OF_YEAR, delta)
            HomePeriod.WEEK -> newCalendar.add(Calendar.WEEK_OF_YEAR, delta)
            HomePeriod.MONTH -> newCalendar.add(Calendar.MONTH, delta)
            HomePeriod.YEAR -> newCalendar.add(Calendar.YEAR, delta)
        }
        currentCalendar = newCalendar
        loadAllExpenses()
    }

    fun refreshExpenses() {
        android.util.Log.d("HomeViewModel", "Refreshing expenses...")
        processDueTransactions()
        loadAllExpenses()
    }

    private fun processDueTransactions() {
        if (processDueJob?.isActive == true) return
        processDueJob = viewModelScope.launch(Dispatchers.IO) {
            recurringRepository.processDueTransactions()
        }
    }
}
