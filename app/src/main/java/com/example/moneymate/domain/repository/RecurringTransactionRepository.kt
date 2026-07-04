package com.example.moneymate.domain.repository

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun observeRecurringTransactions(): Flow<Result<List<RecurringTransaction>>>
    suspend fun saveRecurringTransaction(transaction: RecurringTransaction): Result<Unit>
    suspend fun deleteRecurringTransaction(transaction: RecurringTransaction): Result<Unit>
    suspend fun processDueTransactions(): Result<Int>
}
