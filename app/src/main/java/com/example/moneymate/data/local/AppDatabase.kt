package com.example.moneymate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.moneymate.domain.model.TransactionType

class Converters {
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
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao

    // 🌟 BỔ SUNG KHỐI COMPANION OBJECT NÀY ĐỂ SỬA LỖI UNRESOLVED REFERENCE
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Nếu INSTANCE đã tồn tại thì trả về ngay, nếu chưa thì tiến hành đồng bộ hóa để tạo
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneymate_database" // Tên file database SQLite lưu trên máy
                )
                    // ⚠️ LƯU Ý HÀNH VI MIGRATION:
                    // Vì bạn vừa tăng version lên 9, nếu chưa viết cấu hình Migration cụ thể,
                    // hãy tạm dùng hàm dưới đây để Room tự động xóa sạch DB cũ và cài lại schema mới khi bị lệch version (tránh crash app).
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}