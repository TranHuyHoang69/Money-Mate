package com.example.moneymate.di

import com.example.moneymate.data.repository.AuthRepositoryImpl
import com.example.moneymate.data.repository.ExpenseRepositoryImpl
import com.example.moneymate.domain.repository.AuthRepository
import com.example.moneymate.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
}