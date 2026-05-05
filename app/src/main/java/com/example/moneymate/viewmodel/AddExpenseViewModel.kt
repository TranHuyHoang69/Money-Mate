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

    // FIX LỖI 1: Sử dụng backing field để định nghĩa custom setter
    private var _selectedType = mutableStateOf(TransactionType.SPEND.name)
    var selectedType: String
        get() = _selectedType.value
        set(value) {
            _selectedType.value = value
            loadCategories() // Tải lại danh mục khi đổi tab
        }

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
            _categories.value = Result.Loading
            repository.getCategoriesByType(selectedType).collect { result ->
                _categories.value = result

                // FIX LỖI 3: Kiểm tra kiểu Success với star projection hoặc gán kiểu cụ thể
                if (result is Result.Success<List<Category>>) {
                    val currentList = result.data
                    // Reset nếu danh mục đã chọn không nằm trong tab mới
                    if (selectedCategory != null && currentList.none { it.id == selectedCategory?.id }) {
                        selectedCategory = null
                    }
                }
            }
        }
    }

    fun loadExpenseDetails(expenseId: Long) {
        if (expenseId == -1L || currentExpenseId == expenseId) return
        currentExpenseId = expenseId
        viewModelScope.launch {
            repository.getExpenseById(expenseId).collect { result ->
                if (result is Result.Success<Expense?>) {
                    result.data?.let { expenseData ->
                        amount = expenseData.amount.toString()
                        note = expenseData.note
                        _selectedType.value = expenseData.type.name
                        selectedCategory = expenseData.category
                        selectedDate = expenseData.timestamp
                    }
                }
            }
        }
    }

    fun onAmountChange(newValue: String) {
        if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
            amount = newValue
        }
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        val type = TransactionType.valueOf(selectedType)
        val category = selectedCategory ?: return

        viewModelScope.launch {
            val expense = Expense(
                id = if (currentExpenseId == -1L) 0 else currentExpenseId,
                amount = amountDouble,
                note = note,
                type = type,
                category = category,
                timestamp = selectedDate
            )
            val result = if (currentExpenseId == -1L) repository.insertExpense(expense) else repository.updateExpense(expense)
            if (result is Result.Success) onSuccess()
        }
    }

    fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}