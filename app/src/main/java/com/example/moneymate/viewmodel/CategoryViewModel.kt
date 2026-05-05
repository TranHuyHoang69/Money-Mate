package com.example.moneymate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val dao: CategoryDao
): ViewModel(){

    fun getCategoriesByType(type: String) = dao.getCategoriesByType(type)

    fun insertCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) { // Chạy trên nền (IO Thread)
            try {
                dao.insertCategory(category)
            } catch (e: Exception) {
                // Bạn có thể log lỗi ở đây để debug
                e.printStackTrace()
            }
        }
    }
}