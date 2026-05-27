package com.example.moneymate.data.local


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.moneymate.domain.model.TransactionType

    class Converters{
        @TypeConverter
        fun fromTransactionType(value: TransactionType) = value.name

        @TypeConverter
        fun toTransactionType(value: String) = TransactionType.valueOf(value)
    }

    @Database(
        entities = [ExpenseEntity::class, CategoryEntity::class, ReminderEntity::class],
        version = 9, // Tăng version để Room cập nhật schema mới (có firestoreDocId)
        exportSchema = false
    )
    @TypeConverters(Converters::class) // <-- BẮT BUỘC PHẢI CÓ DÒNG NÀY
    abstract class AppDatabase : RoomDatabase(){
        abstract fun expenseDao(): ExpenseDao
        abstract fun categoryDao(): CategoryDao
        abstract fun reminderDao(): ReminderDao
    }