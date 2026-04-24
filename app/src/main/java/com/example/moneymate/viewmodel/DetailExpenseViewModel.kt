package com.example.moneymate.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseId: Long = savedStateHandle.get<Long>("expenseId") ?: -1L

    // Khởi tạo StateFlow trực tiếp từ Flow của Repository
    // Flow này sẽ tự động phát lại dữ liệu mới mỗi khi Database thay đổi (Update/Delete)
    val expenseState: StateFlow<Result<Expense?>> = repository
        .getExpenseById(expenseId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    // Hàm xóa vẫn giữ nguyên vì nó là hành động (action)
    fun deleteExpense(expense: Expense, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            onSuccess()
        }
    }
}