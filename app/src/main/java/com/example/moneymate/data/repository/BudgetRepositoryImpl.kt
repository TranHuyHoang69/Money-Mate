package com.example.moneymate.data.repository

import android.content.Context
import com.example.moneymate.data.local.BudgetDao
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryStableId
import com.example.moneymate.data.local.toDomain
import com.example.moneymate.data.local.toEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Budget
import com.example.moneymate.domain.model.BudgetProgress
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.BudgetRepository
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.util.BudgetNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val expenseRepository: ExpenseRepository,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : BudgetRepository {

    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: "guest"

    private val notifiedExceededBudgets = mutableSetOf<String>()
    private val pendingDeletedBudgetDocIds = mutableSetOf<String>()

    override fun getBudgetProgress(month: Int, year: Int): Flow<Result<List<BudgetProgress>>> {
        val (start, end) = getMonthRange(month, year)
        return combine(
            budgetDao.getBudgetsByPeriod(currentUserId, month, year),
            expenseRepository.getExpensesByPeriod(start, end),
            firestoreDataSource.observeUserBudgets()
        ) { budgetEntities, expensesResult, remoteBudgetsResult ->
            if (remoteBudgetsResult is Result.Success) {
                val remoteDocIds = remoteBudgetsResult.data.map { it.firestoreDocId }.toSet()
                pendingDeletedBudgetDocIds.removeAll { it !in remoteDocIds }

                val newRemoteBudgets = remoteBudgetsResult.data
                    .filterNot { it.firestoreDocId in pendingDeletedBudgetDocIds }
                    .map { remote -> remote.withLocalCategoryId() }
                    .filterNot { remote ->
                        budgetEntities.any { local ->
                            local.firestoreDocId == remote.firestoreDocId ||
                                (
                                    local.categoryStableId == remote.categoryStableId &&
                                        local.month == remote.month &&
                                        local.year == remote.year
                                )
                        }
                    }

                if (newRemoteBudgets.isNotEmpty()) {
                    budgetDao.insertBudgets(newRemoteBudgets)
                }
            }

            when (expensesResult) {
                is Result.Success -> {
                    val expenses = expensesResult.data
                    val progress = budgetEntities.map { entity ->
                        val budget = entity.toDomain()
                        val spent = expenses
                            .filter {
                                it.type == TransactionType.SPEND &&
                                    it.category.id == budget.categoryId
                            }
                            .sumOf { it.amount }

                        BudgetProgress(
                            budget = budget,
                            spentAmount = spent
                        )
                    }
                    progress.forEach { maybeNotifyBudgetExceeded(it) }
                    Result.Success(progress)
                }
                is Result.Loading -> Result.Loading
                is Result.Error -> Result.Error(expensesResult.message)
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveBudget(budget: Budget): Result<Unit> = try {
        val budgetWithStableCategory = budget.withStableCategory()
        val docId = budget.firestoreDocId.ifEmpty {
            "${budget.year}_${budget.month}_${budgetWithStableCategory.categoryStableId.ifBlank { budgetWithStableCategory.categoryId.toString() }}"
        }
        val entity = budgetWithStableCategory.copy(
            userId = currentUserId,
            firestoreDocId = docId
        ).toEntity()

        budgetDao.insertBudget(entity)
        when (val remoteResult = firestoreDataSource.saveBudgetToRemote(entity)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(remoteResult.message)
            is Result.Loading -> Result.Success(Unit)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Không thể lưu ngân sách")
    }

    override suspend fun deleteBudget(budget: Budget): Result<Unit> = try {
        val docId = budget.firestoreDocId.ifEmpty {
            "${budget.year}_${budget.month}_${budget.categoryStableId.ifBlank { budget.categoryId.toString() }}"
        }
        pendingDeletedBudgetDocIds.add(docId)
        budgetDao.deleteBudgetByKey(
            userId = currentUserId,
            categoryId = budget.categoryId,
            month = budget.month,
            year = budget.year
        )

        when (val remoteResult = firestoreDataSource.deleteBudgetFromRemote(docId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> {
                pendingDeletedBudgetDocIds.remove(docId)
                Result.Error(remoteResult.message)
            }
            is Result.Loading -> Result.Success(Unit)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Không thể xóa ngân sách")
    }

    private fun maybeNotifyBudgetExceeded(progress: BudgetProgress) {
        val budget = progress.budget
        val key = "${budget.userId}_${budget.year}_${budget.month}_${budget.categoryId}"
        if (progress.isExceeded) {
            if (notifiedExceededBudgets.add(key)) {
                BudgetNotificationHelper.showExceededNotification(context, progress)
            }
        } else {
            notifiedExceededBudgets.remove(key)
        }
    }

    private suspend fun Budget.withStableCategory(): Budget {
        if (categoryStableId.isNotBlank()) return this

        val localCategory = categoryDao.getCategoryById(categoryId)
        val stableId = localCategory?.stableId?.takeIf { it.isNotBlank() }
            ?: CategoryStableId.legacyStableId(categoryId)
        return copy(categoryStableId = stableId)
    }

    private suspend fun com.example.moneymate.data.local.BudgetEntity.withLocalCategoryId(): com.example.moneymate.data.local.BudgetEntity {
        val localCategory = categoryStableId
            .takeIf { it.isNotBlank() }
            ?.let { categoryDao.getCategoryByStableId(it, currentUserId) }
            ?: categoryDao.getCategoryByNameAndType(categoryTitle, "SPEND", currentUserId)
            ?: return this
        return copy(
            categoryId = localCategory.categoryId,
            categoryStableId = localCategory.stableId
        )
    }

    private fun getMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return start.timeInMillis to end.timeInMillis
    }
}
