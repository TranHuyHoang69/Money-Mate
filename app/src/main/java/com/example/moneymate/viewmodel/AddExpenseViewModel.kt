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
import com.example.moneymate.domain.usecase.ExpenseUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI STATE ---
data class AddExpenseUiState(
    val amount: String = "",
    val note: String = "",
    val selectedType: TransactionType = TransactionType.SPEND,
    val selectedCategory: Category? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val categories: Result<List<Category>> = Result.Loading,
    val currentExpenseId: Long = -1L
)

// --- EVENTS ---
sealed class AddExpenseEvent {
    data class ChangeAmount(val amount: String) : AddExpenseEvent()
    data class ChangeNote(val note: String) : AddExpenseEvent()
    data class ChangeType(val type: TransactionType) : AddExpenseEvent()
    data class SelectCategory(val category: Category) : AddExpenseEvent()
    data class ChangeDate(val timestamp: Long) : AddExpenseEvent()
    data class LoadDetails(val expenseId: Long) : AddExpenseEvent()
    data class Save(val onSuccess: () -> Unit) : AddExpenseEvent()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val useCases: ExpenseUseCases // Inject bộ UseCases
) : ViewModel() {

    // Đây là biến duy nhất UI cần quan sát
    var uiState by mutableStateOf(AddExpenseUiState())
        private set

    init {
        loadCategories()
    }

    // Cổng duy nhất nhận tương tác từ UI
    fun onEvent(event: AddExpenseEvent) {
        when (event) {
            is AddExpenseEvent.ChangeAmount -> {
                if (event.amount.all { it.isDigit() || it == '.' } && event.amount.count { it == '.' } <= 1) {
                    uiState = uiState.copy(amount = event.amount)
                }
            }
            is AddExpenseEvent.ChangeNote -> {
                uiState = uiState.copy(note = event.note)
            }
            is AddExpenseEvent.ChangeType -> {
                if (uiState.selectedType != event.type) {
                    uiState = uiState.copy(
                        selectedType = event.type,
                        selectedCategory = null // Reset category khi đổi tab
                    )
                    loadCategories()
                }
            }
            is AddExpenseEvent.SelectCategory -> {
                uiState = uiState.copy(selectedCategory = event.category)
            }
            is AddExpenseEvent.ChangeDate -> {
                uiState = uiState.copy(selectedDate = event.timestamp)
            }
            is AddExpenseEvent.LoadDetails -> {
                loadExpenseDetails(event.expenseId)
            }
            is AddExpenseEvent.Save -> {
                saveExpense(event.onSuccess)
            }
        }
    }

    private fun loadCategories() {
        // Sử dụng UseCase để lấy danh sách danh mục theo type
        // Lưu ý: Type truyền vào là Enum .name ("SPEND" hoặc "INCOME")
        useCases.getAllCategories(uiState.selectedType.name)
            .onEach { result ->
                uiState = uiState.copy(categories = result)

                // Nếu đang có category được chọn mà tab mới không có category đó thì reset
                if (result is Result.Success) {
                    if (uiState.selectedCategory != null &&
                        result.data.none { it.id == uiState.selectedCategory?.id }) {
                        uiState = uiState.copy(selectedCategory = null)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadExpenseDetails(expenseId: Long) {
        if (expenseId == -1L || uiState.currentExpenseId == expenseId) return

        viewModelScope.launch {
            useCases.getExpenseById(expenseId).collect { result ->
                if (result is Result.Success && result.data != null) {
                    val expense = result.data
                    uiState = uiState.copy(
                        currentExpenseId = expense.id,
                        amount = expense.amount.toString(),
                        note = expense.note,
                        selectedType = expense.type,
                        selectedCategory = expense.category,
                        selectedDate = expense.timestamp
                    )
                    loadCategories()
                }
            }
        }
    }

    private fun saveExpense(onSuccess: () -> Unit) {
        val amountDouble = uiState.amount.toDoubleOrNull() ?: 0.0
        val category = uiState.selectedCategory ?: return

        val expense = Expense(
            id = if (uiState.currentExpenseId == -1L) 0 else uiState.currentExpenseId,
            amount = amountDouble,
            note = uiState.note,
            type = uiState.selectedType,
            category = category,
            timestamp = uiState.selectedDate
        )

        viewModelScope.launch {
            val result = if (uiState.currentExpenseId == -1L) {
                useCases.addExpense(expense)
            } else {
                useCases.updateExpense(expense)
            }

            if (result is Result.Success) {
                onSuccess()
            }
        }
    }
}
