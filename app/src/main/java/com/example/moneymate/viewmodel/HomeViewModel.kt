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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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



@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    var selectedPeriod by mutableStateOf("Ngày")
    // Lắng nghe tất cả giao dịch từ DB
    private val _expensesState = MutableStateFlow<Result<List<Expense>>>(Result.Loading)
    val expensesState: StateFlow<Result<List<Expense>>> = _expensesState
    private val _totalBalance = MutableStateFlow<Double>(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance
    var detailSortType by mutableStateOf("Thời gian")
    var currentCalendar by mutableStateOf(Calendar.getInstance())


    init {
        viewModelScope.launch(Dispatchers.Main) {
            delay(300)
            loadAllExpenses()
        }

        viewModelScope.launch(Dispatchers.Default) {

        }
    }
    fun reloadExpenses() {
        android.util.Log.d("HomeViewModel", "Reloading expenses...")
        loadAllExpenses()
    }

    fun loadAllExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllExpenses().collect { result ->
                _expensesState.value = result

                // ✅ ĐÃ THÊM: Tự động cập nhật số dư khi nhận dữ liệu mới
                if (result is Result.Success) {
                    val total = result.data.sumOf {
                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                    }
                    _totalBalance.value = total
                }
            }
        }
    }

    fun getTimeRange(period: String): Pair<Long,Long>{
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis

        when(period){
            "Ngày" -> {
                calendar.set(Calendar.HOUR_OF_DAY,0)
                calendar.set(Calendar.MINUTE,0)
            }
            "Tuần" -> calendar.add(Calendar.DAY_OF_WEEK,-7)
            "Tháng" -> calendar.add(Calendar.MONTH,-1)
            "Năm" -> calendar.add(Calendar.YEAR,-1)
        }
        return Pair(calendar.timeInMillis,end)
    }

    // Trong getChartData (HomeViewModel)
    // Sửa hàm getChartData trong HomeViewModel.kt
    fun getChartData(selectedType: String): Flow<List<ChartData>> = expensesState
        .map { result ->
            if (result is Result.Success) {
                // Thực hiện tính toán nặng trên luồng Default (luồng tính toán)
                withContext(Dispatchers.Default) {
                    val typeEnum = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
                    val range = getTimeRange(selectedPeriod)

                    val filtered = result.data.filter {
                        it.type == typeEnum && it.timestamp >= range.first && it.timestamp <= range.second
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
            } else emptyList()
        }.flowOn(Dispatchers.Default) // Đảm bảo toàn bộ chuỗi Flow chạy trên Default
        .distinctUntilChanged()

    fun deleteExpense(expense: Expense, onSuccess: () -> Unit) {
        // Kiểm tra an toàn: Nếu không có ID Firestore thì không xử lý xóa
        if (expense.firestoreDocId.isEmpty()) return

        viewModelScope.launch {
            //  ĐÃ SỬA: Truyền 'expense.firestoreDocId' (Kiểu String) thay vì truyền cả cục 'expense'
            repository.deleteExpense(expense.firestoreDocId)

            // Kích hoạt callback báo hiệu xóa thành công để UI cập nhật (ví dụ: đóng BottomSheet, ẩn Dialog)
            onSuccess()
        }
    }

    // Hàm lấy danh sách giao dịch theo Category ID (Dùng cho DetailListScreen)
    fun getExpensesByCategory(categoryId: Long, type: String): Flow<List<Expense>> {
        return repository.getAllExpenses().map { result ->
            if (result is Result.Success) {
                result.data.filter {
                    it.category.id == categoryId &&
                            (if (type == "CHI PHÍ") it.type == TransactionType.SPEND else it.type == TransactionType.INCOME)
                }
            } else emptyList()
        }
    }

    fun getFilteredExpenses(
        type: String,
        period: String,
        categoryId: Long = 0
    ): Flow<List<Expense>> = _expensesState.map { result ->
        if (result is Result.Success) {
            val range = getTimeRange(period)
            val typeEnum = if (type == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME

            var list = result.data.filter {
                val matchType = it.type == typeEnum
                val matchTime = it.timestamp >= range.first && it.timestamp <= range.second
                val matchCat = if (categoryId == 0L) true else it.category.id == categoryId
                matchType && matchTime && matchCat
            }

            // Sắp xếp
            list = if (detailSortType == "Số tiền") {
                list.sortedByDescending { it.amount }
            } else {
                list.sortedByDescending { it.timestamp }
            }
            list
        } else emptyList()
    }.flowOn(Dispatchers.Default)

    fun moveTimeRange(delta: Int) {
        val newCalendar = currentCalendar.clone() as Calendar
        when (selectedPeriod) { // selectedPeriod là biến bạn quản lý trong ViewModel
            "Ngày" -> newCalendar.add(Calendar.DAY_OF_YEAR, delta)
            "Tuần" -> newCalendar.add(Calendar.WEEK_OF_YEAR, delta)
            "Tháng" -> newCalendar.add(Calendar.MONTH, delta)
            "Năm" -> newCalendar.add(Calendar.YEAR, delta)
        }
        currentCalendar = newCalendar
        loadAllExpenses() // Gọi lại hàm load từ DB
    }
    fun refreshExpenses() {
        android.util.Log.d("HomeViewModel", "Refreshing expenses...")
        loadAllExpenses()
    }
}