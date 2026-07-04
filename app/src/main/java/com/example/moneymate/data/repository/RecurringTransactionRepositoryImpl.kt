package com.example.moneymate.data.repository

import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryStableId
import com.example.moneymate.data.local.RecurringTransactionDao
import com.example.moneymate.data.local.toDomain
import com.example.moneymate.data.local.toEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.RecurringTransaction
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.repository.RecurringTransactionRepository
import com.example.moneymate.util.RecurringTransactionSchedule
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class RecurringTransactionRepositoryImpl @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val categoryDao: CategoryDao,
    private val expenseRepository: ExpenseRepository,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) : RecurringTransactionRepository {

    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: "guest"

    private val pendingDeletedDocIds = mutableSetOf<String>()

    override fun observeRecurringTransactions(): Flow<Result<List<RecurringTransaction>>> {
        return combine(
            recurringTransactionDao.observeByUser(currentUserId),
            firestoreDataSource.observeUserRecurringTransactions()
        ) { localTransactions, remoteResult ->
            if (remoteResult is Result.Success) {
                val remoteDocIds = remoteResult.data.map { it.firestoreDocId }.toSet()
                pendingDeletedDocIds.removeAll { it !in remoteDocIds }

                val remoteTransactions = remoteResult.data
                    .filterNot { it.firestoreDocId in pendingDeletedDocIds }
                    .map { it.withLocalCategoryId() }

                if (remoteTransactions.isNotEmpty()) {
                    recurringTransactionDao.insertAll(remoteTransactions)
                }
            }

            when (remoteResult) {
                is Result.Error -> Result.Error(remoteResult.message)
                else -> Result.Success(localTransactions.map { it.toDomain() })
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveRecurringTransaction(transaction: RecurringTransaction): Result<Unit> {
        return try {
            val docId = transaction.firestoreDocId.ifEmpty { "recurring_${System.currentTimeMillis()}" }
            val transactionWithStableCategory = transaction.withStableCategory()
            val entity = transactionWithStableCategory.copy(
                userId = currentUserId,
                firestoreDocId = docId,
                nextRunAt = if (transaction.nextRunAt <= 0L) {
                    transaction.startDate
                } else {
                    transaction.nextRunAt
                }
            ).toEntity()

            recurringTransactionDao.insert(entity)
            when (val remoteResult = firestoreDataSource.saveRecurringTransactionToRemote(entity)) {
                is Result.Success -> {
                    if (remoteResult.data != docId) {
                        recurringTransactionDao.insert(entity.copy(firestoreDocId = remoteResult.data))
                    }
                    Result.Success(Unit)
                }
                is Result.Error -> Result.Error(remoteResult.message)
                is Result.Loading -> Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể lưu giao dịch định kỳ")
        }
    }

    override suspend fun deleteRecurringTransaction(transaction: RecurringTransaction): Result<Unit> {
        return try {
            val docId = transaction.firestoreDocId
            if (docId.isNotEmpty()) {
                pendingDeletedDocIds.add(docId)
                recurringTransactionDao.deleteByFirestoreDocId(docId)
            } else {
                recurringTransactionDao.delete(transaction.copy(userId = currentUserId).toEntity())
            }

            if (docId.isNotEmpty()) {
                when (val remoteResult = firestoreDataSource.deleteRecurringTransactionFromRemote(docId)) {
                    is Result.Error -> {
                        pendingDeletedDocIds.remove(docId)
                        return Result.Error(remoteResult.message)
                    }
                    else -> Unit
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể xóa giao dịch định kỳ")
        }
    }

    override suspend fun processDueTransactions(): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val dueTransactions = recurringTransactionDao.getDueTransactions(currentUserId, now)
            var generatedCount = 0

            dueTransactions.forEach { entity ->
                val transaction = entity.toDomain()
                val dueRunCalculation = RecurringTransactionSchedule.calculateDueRuns(
                    nextRunAt = entity.nextRunAt,
                    lastGeneratedAt = entity.lastGeneratedAt,
                    now = now,
                    storedRepeat = entity.repeatInterval,
                    maxCatchUpRuns = MAX_CATCH_UP_RUNS
                )

                dueRunCalculation.generatedRunTimes.forEach { runAt ->
                    val expense = Expense(
                        type = transaction.type,
                        amount = transaction.amount,
                        category = transaction.category,
                        timestamp = runAt,
                        note = transaction.note.ifBlank { "Giao dịch định kỳ" }
                    )

                    when (val result = expenseRepository.insertExpense(expense)) {
                        is Result.Error -> return Result.Error(result.message)
                        else -> Unit
                    }

                    generatedCount++
                }

                val updatedEntity = entity.copy(
                    nextRunAt = dueRunCalculation.nextRunAt,
                    lastGeneratedAt = dueRunCalculation.lastGeneratedAt
                )
                recurringTransactionDao.update(updatedEntity)
                firestoreDataSource.saveRecurringTransactionToRemote(updatedEntity)
            }

            Result.Success(generatedCount)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể xử lý giao dịch định kỳ")
        }
    }

    private companion object {
        const val MAX_CATCH_UP_RUNS = 24
    }

    private suspend fun RecurringTransaction.withStableCategory(): RecurringTransaction {
        if (categoryStableId.isNotBlank()) return this

        val localCategory = categoryDao.getCategoryById(categoryId)
        val stableId = localCategory?.stableId?.takeIf { it.isNotBlank() }
            ?: CategoryStableId.legacyStableId(categoryId)
        return copy(categoryStableId = stableId)
    }

    private suspend fun com.example.moneymate.data.local.RecurringTransactionEntity.withLocalCategoryId(): com.example.moneymate.data.local.RecurringTransactionEntity {
        val localCategory = categoryStableId
            .takeIf { it.isNotBlank() }
            ?.let { categoryDao.getCategoryByStableId(it, currentUserId) }
            ?: categoryDao.getCategoryByNameAndType(categoryTitle, type, currentUserId)
            ?: return this
        return copy(
            categoryId = localCategory.categoryId,
            categoryStableId = localCategory.stableId
        )
    }
}
