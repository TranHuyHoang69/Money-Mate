package com.example.moneymate.data.remote

import android.util.Log
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "FirestoreDataSource"

    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    // Lắng nghe real-time expenses của user hiện tại
    fun observeUserExpenses(): Flow<Result<List<Expense>>> = callbackFlow {
        val userId = getCurrentUserId()

        // ✅ FIX: Nếu chưa login, phát empty list, không subscribe vào Firestore
        if (userId == null) {
            trySend(Result.Success(emptyList()))
            close()
            return@callbackFlow
        }

        android.util.Log.d(TAG, "Observing expenses for user: $userId")

        val listener = firestore.collection("users")
            .document(userId)  // ✅ Firestore sẽ tự filter theo userId
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to expenses", error)
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val expenses = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.toObject(FirestoreExpense::class.java)
                                data?.let {
                                    val type = try {
                                        TransactionType.valueOf(it.type)
                                    } catch (e: Exception) {
                                        TransactionType.SPEND
                                    }

                                    Expense(
                                        id = doc.id.hashCode().toLong(),
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
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping expense", e)
                                null
                            }
                        }
                        android.util.Log.d(TAG, "Loaded ${expenses.size} expenses for user: $userId")
                        trySend(Result.Success(expenses))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing snapshot", e)
                        trySend(Result.Error(e.message ?: "Unknown error"))
                    }
                }
            }

        awaitClose {
            android.util.Log.d(TAG, "Stopping listener for user: $userId")
            listener.remove()
        }
    }.catch { e ->
        Log.e(TAG, "Error observing expenses", e)
        emit(Result.Error(e.message ?: "Unknown error"))
    }

    // Thêm expense mới lên Firestore
    suspend fun addExpense(expense: Expense): Result<Unit> = try {
        val userId = getCurrentUserId()
            ?: return Result.Error("User not authenticated")

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

        firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .add(firestoreExpense)
            .await()

        Log.d(TAG, "Expense added successfully")
        Result.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error adding expense", e)
        Result.Error(e.message ?: "Error adding expense")
    }

    // Cập nhật expense
    suspend fun updateExpense(docId: String, expense: Expense): Result<Unit> = try {
        val userId = getCurrentUserId()
            ?: return Result.Error("User not authenticated")

        val updates = mapOf(
            "amount" to expense.amount,
            "categoryId" to expense.category.id,
            "categoryTitle" to expense.category.title,
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
        Log.e(TAG, "Error updating expense", e)
        Result.Error(e.message ?: "Error updating expense")
    }

    // Xóa expense
    suspend fun deleteExpense(docId: String): Result<Unit> = try {
        val userId = getCurrentUserId()
            ?: return Result.Error("User not authenticated")

        firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .document(docId)
            .delete()
            .await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting expense", e)
        Result.Error(e.message ?: "Error deleting expense")
    }
}