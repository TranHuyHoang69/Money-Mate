package com.example.moneymate.domain.model

data class ReceiptScanResult(
    val rawText: String,
    val amount: Double? = null,
    val amountCandidates: List<Double> = emptyList(),
    val dateMillis: Long? = null,
    val merchantName: String? = null,
    val suggestedCategoryTitle: String = "Ăn uống",
    val note: String = "",
    val confidence: Float = 0f,
    val isLearningApplied: Boolean = false,
    val isAiCategorySuggested: Boolean = false,
    val isLongReceipt: Boolean = false,
    val amountCandidateCount: Int = 0,
    val amountWarning: String? = null,
    val amountSourceLine: String? = null,
    val amountSourceReason: String? = null
)
