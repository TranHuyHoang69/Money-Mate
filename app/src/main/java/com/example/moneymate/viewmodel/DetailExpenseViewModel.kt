package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow // ✅ ĐÃ THÊM: Import extension này để hết lỗi receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository // Nhận vào repository
) : ViewModel() {

    private val _expenseState = kotlinx.coroutines.flow.MutableStateFlow<Result<Expense?>>(Result.Loading)
    val expenseState: StateFlow<Result<Expense?>> = _expenseState.asStateFlow()

    private val _eventChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    val eventFlow = _eventChannel.receiveAsFlow()

    fun loadExpenseByFirestoreId(firestoreId: String) {
        if (firestoreId.isEmpty()) {
            _expenseState.value = Result.Error("Mã định danh tài liệu trống")
            return
        }

        viewModelScope.launch {
            _expenseState.value = Result.Loading
            repository.getExpenseByFirestoreId(firestoreId).collect { result ->
                _expenseState.value = result
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        // Kiểm tra an toàn trước khi chạy Coroutine: Nếu ID trống thì không cần làm gì cả
        if (expense.firestoreDocId.isEmpty()) return

        viewModelScope.launch {
            // ✅ ĐÃ SỬA: Chỉ truyền chuỗi ID 'firestoreDocId' thay vì truyền cả Object 'expense'
            repository.deleteExpense(expense.firestoreDocId)

            // Bắn tín hiệu kết thúc qua hàng đợi an toàn để ép UI popBackStack lập tức
            _eventChannel.trySend(Unit)
        }
    }
}