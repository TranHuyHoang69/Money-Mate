package com.example.moneymate.di

import com.example.moneymate.data.repository.AuthRepositoryImpl
import com.example.moneymate.data.repository.ExpenseRepositoryImpl
import com.example.moneymate.domain.repository.AuthRepository
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.usecase.ExpenseUseCases
import com.example.moneymate.domain.usecase.category.AddCategoryUseCase
import com.example.moneymate.domain.usecase.category.DeleteCategoryUseCase
import com.example.moneymate.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymate.domain.usecase.category.UpdateCategoryUseCase
import com.example.moneymate.domain.usecase.expense.AddExpenseUseCase
import com.example.moneymate.domain.usecase.expense.DeleteExpenseUseCase
import com.example.moneymate.domain.usecase.expense.GetAllExpensesUseCase
import com.example.moneymate.domain.usecase.expense.GetExpenseByIdUseCase
import com.example.moneymate.domain.usecase.expense.GetExpensesByPeriodUseCase
import com.example.moneymate.domain.usecase.expense.UpdateExpenseUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository


    companion object{
        @Provides
        @Singleton
        fun provideExpenseUseCases(repository: ExpenseRepository): ExpenseUseCases {
            return ExpenseUseCases(
                getAllExpenses = GetAllExpensesUseCase(repository),
                getExpensesByPeriod = GetExpensesByPeriodUseCase(repository),
                getExpenseById = GetExpenseByIdUseCase(repository),
                addExpense = AddExpenseUseCase(repository),
                updateExpense = UpdateExpenseUseCase(repository),
                deleteExpense = DeleteExpenseUseCase(repository),
                getAllCategories = GetAllCategoriesUseCase(repository),
                addCategory = AddCategoryUseCase(repository),
                updateCategory = UpdateCategoryUseCase(repository),
                deleteCategory = DeleteCategoryUseCase(repository)
            )
        }
    }
}