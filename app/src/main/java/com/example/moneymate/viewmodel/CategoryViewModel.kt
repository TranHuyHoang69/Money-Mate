package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.BudgetDao
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.local.CategoryStableId
import com.example.moneymate.data.local.ExpenseDao
import com.example.moneymate.data.local.RecurringTransactionDao
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.CategoryDeletionPolicy
import com.example.moneymate.domain.model.CategoryUsage
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val dao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Trạng thái lưu trữ UID hiện tại
    private val currentUserId = MutableStateFlow(firebaseAuth.currentUser?.uid ?: "guest")

    private var isSyncing = false
    val selectedCategoryFromManagement = MutableStateFlow<CategoryEntity?>(null)
    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError: Flow<String?> = _categoryError

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCategories: Flow<List<CategoryEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid == "guest") {
            flowOf(emptyList())
        } else {
            // Khi UID thay đổi hợp lệ, chủ động kích hoạt đồng bộ ngầm ngay lập tức để nạp dữ liệu chuẩn
            checkAndSyncCategoriesForId(uid)
            dao.getAllCategoriesForUser(uid)
        }
    }.flowOn(Dispatchers.IO)

    init {
        // 🟢 CẢI TIẾN THAY THẾ: Sử dụng cơ chế lắng nghe sự thay đổi của FirebaseAuth theo thời gian thực
        // Bất cứ khi nào User đăng nhập thành công hoặc đổi tài khoản, luồng này sẽ tự động thức tỉnh
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAuth.authStateFlow().collect { user ->
                val uid = user?.uid
                if (uid != null) {
                    currentUserId.value = uid
                } else {
                    currentUserId.value = "guest"
                }
            }
        }
    }
    /**
     * 🟢 THÊM: Hàm được gọi khi user click chọn danh mục ở màn hình quản lý
     */
    fun selectCategoryAndBack(category: CategoryEntity) {
        selectedCategoryFromManagement.value = category
    }

    /**
     * 🟢 THÊM: Hàm xóa trạng thái sau khi AddScreen đã xử lý xong (để tránh bị lưu lại cho lần sau)
     */
    fun clearSelectedCategoryFromManagement() {
        selectedCategoryFromManagement.value = null
    }

    fun clearCategoryError() {
        _categoryError.value = null
    }

    /**
     * Extension hỗ trợ chuyển đổi trạng thái Auth thành Flow để lắng nghe an toàn
     */
    private fun FirebaseAuth.authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        addAuthStateListener(listener)
        awaitClose { removeAuthStateListener(listener) }
    }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> {
        val uid = firebaseAuth.currentUser?.uid ?: "guest"
        return dao.getCategoriesByTypeAndUser(type, uid).flowOn(Dispatchers.IO)
    }

    fun insertCategory(category: CategoryEntity) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val normalizedCategory = CategoryStableId.ensureStableId(category.copy(userId = uid))
                val existingCategory = dao.getCategoryByStableId(normalizedCategory.stableId, uid)
                    ?: dao.getCategoryByNameAndType(category.title.trim(), category.type, uid)

                val categoryToSave = if (existingCategory != null) {
                    normalizedCategory.copy(
                        categoryId = existingCategory.categoryId,
                        stableId = existingCategory.stableId.ifBlank { normalizedCategory.stableId },
                        userId = uid
                    )
                } else {
                    normalizedCategory
                }

                val rowId = dao.insertCategory(categoryToSave)
                val savedCategory = categoryToSave.copy(
                    categoryId = categoryToSave.categoryId.takeIf { it != 0L } ?: rowId
                )
                firestoreDataSource.saveCategoryToRemote(savedCategory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usage = CategoryUsage(
                    expenseCount = expenseDao.countExpensesByCategory(
                        categoryId = category.categoryId,
                        categoryStableId = category.stableId
                    ),
                    budgetCount = budgetDao.countBudgetsByCategory(
                        userId = firebaseAuth.currentUser?.uid ?: "guest",
                        categoryId = category.categoryId,
                        categoryStableId = category.stableId
                    ),
                    recurringTransactionCount = recurringTransactionDao.countRecurringTransactionsByCategory(
                        userId = firebaseAuth.currentUser?.uid ?: "guest",
                        categoryId = category.categoryId,
                        categoryStableId = category.stableId
                    )
                )
                val blockingMessage = CategoryDeletionPolicy.blockingMessage(usage)
                if (blockingMessage != null) {
                    _categoryError.value = blockingMessage
                    return@launch
                }

                dao.deleteCategory(category)
                when (val remoteResult = firestoreDataSource.deleteCategoryFromRemote(category)) {
                    is Result.Error -> _categoryError.value = remoteResult.message
                    else -> Unit
                }
            } catch (e: Exception) {
                _categoryError.value = e.message ?: "Không thể xóa danh mục"
                e.printStackTrace()
            }
        }
    }

    /**
     * Hàm trigger đồng bộ thủ công từ UI nếu cần thiết
     */
    fun checkAndSyncCategories() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            currentUserId.value = uid
            checkAndSyncCategoriesForId(uid)
        } else {
            currentUserId.value = "guest"
        }
    }

    /**
     * 🟢 TÁCH BIỆT LOGIC ĐỒNG BỘ: Chạy bất đồng bộ an toàn dựa trên UID truyền vào trực tiếp
     */
    private fun checkAndSyncCategoriesForId(uid: String) {
        if (isSyncing) return
        isSyncing = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestoreDataSource.observeUserCategories().first().let { result ->
                    if (result is Result.Success) {
                        val remoteCategories = result.data

                        if (remoteCategories.isNotEmpty()) {
                            remoteCategories.forEach { remoteCat ->
                                val normalizedRemote = CategoryStableId.ensureStableId(
                                    remoteCat.copy(userId = uid)
                                )
                                val existingCategory = dao.getCategoryByStableId(
                                    normalizedRemote.stableId,
                                    uid
                                )
                                    ?: dao.getCategoryByNameAndType(
                                        normalizedRemote.title.trim(),
                                        normalizedRemote.type,
                                        uid
                                    )

                                if (existingCategory != null) {
                                    val localCategory = normalizedRemote.copy(
                                        categoryId = existingCategory.categoryId,
                                        stableId = existingCategory.stableId.ifBlank {
                                            normalizedRemote.stableId
                                        },
                                        userId = uid
                                    )
                                    dao.updateCategory(localCategory)
                                    firestoreDataSource.saveCategoryToRemote(localCategory)
                                } else {
                                    val rowId = dao.insertCategory(
                                        normalizedRemote.copy(categoryId = 0L, userId = uid)
                                    )
                                    firestoreDataSource.saveCategoryToRemote(
                                        normalizedRemote.copy(categoryId = rowId, userId = uid)
                                    )
                                }
                            }
                        } else {
                            val localCategories = dao.getAllCategoriesForUser(uid).first()
                            val userSpecificCategories = localCategories.filter { it.userId == uid }

                            if (userSpecificCategories.isEmpty()) {
                                initDefaultCategoriesForNewUser(uid)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncing = false
            }
        }
    }

    private suspend fun initDefaultCategoriesForNewUser(uid: String) {
        val defaultSystemCategories = listOf(
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_FOOD, title = "Ăn uống", iconResName = "ic_food", colorHex = "#4CB080", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_SHOPPING, title = "Mua sắm", iconResName = "ic_shop", colorHex = "#E91E63", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_TRANSPORT, title = "Di chuyển", iconResName = "ic_car", colorHex = "#2196F3", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.INCOME_SALARY, title = "Tiền lương", iconResName = "ic_money", colorHex = "#FF9800", type = "INCOME", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_HEALTH, title = "Sức khoẻ", iconResName = "ic_cat_health_health", colorHex = "#F44336", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_ENTERTAINMENT, title = "Giải trí", iconResName = "ic_cat_finance_wallet", colorHex = "#C2185B", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_COFFEE, title = "Cafe", iconResName = "ic_cat_food_coffee", colorHex = "#4CAF50", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, stableId = CategoryStableId.SPEND_GIFT, title = "Quà tặng", iconResName = "ic_cat_shop_gift", colorHex = "#FF5722", type = "SPEND", isDefault = true )
        )

        defaultSystemCategories.forEach { entity ->
            val existingCategory = dao.getCategoryByStableId(entity.stableId, uid)
            if (existingCategory == null) {
                val rowId = dao.insertCategory(entity)
                firestoreDataSource.saveCategoryToRemote(entity.copy(categoryId = rowId))
            } else {
                firestoreDataSource.saveCategoryToRemote(existingCategory)
            }
        }
    }

    fun clearLocalOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllCategories()
            currentUserId.value = "guest"
        }
    }
}
