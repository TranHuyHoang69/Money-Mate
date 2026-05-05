package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")

data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Long = 0,
    val title: String,
    val iconResName: String, // Lưu tên String của icon (vd: "ic_food") để dễ quản lý
    val colorHex: String,    // Lưu mã màu dưới dạng Hex (vd: "#FF5733")
    val type: String,
    val isDefault: Boolean = false // Để phân biệt danh mục hệ thống và danh mục user tự tạo
)