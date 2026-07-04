package com.example.moneymate.data.remote

import com.example.moneymate.data.local.BudgetEntity
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.local.CategoryStableId
import com.example.moneymate.data.local.RecurringTransactionEntity
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    fun observeUserExpenses(): Flow<Result<List<Expense>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(FirestoreSyncError.safeMessage(error, "Expense listener failed")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val expenses = snapshot.documents.mapNotNull { doc ->
                        doc.toExpenseDomain()
                    }.sortedByDescending { it.timestamp }
                    trySend(Result.Success(expenses))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun addExpense(expense: Expense): Result<String> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
        val expensesRef = firestore.collection("users")
            .document(userId)
            .collection("expenses")
        val docRef = if (expense.firestoreDocId.isNotBlank()) {
            expensesRef.document(expense.firestoreDocId)
        } else {
            expensesRef.document()
        }

        val firestoreExpense = FirestoreExpense(
            id = docRef.id,
            userId = userId,
            type = expense.type.name,
            amount = expense.amount,
            categoryId = expense.category.id,
            categoryStableId = expense.category.stableId,
            categoryTitle = expense.category.title,
            categoryColorHex = expense.category.colorHex,
            timestamp = expense.timestamp,
            note = expense.note
        )

        docRef.set(firestoreExpense).await()
        Result.Success(docRef.id)
    } catch (e: Exception) {
        Result.Error(FirestoreSyncError.safeMessage(e, "Error adding expense"))
    }

    suspend fun updateExpense(docId: String, expense: Expense): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            val updates = mapOf(
                "type" to expense.type.name,
                "amount" to expense.amount,
                "categoryId" to expense.category.id,
                "categoryStableId" to expense.category.stableId,
                "categoryTitle" to expense.category.title,
                "categoryColorHex" to expense.category.colorHex,
                "timestamp" to expense.timestamp,
                "note" to expense.note
            )

            firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .document(docId)
                .update(updates)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FirestoreSyncError.safeMessage(e, "Error updating expense"))
        }
    }

    suspend fun deleteExpense(docId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .document(docId)
                .delete()
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(FirestoreSyncError.safeMessage(e, "Error deleting expense"))
        }
    }

    private fun DocumentSnapshot.toExpenseDomain(): Expense? {
        val amount = firstDouble("amount", "money", "value", "totalAmount") ?: return null
        val type = parseTransactionType(firstString("type", "transactionType", "expenseType"))
        val timestamp = firstLong("timestamp", "date", "time", "createdAt", "createdDate") ?: 0L
        val categoryTitle = firstString("categoryTitle", "categoryName", "category")
            .ifBlank { "Uncategorized" }

        return Expense(
            id = 0L,
            firestoreDocId = id,
            type = type,
            amount = amount,
            timestamp = timestamp,
            note = firstString("note", "description", "memo"),
            category = Category(
                id = firstLong("categoryId") ?: 0L,
                stableId = firstString("categoryStableId", "stableCategoryId"),
                title = categoryTitle,
                iconResName = firstString("categoryIconResName", "iconResName"),
                colorHex = firstString("categoryColorHex", "categoryColor").ifBlank { "#607D8B" },
                type = type
            )
        )
    }

    private fun DocumentSnapshot.firstString(vararg fields: String): String {
        return fields.firstNotNullOfOrNull { field ->
            getString(field)?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun DocumentSnapshot.firstDouble(vararg fields: String): Double? {
        return fields.firstNotNullOfOrNull { field ->
            when (val value = get(field)) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }
    }

    private fun DocumentSnapshot.firstLong(vararg fields: String): Long? {
        return fields.firstNotNullOfOrNull { field ->
            when (val value = get(field)) {
                is Number -> value.toLong()
                is Timestamp -> value.toDate().time
                is String -> value.toLongOrNull()
                else -> null
            }
        }
    }

    private fun parseTransactionType(rawType: String): TransactionType {
        val normalized = rawType.trim().uppercase(Locale.US)
        return when (normalized) {
            "INCOME", "THU", "THU_NHAP", "THU NHAP" -> TransactionType.INCOME
            "SPEND", "EXPENSE", "CHI", "CHI_PHI", "CHI PHI" -> TransactionType.SPEND
            else -> TransactionType.SPEND
        }
    }

    fun observeUserCategories(): Flow<Result<List<CategoryEntity>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        val data = doc.toObject(FireStoreCategory::class.java)
                        data?.let {
                            val stableId = it.stableId.ifBlank {
                                CategoryStableId.defaultStableId(it.type, it.iconResName)
                                    ?: "legacy_remote_${doc.id.hashCode().toLong().and(0xffffffffL).toString(16)}"
                            }
                            CategoryEntity(
                                categoryId = it.categoryId,
                                stableId = stableId,
                                userId = it.userId.ifBlank { userId },
                                title = it.title,
                                iconResName = it.iconResName,
                                colorHex = it.colorHex,
                                type = it.type,
                                isDefault = it.isDefault
                            )
                        }
                    }
                    trySend(Result.Success(categories))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveCategoryToRemote(category: CategoryEntity): Result<Unit> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
        val categoryWithStableId = CategoryStableId.ensureStableId(category)
        val remoteData = FireStoreCategory(
            categoryId = categoryWithStableId.categoryId,
            stableId = categoryWithStableId.stableId,
            userId = userId,
            title = categoryWithStableId.title,
            iconResName = categoryWithStableId.iconResName,
            colorHex = categoryWithStableId.colorHex,
            type = categoryWithStableId.type,
            isDefault = categoryWithStableId.isDefault
        )

        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryWithStableId.stableId)
            .set(remoteData)
            .await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error saving category to cloud")
    }

    suspend fun deleteCategoryFromRemote(category: CategoryEntity): Result<Unit> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")

        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(category.stableId.ifBlank { category.title })
            .delete()
            .await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error deleting category from cloud")
    }

    fun observeUserReminders(): Flow<Result<List<ReminderEntity>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reminders = snapshot.documents.mapNotNull { doc ->
                        val data = doc.toObject(FireStoreReminder::class.java)
                        data?.let {
                            ReminderEntity(
                                id = it.id,
                                userId = it.userId,
                                title = it.title,
                                note = it.note,
                                reminderDateTime = it.reminderDateTime,
                                repeatInterval = it.repeatInterval,
                                isActive = it.isActive
                            )
                        }
                    }
                    trySend(Result.Success(reminders))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveReminderToRemote(reminder: ReminderEntity): Result<Unit> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
        val remoteData = FireStoreReminder(
            id = reminder.id,
            userId = userId,
            title = reminder.title,
            note = reminder.note,
            reminderDateTime = reminder.reminderDateTime,
            repeatInterval = reminder.repeatInterval,
            isActive = reminder.isActive
        )

        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id.toString())
            .set(remoteData)
            .await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error saving reminder")
    }

    suspend fun deleteReminderFromRemote(reminderId: Int): Result<Unit> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")

        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminderId.toString())
            .delete()
            .await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error deleting reminder")
    }

    fun observeUserBudgets(): Flow<Result<List<BudgetEntity>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val budgets = snapshot.documents.mapNotNull { doc ->
                        val data = doc.toObject(FirestoreBudget::class.java)
                        data?.let {
                            BudgetEntity(
                                firestoreDocId = doc.id,
                                userId = userId,
                                categoryId = it.categoryId,
                                categoryStableId = it.categoryStableId,
                                categoryTitle = it.categoryTitle,
                                categoryColorHex = it.categoryColorHex,
                                amount = it.amount,
                                month = it.month,
                                year = it.year
                            )
                        }
                    }
                    trySend(Result.Success(budgets))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveBudgetToRemote(budget: BudgetEntity): Result<String> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
        val docId = budget.firestoreDocId.ifEmpty {
            "${budget.year}_${budget.month}_${budget.categoryStableId.ifBlank { budget.categoryId.toString() }}"
        }
        val remoteData = FirestoreBudget(
            userId = userId,
            categoryId = budget.categoryId,
            categoryStableId = budget.categoryStableId,
            categoryTitle = budget.categoryTitle,
            categoryColorHex = budget.categoryColorHex,
            amount = budget.amount,
            month = budget.month,
            year = budget.year
        )

        firestore.collection("users")
            .document(userId)
            .collection("budgets")
            .document(docId)
            .set(remoteData)
            .await()

        Result.Success(docId)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error saving budget")
    }

    suspend fun deleteBudgetFromRemote(docId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            firestore.collection("users")
                .document(userId)
                .collection("budgets")
                .document(docId)
                .delete()
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error deleting budget")
        }
    }

    fun observeUserRecurringTransactions(): Flow<Result<List<RecurringTransactionEntity>>> =
        callbackFlow {
            val userId = getCurrentUserId()
            if (userId == null) {
                trySend(Result.Success(emptyList()))
                close()
                return@callbackFlow
            }

            val listener = firestore.collection("users")
                .document(userId)
                .collection("recurringTransactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error.message ?: "Unknown error"))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val transactions = snapshot.documents.mapNotNull { doc ->
                            val data = doc.toObject(FirestoreRecurringTransaction::class.java)
                            data?.let {
                                RecurringTransactionEntity(
                                    firestoreDocId = doc.id,
                                    userId = userId,
                                    type = it.type,
                                    amount = it.amount,
                                    categoryId = it.categoryId,
                                    categoryStableId = it.categoryStableId,
                                    categoryTitle = it.categoryTitle,
                                    categoryColorHex = it.categoryColorHex,
                                    note = it.note,
                                    repeatInterval = it.repeatInterval,
                                    startDate = it.startDate,
                                    nextRunAt = it.nextRunAt,
                                    lastGeneratedAt = it.lastGeneratedAt,
                                    isActive = it.isActive
                                )
                            }
                        }
                        trySend(Result.Success(transactions))
                    }
                }

            awaitClose { listener.remove() }
        }

    suspend fun saveRecurringTransactionToRemote(
        transaction: RecurringTransactionEntity
    ): Result<String> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
        val docId = transaction.firestoreDocId.ifEmpty {
            firestore.collection("users")
                .document(userId)
                .collection("recurringTransactions")
                .document()
                .id
        }
        val remoteData = FirestoreRecurringTransaction(
            userId = userId,
            type = transaction.type,
            amount = transaction.amount,
            categoryId = transaction.categoryId,
            categoryStableId = transaction.categoryStableId,
            categoryTitle = transaction.categoryTitle,
            categoryColorHex = transaction.categoryColorHex,
            note = transaction.note,
            repeatInterval = transaction.repeatInterval,
            startDate = transaction.startDate,
            nextRunAt = transaction.nextRunAt,
            lastGeneratedAt = transaction.lastGeneratedAt,
            isActive = transaction.isActive
        )

        firestore.collection("users")
            .document(userId)
            .collection("recurringTransactions")
            .document(docId)
            .set(remoteData)
            .await()

        Result.Success(docId)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error saving recurring transaction")
    }

    suspend fun deleteRecurringTransactionFromRemote(docId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            firestore.collection("users")
                .document(userId)
                .collection("recurringTransactions")
                .document(docId)
                .delete()
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error deleting recurring transaction")
        }
    }

    suspend fun deleteUserData(userId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val collections = listOf(
                "expenses",
                "categories",
                "reminders",
                "budgets",
                "recurringTransactions",
                "security",
                "settings"
            )

            collections.forEach { collection ->
                val snapshot = userRef.collection(collection).get().await()
                snapshot.documents.chunked(MAX_BATCH_DELETE_SIZE).forEach { documents ->
                    val batch = firestore.batch()
                    documents.forEach { document ->
                        batch.delete(document.reference)
                    }
                    batch.commit().await()
                }
            }

            userRef.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error deleting user data")
        }
    }

    private companion object {
        const val MAX_BATCH_DELETE_SIZE = 450
    }
}
