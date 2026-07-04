package com.example.moneymate.domain.usecase.expense

object ExpenseAmountValidator {
    const val ERROR_MESSAGE = "Số tiền phải lớn hơn 0"

    fun parseValidAmount(value: String): Double? {
        return value.trim().toDoubleOrNull()?.takeIf { isValidAmount(it) }
    }

    fun isValidAmount(value: Double): Boolean {
        return value > 0.0
    }

    fun errorOrNull(value: String): String? {
        return if (value.isBlank() || parseValidAmount(value) == null) {
            ERROR_MESSAGE
        } else {
            null
        }
    }
}
