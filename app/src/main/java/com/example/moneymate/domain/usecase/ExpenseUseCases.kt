package com.example.moneymate.domain.usecase

import com.example.moneymate.domain.usecase.category.AddCategoryUseCase
import com.example.moneymate.domain.usecase.category.DeleteCategoryUseCase
import com.example.moneymate.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymate.domain.usecase.category.UpdateCategoryUseCase
import com.example.moneymate.domain.usecase.expense.AddExpenseUseCase
import com.example.moneymate.domain.usecase.expense.DeleteExpenseUseCase
import com.example.moneymate.domain.usecase.expense.GetAllExpensesUseCase
import com.example.moneymate.domain.usecase.expense.GetExpenseByFirestoreIdUseCase
import com.example.moneymate.domain.usecase.expense.GetExpenseByIdUseCase
import com.example.moneymate.domain.usecase.expense.GetExpensesByPeriodUseCase
import com.example.moneymate.domain.usecase.expense.UpdateExpenseUseCase

data class ExpenseUseCases(
    // Expense
    val getAllExpenses: GetAllExpensesUseCase,
    val getExpensesByPeriod: GetExpensesByPeriodUseCase,
    val getExpenseById: GetExpenseByIdUseCase,
    val addExpense: AddExpenseUseCase,
    val updateExpense: UpdateExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase,
    val getExpenseByFirestoreId: GetExpenseByFirestoreIdUseCase,

    // Category
    val getAllCategories: GetAllCategoriesUseCase,
    val addCategory: AddCategoryUseCase,
    val updateCategory: UpdateCategoryUseCase,
    val deleteCategory: DeleteCategoryUseCase
)