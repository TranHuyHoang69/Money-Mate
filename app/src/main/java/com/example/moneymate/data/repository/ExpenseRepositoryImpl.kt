
package com.example.moneymate.data.repository

import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.ExpenseDao
import com.example.moneymate.data.local.toDomain
import com.example.moneymate.data.local.toEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<Result<List<Expense>>> {
        val userId = firebaseAuth.currentUser?.uid
        android.util.Log.d("ExpenseRepository", "getAllExpenses called, userId: $userId")

        return if (userId != null) {
            // User đã login → dùng Firestore
            firestoreDataSource.observeUserExpenses()
        } else {
            // User logout → trả về empty (không dùng Room local data)
            kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))
        }
    }

    override fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>> {
        val userId = firebaseAuth.currentUser?.uid
        android.util.Log.d("ExpenseRepository", "getExpensesByPeriod called, userId: $userId")

        return if (userId != null) {
            // User login → Firestore
            firestoreDataSource.observeUserExpenses().map { result ->
                when (result) {
                    is Result.Success -> {
                        // Filter theo thời gian
                        val filtered = result.data.filter { it.timestamp in start..end }
                        Result.Success(filtered) as Result<List<Expense>>
                    }
                    else -> result
                }
            }
        } else {
            // User logout → trả empty
            kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))
        }
    }

    override fun getExpenseById(id: Long): Flow<Result<Expense?>> =
        expenseDao.getExpenseWithCategoryById(id)
            .map { entity ->
                Result.Success(entity?.toDomain()) as Result<Expense?>
            }
            .flowOn(Dispatchers.IO)
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override fun getExpenseByFirestoreId(firestoreId: String): Flow<Result<Expense?>> {
        android.util.Log.d("ExpenseRepository", "getExpenseByFirestoreId từ Firestore với id: $firestoreId")

        // Lấy luồng danh sách từ Firestore, sau đó tìm bản ghi có firestoreDocId trùng khớp
        return firestoreDataSource.observeUserExpenses().map { result ->
            when (result) {
                is Result.Success -> {
                    val singleExpense = result.data.find { it.firestoreDocId == firestoreId }
                    Result.Success(singleExpense) as Result<Expense?>
                }
                is Result.Loading -> Result.Loading
                is Result.Error -> Result.Error(result.message)
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllCategories(): Flow<Result<List<Category>>> =
        categoryDao.getAllCategories().map { list ->
            val domainList = list.map { it.toDomain() }
            Result.Success(domainList) as Result<List<Category>>
        }
            .flowOn(Dispatchers.IO)
            .onStart { emit(Result.Loading) }
            .catch { emit(Result.Error(it.message ?: "Error")) }

    override fun getCategoriesByType(type: String): Flow<Result<List<Category>>> =
        categoryDao.getCategoriesByType(type).map { list ->
            Result.Success(list.map { it.toDomain() }) as Result<List<Category>>
        }.flowOn(Dispatchers.IO).onStart { emit(Result.Loading) }.catch { emit(Result.Error(it.message ?: "Error")) }

    override suspend fun insertExpense(expense: Expense): Result<Unit> = try {
        // 1. Nếu user đã đăng nhập, đẩy lên Firestore trước
        if (firebaseAuth.currentUser != null) {
            firestoreDataSource.addExpense(expense)
        }
        // 2. Lưu local backup phòng trường hợp mất mạng
        expenseDao.insertExpense(expense.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("ExpenseRepository", "Lỗi thêm giao dịch: ${e.message}")
        Result.Error(e.message ?: "Error")
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> = try {
        expenseDao.updateExpense(expense.toEntity())

        if (firebaseAuth.currentUser != null) {
            // ✅ ĐÃ SỬA: Truyền thêm chuỗi ID vào tham số đầu tiên theo đúng yêu cầu của DataSource
            firestoreDataSource.updateExpense(
                docId = expense.firestoreDocId,
                expense = expense
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun deleteExpense(firestoreId: String): Result<Unit> {
        return try {
            if (firestoreId.isEmpty()) return Result.Error("ID trống, không thể xóa")

            // 1. Xóa trực tiếp tài liệu trên Firestore đám mây
            if (firebaseAuth.currentUser != null) {
                firestoreDataSource.deleteExpense(firestoreId)
            }

            // 2. Xóa tài liệu local sao lưu trong Room DB (Dựa theo cột cứu hộ firestoreDocId trong Room nếu có)
            // Nếu Room của bạn không có cột này, bạn có thể tạm thời chỉ xóa trên Cloud hoặc tạo Query xóa theo chuỗi ID trong Dao.
            expenseDao.deleteExpenseByFirestoreId(firestoreId)

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Lỗi xóa giao dịch: ${e.message}")
            Result.Error(e.message ?: "Error")
        }
    }

    override suspend fun insertCategory(category: Category) = try {
        categoryDao.insertCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun updateCategory(category: Category) = try {
        categoryDao.updateCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun deleteCategory(category: Category) = try {
        categoryDao.deleteCategory(category.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error")
    }

    override suspend fun clearAllLocalData() = try {
        expenseDao.clearAllExpenses()
        expenseDao.clearAllCategories()
        android.util.Log.d("ExpenseRepository", "Local data cleared")
        Result.Success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("ExpenseRepository", "Error clearing local data", e)
        Result.Error(e.message ?: "Error")
    }
}