    package com.example.moneymate.data.local


    import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import com.example.moneymate.domain.model.TransactionType

    class Converters{
        @TypeConverter
        fun fromTransactionType(value: TransactionType) = value.name

        @TypeConverter
        fun toTransactionType(value: String) = TransactionType.valueOf(value)
    }

    @Database(
        entities = [ExpenseEntity::class, CategoryEntity::class],
        version = 2,
        exportSchema = false
    )
    abstract class AppDatabase : RoomDatabase(){
        abstract fun expenseDao(): ExpenseDao
        abstract fun categoryDao(): CategoryDao
    }