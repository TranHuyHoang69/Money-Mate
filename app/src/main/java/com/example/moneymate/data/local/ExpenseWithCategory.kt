package com.example.moneymate.data.local

import androidx.room.Embedded
import androidx.room.Relation


class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "categoryId"
    )
    val categoryEntity: CategoryEntity
)