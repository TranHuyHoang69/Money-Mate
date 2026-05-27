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
        type = try {
            TransactionType.valueOf(this.type)
        } catch (e: Exception) {
            TransactionType.SPEND
        },
        isDefault = this.isDefault
    )
}

// 🟢 NHẬN THÊM THAM SỐ userId do đối tượng Category (Domain) của bạn không có trường này
fun Category.toEntity(userId: String): CategoryEntity {
    return CategoryEntity(
        categoryId = this.id,
        userId = userId,
        title = this.title,
        iconResName = this.iconResName,
        colorHex = this.colorHex,
        type = this.type.name,
        isDefault = this.isDefault
    )
}

/**
 * Mappers cho Expense
 */
fun ExpenseWithCategory.toDomain(): Expense {
    return Expense(
        id = this.expense.id,
        firestoreDocId = this.expense.firestoreDocId,
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

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = this.id,
        firestoreDocId = this.firestoreDocId,
        type = this.type.name,
        amount = this.amount,
        categoryId = this.category.id,
        timestamp = this.timestamp,
        note = this.note
    )
}