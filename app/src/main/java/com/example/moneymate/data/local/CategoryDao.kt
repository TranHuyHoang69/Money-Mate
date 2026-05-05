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
    @Query("SELECT * FROM categories ORDER BY title ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>> // Sửa tên thành số nhiều cho chuẩn

    @Query("SELECT * FROM categories WHERE type = :categoryType")
    fun getCategoriesByType(categoryType: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}