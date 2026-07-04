package com.example.moneymate.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.repository.ReceiptLearningRepository
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.ReceiptScanResult
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.usecase.ExpenseUseCases
import com.example.moneymate.util.GeminiCategorySuggestionService
import com.example.moneymate.util.ReceiptOcrProcessor
import com.example.moneymate.util.ReceiptTextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptScanUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: Uri? = null,
    val rawText: String = "",
    val parsedResult: ReceiptScanResult? = null,
    val error: String = ""
)

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val receiptOcrProcessor: ReceiptOcrProcessor,
    private val receiptLearningRepository: ReceiptLearningRepository,
    private val geminiCategorySuggestionService: GeminiCategorySuggestionService,
    private val expenseUseCases: ExpenseUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    fun scanReceipt(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.update { it.copy(error = "Không có ảnh hóa đơn được chọn") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedImageUri = imageUri,
                    rawText = "",
                    parsedResult = null,
                    error = ""
                )
            }

            try {
                val rawText = receiptOcrProcessor.recognizeText(imageUri)
                val availableCategoryTitles = loadSpendCategoryTitles()
                val localParsedResult = ReceiptTextParser.parse(
                    rawText = rawText,
                    availableCategories = availableCategoryTitles
                )
                val geminiCategory = geminiCategorySuggestionService.suggestCategory(
                    rawText = rawText,
                    merchantName = localParsedResult.merchantName,
                    localCategory = localParsedResult.suggestedCategoryTitle,
                    availableCategories = availableCategoryTitles
                )
                val aiParsedResult = geminiCategory?.let { category ->
                    localParsedResult.copy(
                        suggestedCategoryTitle = category,
                        isAiCategorySuggested = true
                    )
                } ?: localParsedResult
                val parsedResult = receiptLearningRepository.applyLearnedSelection(
                    aiParsedResult
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rawText = rawText,
                        parsedResult = parsedResult,
                        error = if (rawText.isBlank()) {
                            "Không đọc được chữ từ ảnh hóa đơn"
                        } else {
                            ""
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Không thể quét hóa đơn"
                    )
                }
            }
        }
    }

    fun clearResult() {
        _uiState.value = ReceiptScanUiState()
    }

    fun saveUserSelection(result: ReceiptScanResult) {
        viewModelScope.launch(Dispatchers.IO) {
            receiptLearningRepository.saveUserSelection(result)
        }
    }

    fun forgetLearningForCurrentResult() {
        val currentResult = _uiState.value.parsedResult ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val deleted = receiptLearningRepository.forgetUserSelection(currentResult)
            val reparsedResult = ReceiptTextParser.parse(
                rawText = currentResult.rawText,
                availableCategories = loadSpendCategoryTitles()
            )
            _uiState.update {
                it.copy(
                    parsedResult = reparsedResult,
                    error = if (deleted) {
                        ""
                    } else {
                        "Không tìm thấy mẫu OCR đã học để xóa"
                    }
                )
            }
        }
    }

    private suspend fun loadSpendCategoryTitles(): List<String> {
        return runCatching {
            val result = expenseUseCases
                .getAllCategories(TransactionType.SPEND.name)
                .first { it !is Result.Loading }

            (result as? Result.Success)
                ?.data
                ?.map { it.title }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                .orEmpty()
        }.getOrDefault(emptyList())
    }
}
