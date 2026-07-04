package com.example.moneymate.data.repository

import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.local.CategoryStableId
import com.example.moneymate.data.local.BudgetDao
import com.example.moneymate.data.local.ExpenseDao
import com.example.moneymate.data.local.ExpenseWithCategory
import com.example.moneymate.data.local.RecurringTransactionDao
import com.example.moneymate.data.local.toDomain
import com.example.moneymate.data.local.toEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.data.remote.FirestoreSyncError
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.CategoryDeletionPolicy
import com.example.moneymate.domain.model.CategoryUsage
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.ExpensePendingOperation
import com.example.moneymate.domain.model.ExpenseSyncStatus
import com.example.moneymate.domain.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) : ExpenseRepository {

    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: "guest"

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllExpenses(): Flow<Result<List<Expense>>> {
        return authUserIdFlow()
            .flatMapLatest { userId ->
                val remoteExpenses = if (userId == null) {
                    flowOf<Result<List<Expense>>>(Result.Success(emptyList()))
                } else {
                    firestoreDataSource.observeUserExpenses().onStart { emit(Result.Loading) }
                }

                combine(
                    expenseDao.getAllExpensesWithCategory(),
                    remoteExpenses
                ) { localExpenses, remoteResult ->
                    if (remoteResult is Result.Success && userId != null) {
                        mergeRemoteExpenses(remoteResult.data)
                    }

                    val result: Result<List<Expense>> = Result.Success(localExpenses.map { it.toDomain() })
                    result
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { emit(Result.Error(it.message ?: "Error")) }
    }

    override fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>> {
        if (firebaseAuth.currentUser == null) {
            return flowOf(Result.Success(emptyList()))
        }

        return getAllExpenses().map { result ->
            when (result) {
                is Result.Success -> Result.Success(result.data.filter { it.timestamp in start..end })
                is Result.Error -> Result.Error(result.message)
                Result.Loading -> Result.Loading
            }
        }
    }

    override fun getExpenseById(id: Long): Flow<Result<Expense?>> =
        expenseDao.getExpenseWithCategoryById(id)
            .map { entity ->
                val result: Result<Expense?> = Result.Success(entity?.toDomain())
                result
            }
            .flowOn(Dispatchers.IO)
            .onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override fun getExpenseByFirestoreId(firestoreId: String): Flow<Result<Expense?>> =
        expenseDao.getExpenseWithCategoryByFirestoreId(firestoreId)
            .map { entity ->
                val result: Result<Expense?> = Result.Success(entity?.toDomain())
                result
            }
            .flowOn(Dispatchers.IO)
            .onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override fun getAllCategories(): Flow<Result<List<Category>>> =
        categoryDao.getAllCategoriesForUser(currentUserId)
            .map { list ->
                val result: Result<List<Category>> = Result.Success(list.map { it.toDomain() })
                result
            }
            .flowOn(Dispatchers.IO)
            .onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override fun getCategoriesByType(type: String): Flow<Result<List<Category>>> =
        categoryDao.getCategoriesByTypeAndUser(type, currentUserId)
            .map { list ->
                val result: Result<List<Category>> = Result.Success(list.map { it.toDomain() })
                result
            }
            .flowOn(Dispatchers.IO)
            .onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override suspend fun insertExpense(expense: Expense): Result<Unit> = try {
        val hasUser = firebaseAuth.currentUser != null
        val now = System.currentTimeMillis()
        val docId = expense.firestoreDocId.ifBlank { newLocalExpenseDocId(hasUser) }
        val expenseWithStableCategory = expense.withStableCategory()
        val localExpense = expenseWithStableCategory.copy(
            firestoreDocId = docId,
            syncStatus = if (hasUser) ExpenseSyncStatus.PENDING_CREATE else ExpenseSyncStatus.SYNCED,
            pendingOperation = if (hasUser) ExpensePendingOperation.CREATE else ExpensePendingOperation.NONE,
            isDeleted = false,
            localUpdatedAt = now,
            lastSyncError = null,
            remoteUpdatedAt = 0L,
            lastSyncAttemptAt = 0L,
            retryCount = 0
        )

        expenseDao.insertExpense(localExpense.toEntity())
        if (hasUser) {
            syncPendingExpenses()
        }

        Result.Success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("ExpenseRepository", "Error inserting expense", e)
        Result.Error(e.message ?: "Error")
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            val existing = expenseDao.getExpenseWithCategoryByFirestoreIdOnce(expense.firestoreDocId)
                ?: return Result.Error("Khong tim thay giao dich local")
            val existingDomain = existing.toDomain()

            if (existingDomain.isDeleted ||
                existingDomain.syncStatus == ExpenseSyncStatus.PENDING_DELETE ||
                existingDomain.pendingOperation == ExpensePendingOperation.DELETE
            ) {
                return Result.Error("Giao dich dang cho xoa, khong the chinh sua")
            }

            val hasUser = firebaseAuth.currentUser != null
            val (nextStatus, nextOperation) = when {
                existingDomain.syncStatus == ExpenseSyncStatus.PENDING_CREATE ||
                    existingDomain.pendingOperation == ExpensePendingOperation.CREATE -> {
                    ExpenseSyncStatus.PENDING_CREATE to ExpensePendingOperation.CREATE
                }
                hasUser -> ExpenseSyncStatus.PENDING_UPDATE to ExpensePendingOperation.UPDATE
                else -> ExpenseSyncStatus.SYNCED to ExpensePendingOperation.NONE
            }

            val expenseWithStableCategory = expense.withStableCategory()
            val localExpense = expenseWithStableCategory.copy(
                id = existing.expense.id,
                firestoreDocId = existing.expense.firestoreDocId,
                syncStatus = nextStatus,
                pendingOperation = nextOperation,
                isDeleted = existing.expense.isDeleted,
                localUpdatedAt = System.currentTimeMillis(),
                lastSyncError = null,
                remoteUpdatedAt = existing.expense.remoteUpdatedAt,
                lastSyncAttemptAt = 0L,
                retryCount = 0
            )

            expenseDao.updateExpense(localExpense.toEntity())
            if (firebaseAuth.currentUser != null) {
                syncPendingExpenses()
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error updating expense", e)
            Result.Error(e.message ?: "Error")
        }
    }

    override suspend fun deleteExpense(firestoreId: String): Result<Unit> {
        return try {
            if (firestoreId.isEmpty()) return Result.Error("ID trong, khong the xoa")

            val existing = expenseDao.getExpenseWithCategoryByFirestoreIdOnce(firestoreId)
                ?: return Result.Error("Khong tim thay giao dich local")
            val existingDomain = existing.toDomain()

            if (firebaseAuth.currentUser == null ||
                existingDomain.syncStatus == ExpenseSyncStatus.PENDING_CREATE ||
                existingDomain.pendingOperation == ExpensePendingOperation.CREATE
            ) {
                expenseDao.deleteExpenseById(existing.expense.id)
                return Result.Success(Unit)
            }

            expenseDao.updateExpense(
                existing.expense.copy(
                    syncStatus = ExpenseSyncStatus.PENDING_DELETE.name,
                    pendingOperation = ExpensePendingOperation.DELETE.name,
                    isDeleted = true,
                    localUpdatedAt = System.currentTimeMillis(),
                    lastSyncError = null,
                    lastSyncAttemptAt = 0L,
                    retryCount = 0
                )
            )
            syncPendingExpenses()

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error deleting expense", e)
            Result.Error(e.message ?: "Error")
        }
    }

    override suspend fun syncPendingExpenses(): Result<Unit> {
        if (firebaseAuth.currentUser == null) return Result.Success(Unit)

        return try {
            val pendingExpenses = expenseDao.getPendingExpensesForSync(
                noneOperation = ExpensePendingOperation.NONE.name,
                failedStatus = ExpenseSyncStatus.FAILED.name,
                legacyPendingStatuses = LEGACY_PENDING_SYNC_STATUSES
            )
            var firstError: String? = null

            pendingExpenses.forEach { pending ->
                val expense = pending.toDomain()
                val operation = pendingOperationFor(expense)
                val syncResult = when (operation) {
                    ExpensePendingOperation.CREATE -> firestoreDataSource.addExpense(expense)
                    ExpensePendingOperation.UPDATE -> firestoreDataSource.updateExpense(
                        docId = expense.firestoreDocId,
                        expense = expense
                    )
                    ExpensePendingOperation.DELETE -> firestoreDataSource.deleteExpense(expense.firestoreDocId)
                    else -> Result.Success(Unit)
                }

                when (syncResult) {
                    is Result.Success -> markPendingExpenseSynced(pending, syncResult)
                    is Result.Error -> {
                        firstError = firstError ?: syncResult.message
                        markPendingExpenseFailed(pending, syncResult.message)
                    }
                    Result.Loading -> Unit
                }
            }

            firstError?.let { Result.Error(it) } ?: Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error syncing pending expenses", e)
            Result.Error(e.message ?: "Error syncing expenses")
        }
    }

    override suspend fun insertCategory(category: Category): Result<Unit> = try {
        categoryDao.insertCategory(CategoryStableId.ensureStableId(category.toEntity(currentUserId)))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = try {
        categoryDao.updateCategory(CategoryStableId.ensureStableId(category.toEntity(currentUserId)))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun deleteCategory(category: Category): Result<Unit> {
        return try {
            val categoryEntity = category.toEntity(currentUserId)
            val usage = getCategoryUsage(categoryEntity)
            val blockingMessage = CategoryDeletionPolicy.blockingMessage(usage)
            if (blockingMessage != null) {
                return Result.Error(blockingMessage)
            }

            categoryDao.deleteCategory(categoryEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error")
        }
    }

    override suspend fun clearAllLocalData(): Result<Unit> = try {
        expenseDao.clearAllExpenses()
        categoryDao.clearAllCategories()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    private suspend fun markPendingExpenseSynced(
        pending: ExpenseWithCategory,
        result: Result.Success<*>
    ) {
        val status = pending.toDomain().syncStatus
        val operation = pendingOperationFor(pending.toDomain())
        val now = System.currentTimeMillis()
        if (status == ExpenseSyncStatus.PENDING_DELETE || operation == ExpensePendingOperation.DELETE) {
            expenseDao.deleteExpenseById(pending.expense.id)
            return
        }

        val syncedDocId = (result.data as? String).orEmpty().ifBlank {
            pending.expense.firestoreDocId
        }
        expenseDao.updateExpense(
            pending.expense.copy(
                firestoreDocId = syncedDocId,
                syncStatus = ExpenseSyncStatus.SYNCED.name,
                pendingOperation = ExpensePendingOperation.NONE.name,
                isDeleted = false,
                remoteUpdatedAt = now,
                lastSyncAttemptAt = now,
                lastSyncError = null,
                retryCount = 0
            )
        )
    }

    private suspend fun markPendingExpenseFailed(
        pending: ExpenseWithCategory,
        message: String?
    ) {
        val retryable = FirestoreSyncError.isRetryable(message)
        val nextStatus = if (retryable) {
            pending.expense.syncStatus
        } else {
            ExpenseSyncStatus.FAILED.name
        }
        expenseDao.updateExpense(
            pending.expense.copy(
                syncStatus = nextStatus,
                lastSyncAttemptAt = System.currentTimeMillis(),
                lastSyncError = message ?: "UNKNOWN: Sync failed",
                retryCount = pending.expense.retryCount + 1
            )
        )
    }

    private suspend fun mergeRemoteExpenses(remoteExpenses: List<Expense>) {
        remoteExpenses.forEach { remoteExpense ->
            try {
                val existing = expenseDao.getExpenseWithCategoryByFirestoreIdOnce(
                    remoteExpense.firestoreDocId
                )
                val existingDomain = existing?.toDomain()
                val existingStatus = existingDomain?.syncStatus
                if (existingStatus in PENDING_STATUSES ||
                    existingDomain?.pendingOperation != ExpensePendingOperation.NONE
                ) {
                    return@forEach
                }

                val localCategory = resolveLocalCategory(remoteExpense.category)
                val localExpense = remoteExpense.copy(
                    id = existing?.expense?.id ?: 0L,
                    category = localCategory,
                    syncStatus = ExpenseSyncStatus.SYNCED,
                    pendingOperation = ExpensePendingOperation.NONE,
                    isDeleted = false,
                    localUpdatedAt = existing?.expense?.localUpdatedAt ?: remoteExpense.timestamp,
                    remoteUpdatedAt = System.currentTimeMillis(),
                    lastSyncError = null,
                    retryCount = 0
                )
                expenseDao.insertAllExpenses(listOf(localExpense.toEntity()))
            } catch (e: Exception) {
                android.util.Log.w(
                    "ExpenseRepository",
                    "Skip remote expense merge ${remoteExpense.firestoreDocId}: ${e.message}"
                )
            }
        }
    }

    private fun newLocalExpenseDocId(hasUser: Boolean): String {
        val prefix = if (hasUser) "expense" else "local"
        return "${prefix}_${UUID.randomUUID()}"
    }

    private suspend fun Expense.withStableCategory(): Expense {
        if (category.stableId.isNotBlank()) return this

        val localCategory = categoryDao.getCategoryById(category.id)
        val stableId = localCategory?.stableId?.takeIf { it.isNotBlank() }
            ?: CategoryStableId.legacyStableId(category.id)
        return copy(category = category.copy(stableId = stableId))
    }

    private suspend fun resolveLocalCategory(remoteCategory: Category): Category {
        val localCategory: CategoryEntity? = remoteCategory.stableId
            .takeIf { it.isNotBlank() }
            ?.let { categoryDao.getCategoryByStableId(it, currentUserId) }
            ?: remoteCategory.id
                .takeIf { it != 0L }
                ?.let { categoryDao.getCategoryById(it) }
            ?: categoryDao.getCategoryByNameAndType(
                remoteCategory.title,
                remoteCategory.type.name,
                currentUserId
            )

        return localCategory?.toDomain() ?: createLocalCategoryForRemoteExpense(remoteCategory)
    }

    private suspend fun createLocalCategoryForRemoteExpense(remoteCategory: Category): Category {
        val stableId = remoteCategory.stableId.ifBlank {
            if (remoteCategory.id != 0L) {
                CategoryStableId.legacyStableId(remoteCategory.id)
            } else {
                remoteCategoryStableId(remoteCategory)
            }
        }
        val fallbackIcon = if (remoteCategory.type == com.example.moneymate.domain.model.TransactionType.INCOME) {
            "ic_money"
        } else {
            "ic_food"
        }
        val categoryEntity = CategoryEntity(
            stableId = stableId,
            userId = currentUserId,
            title = remoteCategory.title.ifBlank { "Uncategorized" },
            iconResName = remoteCategory.iconResName.ifBlank { fallbackIcon },
            colorHex = remoteCategory.colorHex.ifBlank { "#607D8B" },
            type = remoteCategory.type.name,
            isDefault = false
        )
        val localId = categoryDao.insertCategory(CategoryStableId.ensureStableId(categoryEntity))
        return categoryEntity.copy(categoryId = localId).toDomain()
    }

    private fun remoteCategoryStableId(remoteCategory: Category): String {
        val normalizedTitle = remoteCategory.title
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "uncategorized" }
        return "remote_${remoteCategory.type.name.lowercase(Locale.US)}_$normalizedTitle"
    }

    private fun authUserIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        trySend(firebaseAuth.currentUser?.uid)
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    private suspend fun getCategoryUsage(category: CategoryEntity): CategoryUsage {
        return CategoryUsage(
            expenseCount = expenseDao.countExpensesByCategory(
                categoryId = category.categoryId,
                categoryStableId = category.stableId
            ),
            budgetCount = budgetDao.countBudgetsByCategory(
                userId = currentUserId,
                categoryId = category.categoryId,
                categoryStableId = category.stableId
            ),
            recurringTransactionCount = recurringTransactionDao.countRecurringTransactionsByCategory(
                userId = currentUserId,
                categoryId = category.categoryId,
                categoryStableId = category.stableId
            )
        )
    }

    private companion object {
        val PENDING_STATUSES = setOf(
            ExpenseSyncStatus.PENDING_CREATE,
            ExpenseSyncStatus.PENDING_UPDATE,
            ExpenseSyncStatus.PENDING_DELETE
        )
        val LEGACY_PENDING_SYNC_STATUSES = PENDING_STATUSES.map { it.name }
    }
}

private fun pendingOperationFor(expense: Expense): ExpensePendingOperation {
    if (expense.pendingOperation != ExpensePendingOperation.NONE) {
        return expense.pendingOperation
    }

    return when (expense.syncStatus) {
        ExpenseSyncStatus.PENDING_CREATE -> ExpensePendingOperation.CREATE
        ExpenseSyncStatus.PENDING_UPDATE -> ExpensePendingOperation.UPDATE
        ExpenseSyncStatus.PENDING_DELETE -> ExpensePendingOperation.DELETE
        else -> ExpensePendingOperation.NONE
    }
}
