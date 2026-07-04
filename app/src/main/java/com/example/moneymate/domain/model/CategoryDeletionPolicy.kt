package com.example.moneymate.domain.model

data class CategoryUsage(
    val expenseCount: Int = 0,
    val budgetCount: Int = 0,
    val recurringTransactionCount: Int = 0
) {
    val isUsed: Boolean
        get() = expenseCount > 0 || budgetCount > 0 || recurringTransactionCount > 0
}

object CategoryDeletionPolicy {
    fun blockingMessage(usage: CategoryUsage): String? {
        if (!usage.isUsed) return null

        val details = buildList {
            if (usage.expenseCount > 0) add("$expenseLabel: ${usage.expenseCount}")
            if (usage.budgetCount > 0) add("$budgetLabel: ${usage.budgetCount}")
            if (usage.recurringTransactionCount > 0) {
                add("$recurringLabel: ${usage.recurringTransactionCount}")
            }
        }.joinToString(", ")

        return "Không thể xóa danh mục đang được sử dụng ($details)."
    }

    private const val expenseLabel = "giao dịch"
    private const val budgetLabel = "ngân sách"
    private const val recurringLabel = "giao dịch định kỳ"
}
