package com.example.moneymate.viewmodel

// QUAN TRỌNG: Đảm bảo import đúng Category của project
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    var amount by mutableStateOf("")
    var note by mutableStateOf("")
    var selectedType by mutableStateOf("CHI PHÍ")

    // Đảm bảo kiểu dữ liệu là Category? từ model của bạn
    var selectedCategory by mutableStateOf<Category?>(null)
    var currentExpenseId by mutableStateOf(-1L)
    var selectedDate by mutableStateOf(System.currentTimeMillis())
    private val _categories = MutableStateFlow<Result<List<Category>>>(Result.Loading)
    val categories: StateFlow<Result<List<Category>>> = _categories

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            // Đảm bảo repository.getAllCategories() trả về Flow
            repository.getAllCategories().collect { _categories.value = it }
        }
    }

    fun loadExpenseDetails(expenseId: Long) {
        if (expenseId == -1L || currentExpenseId == expenseId) return
        currentExpenseId = expenseId

        viewModelScope.launch {
            // Vì getExpenseById trả về Flow<Result<Expense?>>
            // Chúng ta dùng .collect để lắng nghe dữ liệu
            repository.getExpenseById(expenseId).collect { result ->
                if (result is Result.Success) {
                    val expenseData = result.data
                    if (expenseData != null) {
                        amount = expenseData.amount.toString()
                        note = expenseData.note
                        selectedType = if (expenseData.type == TransactionType.SPEND) "CHI PHÍ" else "THU NHẬP"
                        selectedCategory = expenseData.category
                        selectedDate = expenseData.timestamp
                    }
                }
            }
        }
    }

    fun onAmountChange(newValue: String) {
        if (newValue.all { it.isDigit() || it == '.' }) amount = newValue
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        val type = if (selectedType == "CHI PHÍ") TransactionType.SPEND else TransactionType.INCOME
        val category = selectedCategory ?: return

        viewModelScope.launch {
            val expense = Expense(
                id = if (currentExpenseId == -1L) 0 else currentExpenseId,
                amount = amountDouble,
                note = note,
                type = type,
                category = category,
                // SỬA TẠI ĐÂY: Thay System.currentTimeMillis() bằng selectedDate
                timestamp = selectedDate
            )

            if (currentExpenseId == -1L) {
                repository.insertExpense(expense)
            } else {
                repository.updateExpense(expense)
            }

            onSuccess()
        }
    }

    fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

