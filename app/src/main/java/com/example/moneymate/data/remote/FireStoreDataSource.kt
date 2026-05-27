package com.example.moneymate.data.remote

import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "FirestoreDataSource"

    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    // ==================== QUẢN LÝ EXPENSES (CẬP NHẬT ĐỒNG BỘ ID) ====================

    fun observeUserExpenses(): Flow<Result<List<Expense>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(userId).collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val expenses = snapshot.documents.mapNotNull { doc ->
                        val data = doc.toObject(FirestoreExpense::class.java)
                        data?.let {
                            val type = try { TransactionType.valueOf(it.type) } catch (e: Exception) { TransactionType.SPEND }
                            Expense(
                                id = 0L,
                                firestoreDocId = doc.id,
                                type = type,
                                amount = it.amount,
                                timestamp = it.timestamp,
                                note = it.note,
                                category = Category(
                                    id = it.categoryId,
                                    title = it.categoryTitle,
                                    iconResName = "",
                                    colorHex = it.categoryColorHex,
                                    type = type
                                )
                            )
                        }
                    }
                    trySend(Result.Success(expenses))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addExpense(expense: Expense): Result<String> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")

        val docRef = firestore.collection("users").document(userId).collection("expenses").document()
        val generatedId = docRef.id

        val firestoreExpense = FirestoreExpense(
            userId = userId,
            type = expense.type.name,
            amount = expense.amount,
            categoryId = expense.category.id,
            categoryTitle = expense.category.title,
            categoryColorHex = expense.category.colorHex,
            timestamp = expense.timestamp,
            note = expense.note
        )

        docRef.set(firestoreExpense).await()
        Result.Success(generatedId)
    } catch (e: Exception) { Result.Error(e.message ?: "Error adding expense") }

    suspend fun updateExpense(docId: String, expense: Expense): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            val updates = mapOf(
                "amount" to expense.amount,
                "categoryId" to expense.category.id,
                "categoryTitle" to expense.category.title,
                "categoryColorHex" to expense.category.colorHex,
                "timestamp" to expense.timestamp,
                "note" to expense.note
            )
            firestore.collection("users").document(userId).collection("expenses").document(docId).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e.message ?: "Error updating expense") }
    }

    suspend fun deleteExpense(docId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            if (docId.isEmpty()) return Result.Error("Document ID empty")

            firestore.collection("users").document(userId).collection("expenses").document(docId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e.message ?: "Error deleting expense") }
    }

    // ==================== 🟢 QUẢN LÝ CATEGORIES ĐỒNG BỘ CLOUD ====================

    fun observeUserCategories(): Flow<Result<List<CategoryEntity>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        // Lắng nghe Realtime thay đổi danh mục của riêng User hiện tại trên Cloud
        val listener = firestore.collection("users").document(userId).collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        // 🟢 ĐÃ SỬA: Map chuẩn sang lớp FireStoreCategory (Chữ S viết hoa đồng bộ)
                        val data = doc.toObject(FireStoreCategory::class.java)
                        data?.let {
                            CategoryEntity(
                                categoryId = it.categoryId,
                                userId = it.userId,
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

        val remoteData = FireStoreCategory(
            categoryId = category.categoryId,
            userId = userId,
            title = category.title,
            iconResName = category.iconResName,
            colorHex = category.colorHex,
            type = category.type,
            isDefault = category.isDefault
        )

        // 🟢 SỬA TẠI ĐÂY: Thống nhất lấy 'title' làm tên document để tự động ghi đè chống lặp
        val docName = category.title

        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(docName)
            .set(remoteData)
            .await()

        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error saving category to cloud") }

    suspend fun deleteCategoryFromRemote(category: CategoryEntity): Result<Unit> = try {
        val userId = getCurrentUserId() ?: return Result.Error("User not authenticated")

        // 🟢 SỬA TẠI ĐÂY: Đồng bộ tên document theo 'title' khi xóa
        val docName = category.title

        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(docName)
            .delete()
            .await()

        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(e.message ?: "Error deleting category from cloud") }


    // ==================== QUẢN LÝ REMINDER ====================

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

        val removeData = FireStoreReminder(
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
            .set(removeData)
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
}

