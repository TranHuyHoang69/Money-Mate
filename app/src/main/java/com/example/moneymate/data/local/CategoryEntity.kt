package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["userId", "stableId"])
    ]
)

data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Long = 0,
    val stableId: String = "",
    val userId: String,
    val title: String,
    val iconResName: String, // Lưu tên String của icon (vd: "ic_food") để dễ quản lý
    val colorHex: String,    // Lưu mã màu dưới dạng Hex (vd: "#FF5733")
    val type: String,
    val isDefault: Boolean = false // Để phân biệt danh mục hệ thống và danh mục user tự tạo
)
