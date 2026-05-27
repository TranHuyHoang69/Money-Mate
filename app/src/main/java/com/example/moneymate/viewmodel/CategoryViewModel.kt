package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.remote.FirestoreDataSource
import com.example.moneymate.domain.Result
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
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Trạng thái lưu trữ UID hiện tại
    private val currentUserId = MutableStateFlow(firebaseAuth.currentUser?.uid ?: "guest")

    private var isSyncing = false
    val selectedCategoryFromManagement = MutableStateFlow<CategoryEntity?>(null)

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
                val existingCategory = dao.getCategoryByNameAndType(category.title.trim(), category.type, uid)

                val categoryToSave = if (existingCategory != null) {
                    category.copy(categoryId = existingCategory.categoryId, userId = uid)
                } else {
                    category.copy(userId = uid)
                }

                dao.insertCategory(categoryToSave)
                firestoreDataSource.saveCategoryToRemote(categoryToSave)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteCategory(category)
                firestoreDataSource.deleteCategoryFromRemote(category)
            } catch (e: Exception) {
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
                            val localCategories = dao.getAllCategoriesForUser(uid).first()

                            remoteCategories.forEach { remoteCat ->
                                val isAlreadyExisted = localCategories.any { localCat ->
                                    localCat.title.trim().lowercase() == remoteCat.title.trim().lowercase() &&
                                            localCat.type == remoteCat.type
                                }
                                if (!isAlreadyExisted) {
                                    dao.insertCategory(remoteCat.copy(categoryId = 0L, userId = uid))
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
            CategoryEntity(userId = uid, title = "Ăn uống", iconResName = "ic_food", colorHex = "#4CB080", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Mua sắm", iconResName = "ic_shop", colorHex = "#E91E63", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Di chuyển", iconResName = "ic_car", colorHex = "#2196F3", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Tiền lương", iconResName = "ic_money", colorHex = "#FF9800", type = "INCOME", isDefault = true),
            CategoryEntity(userId = uid, title = "Sức khoẻ", iconResName = "ic_cat_health_health", colorHex = "#F44336", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Giải trí", iconResName = "ic_cat_finance_wallet", colorHex = "#C2185B", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Cafe", iconResName = "ic_cat_food_coffee", colorHex = "#4CAF50", type = "SPEND", isDefault = true),
            CategoryEntity(userId = uid, title = "Quà tặng", iconResName = "ic_cat_shop_gift", colorHex = "#FF5722", type = "SPEND", isDefault = true )
        )

        dao.insertAll(defaultSystemCategories)

        defaultSystemCategories.forEach { entity ->
            firestoreDataSource.saveCategoryToRemote(entity)
        }
    }

    fun clearLocalOnLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllCategories()
            currentUserId.value = "guest"
        }
    }
}