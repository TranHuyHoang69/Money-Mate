package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.moneymate.domain.model.Budget

@Entity(
    tableName = "budgets",
    indices = [
        Index(
            value = ["userId", "categoryId", "month", "year"],
            unique = true
        ),
        Index(
            value = ["userId", "categoryStableId", "month", "year"]
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreDocId: String = "",
    val userId: String,
    val categoryId: Long,
    val categoryStableId: String = "",
    val categoryTitle: String,
    val categoryColorHex: String,
    val amount: Double,
    val month: Int,
    val year: Int
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    firestoreDocId = firestoreDocId,
    userId = userId,
    categoryId = categoryId,
    categoryStableId = categoryStableId,
    categoryTitle = categoryTitle,
    categoryColorHex = categoryColorHex,
    amount = amount,
    month = month,
    year = year
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    firestoreDocId = firestoreDocId,
    userId = userId,
    categoryId = categoryId,
    categoryStableId = categoryStableId,
    categoryTitle = categoryTitle,
    categoryColorHex = categoryColorHex,
    amount = amount,
    month = month,
    year = year
)
