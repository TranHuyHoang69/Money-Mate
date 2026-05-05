package com.example.moneymate.data.local

import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType

/**
 * Mappers cho Category
 */
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = this.categoryId,
        title = this.title,
        iconResName = this.iconResName,
        colorHex = this.colorHex,
        type = this.type,    // Map từ Entity sang Domain
        isDefault = this.isDefault
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        categoryId = this.id,
        title = this.title,
        iconResName = this.iconResName,
        colorHex = this.colorHex,
        type = this.type,    // Map từ Domain sang Entity (Hết lỗi tại đây)
        isDefault = this.isDefault
    )
}

/**
 * Mappers cho Expense
 */

// Chuyển từ "Dữ liệu liên kết" (DB) sang "Model nghiệp vụ" (Domain)
// Dùng cho getAllExpenses, getExpenseById, v.v.
fun ExpenseWithCategory.toDomain(): Expense {
    return Expense(
        id = this.expense.id,
        type = try {
            TransactionType.valueOf(this.expense.type)
        } catch (e: Exception) {
            TransactionType.SPEND
        },
        amount = this.expense.amount,
        timestamp = this.expense.timestamp,
        note = this.expense.note,
        category = this.categoryEntity.toDomain()
    )
}

// Chuyển từ "Model nghiệp vụ" (Domain) sang "Thực thể đơn" (DB)
// Dùng cho insertExpense, updateExpense
fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = this.id,
        type = this.type.name,
        amount = this.amount,
        categoryId = this.category.id, // Lấy ID từ object Category để làm khóa ngoại
        timestamp = this.timestamp,
        note = this.note
    )
}