    package com.example.moneymate.di

    import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.usecase.ExpenseUseCases
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

    @Module
    @InstallIn(SingletonComponent::class)
    object UseCaseModule {

        @Provides
        @Singleton
        fun provideExpenseUseCases(repository: ExpenseRepository): ExpenseUseCases {
            return ExpenseUseCases(
                // Expense UseCases
                getAllExpenses = GetAllExpensesUseCase(repository),
                getExpensesByPeriod = GetExpensesByPeriodUseCase(repository),
                getExpenseById = GetExpenseByIdUseCase(repository),
                addExpense = AddExpenseUseCase(repository),
                updateExpense = UpdateExpenseUseCase(repository),
                deleteExpense = DeleteExpenseUseCase(repository),
                getExpenseByFirestoreId = GetExpenseByFirestoreIdUseCase(repository),

                // Category UseCases
                getAllCategories = GetAllCategoriesUseCase(repository),
                addCategory = AddCategoryUseCase(repository),
                updateCategory = UpdateCategoryUseCase(repository),
                deleteCategory = DeleteCategoryUseCase(repository)
            )
        }
    }