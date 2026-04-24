package com.example.moneymate.data.local


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
}