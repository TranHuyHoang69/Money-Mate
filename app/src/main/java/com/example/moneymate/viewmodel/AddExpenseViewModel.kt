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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
    val currentFirestoreDocId: String = ""
)

// --- EVENTS ---
sealed class AddExpenseEvent {
    data class LoadDetailsByFirestoreId(val id: String) : AddExpenseEvent()
    data class ChangeAmount(val amount: String) : AddExpenseEvent()
    data class ChangeNote(val note: String) : AddExpenseEvent()
    data class ChangeType(val type: TransactionType) : AddExpenseEvent()
    data class SelectCategory(val category: Category) : AddExpenseEvent()
    data class ChangeDate(val timestamp: Long) : AddExpenseEvent()
    data class LoadDetails(val expenseId: Long) : AddExpenseEvent()
    object Save : AddExpenseEvent()
    object Delete : AddExpenseEvent() // ✅ ĐÃ THÊM: Event phục vụ cho nút Xóa
}

// --- UI SIDE EFFECTS ---
sealed class AddExpenseUiEvent {
    object SaveSuccess : AddExpenseUiEvent()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val useCases: ExpenseUseCases
) : ViewModel() {

    var uiState by mutableStateOf(AddExpenseUiState())
        private set

    private val _eventChannel = kotlinx.coroutines.channels.Channel<AddExpenseUiEvent>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    val eventFlow: kotlinx.coroutines.flow.Flow<AddExpenseUiEvent> = _eventChannel.receiveAsFlow()

    init {
        loadCategories()
    }

    fun onEvent(event: AddExpenseEvent) {
        when (event) {
            is AddExpenseEvent.LoadDetailsByFirestoreId -> {
                loadExpenseDetailsByFirestoreId(event.id)
            }
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
                        selectedCategory = null
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
                saveExpense()
            }
            is AddExpenseEvent.Delete -> { // ✅ ĐÃ THÊM: Bắt sự kiện Xóa khi click nút Xóa ở UI
                deleteExpense()
            }
        }
    }

    private fun loadCategories() {
        useCases.getAllCategories(uiState.selectedType.name)
            .onEach { result ->
                uiState = uiState.copy(categories = result)
                if (result is Result.Success) {
                    if (uiState.selectedCategory != null &&
                        result.data.none { it.id == uiState.selectedCategory?.id }) {
                        uiState = uiState.copy(selectedCategory = null)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadExpenseDetailsByFirestoreId(id: String) {
        if (id.isEmpty() || uiState.currentFirestoreDocId == id) return

        viewModelScope.launch {
            val result = useCases.getExpenseByFirestoreId(id).first()

            if (result is Result.Success<Expense?>) {
                val expense = result.data

                if (expense != null) {
                    uiState = uiState.copy(
                        // ✅ ĐÃ SỬA AN TOÀN: Dùng trực tiếp tham số `id` truyền từ màn hình vào để đảm bảo không bị rỗng
                        currentFirestoreDocId = id,
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

    private fun loadExpenseDetails(expenseId: Long) {
        if (expenseId == -1L) return
        viewModelScope.launch {
            val result = useCases.getExpenseById(expenseId).first()
            if (result is Result.Success && result.data != null) {
                val expense = result.data
                uiState = uiState.copy(
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

    private fun saveExpense() {
        val amountDouble = uiState.amount.toDoubleOrNull() ?: 0.0
        val category = uiState.selectedCategory ?: return

        val expense = Expense(
            firestoreDocId = uiState.currentFirestoreDocId,
            amount = amountDouble,
            note = uiState.note,
            type = uiState.selectedType,
            category = category,
            timestamp = uiState.selectedDate
        )

        viewModelScope.launch {
            try {
                // ✅ Được bọc try-catch phòng trường hợp mạng lỗi hoặc Firestore từ chối ghi tài liệu
                if (uiState.currentFirestoreDocId.isEmpty()) {
                    useCases.addExpense(expense)
                } else {
                    useCases.updateExpense(expense)
                }
                // Phát tín hiệu đóng màn hình về UI lập tức
                _eventChannel.trySend(AddExpenseUiEvent.SaveSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ ĐÃ THÊM: Logic xử lý Xóa bản ghi bằng Firestore ID chuỗi
    private fun deleteExpense() {
        val currentId = uiState.currentFirestoreDocId
        if (currentId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Gọi xuống UseCase xóa trên Firestore
                useCases.deleteExpense(currentId)
                // Phát tín hiệu đóng màn hình về UI lập tức sau khi xóa thành công
                _eventChannel.trySend(AddExpenseUiEvent.SaveSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}