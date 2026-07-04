package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Budget
import com.example.moneymate.domain.model.BudgetProgress
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.repository.BudgetRepository
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class BudgetUiState(
    val month: Int,
    val year: Int,
    val displayMonth: String,
    val budgets: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val monthFormatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())

    private val _uiState = MutableStateFlow(
        BudgetUiState(
            month = calendar.get(Calendar.MONTH) + 1,
            year = calendar.get(Calendar.YEAR),
            displayMonth = monthFormatter.format(calendar.time)
        )
    )
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private var budgetJob: Job? = null

    init {
        observeCategories()
        observeBudgets()
    }

    fun movePreviousMonth() {
        calendar.add(Calendar.MONTH, -1)
        updatePeriodAndReload()
    }

    fun moveNextMonth() {
        calendar.add(Calendar.MONTH, 1)
        updatePeriodAndReload()
    }

    fun saveBudget(category: Category, amountInput: String) {
        val amount = parseAmount(amountInput)
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(error = "Số tiền ngân sách không hợp lệ")
            return
        }

        val current = _uiState.value
        val existing = current.budgets.firstOrNull { it.budget.categoryId == category.id }?.budget

        viewModelScope.launch {
            budgetRepository.saveBudget(
                Budget(
                    id = existing?.id ?: 0,
                    firestoreDocId = existing?.firestoreDocId.orEmpty(),
                    categoryId = category.id,
                    categoryStableId = category.stableId,
                    categoryTitle = category.title,
                    categoryColorHex = category.colorHex,
                    amount = amount,
                    month = current.month,
                    year = current.year
                )
            )
        }
    }

    fun deleteBudget(progress: BudgetProgress) {
        viewModelScope.launch {
            val result = budgetRepository.deleteBudget(progress.budget)
            if (result is Result.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun observeCategories() {
        viewModelScope.launch {
            expenseRepository.getCategoriesByType("SPEND").collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(categories = result.data)
                }
            }
        }
    }

    private fun observeBudgets() {
        budgetJob?.cancel()
        val state = _uiState.value
        budgetJob = viewModelScope.launch {
            budgetRepository.getBudgetProgress(state.month, state.year).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                    is Result.Success -> _uiState.value.copy(
                        budgets = result.data,
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

    private fun updatePeriodAndReload() {
        _uiState.value = _uiState.value.copy(
            month = calendar.get(Calendar.MONTH) + 1,
            year = calendar.get(Calendar.YEAR),
            displayMonth = monthFormatter.format(calendar.time),
            isLoading = true
        )
        observeBudgets()
    }

    private fun parseAmount(input: String): Double {
        return input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    }
}
