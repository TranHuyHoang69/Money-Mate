package com.example.moneymate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE userId = :userId OR isDefault = 1 ORDER BY title ASC")
    fun getAllCategoriesForUser(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :categoryType AND (userId = :userId OR isDefault = 1) ORDER BY title ASC")
    fun getCategoriesByTypeAndUser(categoryType: String, userId: String): Flow<List<CategoryEntity>>

    // 🟢 THÊM: Hàm tìm nhanh một danh mục dựa trên Tên và Loại của một User cụ thể
    @Query("SELECT * FROM categories WHERE LOWER(title) = LOWER(:title) AND type = :type AND userId = :userId LIMIT 1")
    suspend fun getCategoryByNameAndType(title: String, type: String, userId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE stableId = :stableId AND (userId = :userId OR isDefault = 1) LIMIT 1")
    suspend fun getCategoryByStableId(stableId: String, userId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}
