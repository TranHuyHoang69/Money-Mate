package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.RecurringTransaction
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringTransactionUiState(
    val transactions: List<RecurringTransaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val generatedCount: Int = 0
)

@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringTransactionUiState())
    val uiState: StateFlow<RecurringTransactionUiState> = _uiState.asStateFlow()

    init {
        processDueTransactions()
        observeTransactions()
        observeCategories()
    }

    fun saveTransaction(
        existing: RecurringTransaction?,
        type: TransactionType,
        amountInput: String,
        category: Category,
        note: String,
        repeatInterval: String,
        startDate: Long,
        isActive: Boolean,
        onSaved: () -> Unit = {}
    ) {
        val amount = amountInput.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(error = "Số tiền không hợp lệ")
            return
        }

        viewModelScope.launch {
            val result = recurringRepository.saveRecurringTransaction(
                RecurringTransaction(
                    id = existing?.id ?: 0L,
                    firestoreDocId = existing?.firestoreDocId.orEmpty(),
                    userId = existing?.userId.orEmpty(),
                    type = type,
                    amount = amount,
                    categoryId = category.id,
                    categoryStableId = category.stableId,
                    categoryTitle = category.title,
                    categoryColorHex = category.colorHex,
                    note = note.trim(),
                    repeatInterval = repeatInterval,
                    startDate = startDate,
                    nextRunAt = existing?.nextRunAt?.takeIf { it >= startDate } ?: startDate,
                    lastGeneratedAt = existing?.lastGeneratedAt,
                    isActive = isActive
                )
            )
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                processDueTransactions()
                onSaved()
            }
        }
    }

    fun toggleActive(transaction: RecurringTransaction, active: Boolean) {
        viewModelScope.launch {
            val result = recurringRepository.saveRecurringTransaction(transaction.copy(isActive = active))
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else if (active) {
                processDueTransactions()
            }
        }
    }

    fun deleteTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            val result = recurringRepository.deleteRecurringTransaction(transaction)
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(error = null, generatedCount = 0)
    }

    private fun processDueTransactions() {
        viewModelScope.launch {
            when (val result = recurringRepository.processDueTransactions()) {
                is Result.Success -> {
                    if (result.data > 0) {
                        _uiState.value = _uiState.value.copy(generatedCount = result.data)
                    }
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                is Result.Loading -> Unit
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            recurringRepository.observeRecurringTransactions().collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                    is Result.Success -> _uiState.value.copy(
                        transactions = result.data,
                        isLoading = false,
                        error = null
                    )
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            expenseRepository.getAllCategories().collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(categories = result.data)
                }
            }
        }
    }
}
