package com.example.moneymate.di


import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneymate.data.local.AppDatabase
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.ExpenseDao
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "expense-db")
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertDefaultData(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Kiểm tra xem đã có dữ liệu chưa, nếu chưa có (count = 0) thì mới chèn
                    // Điều này giúp dữ liệu luôn hiện kể cả khi bạn nâng cấp version (Migration)
                    val cursor = db.query("SELECT COUNT(*) FROM categories")
                    if (cursor.moveToFirst()) {
                        val count = cursor.getInt(0)
                        if (count == 0) {
                            insertDefaultData(db)
                        }
                    }
                    cursor.close()
                }
            })
            .build()
    }

    // Tách hàm chèn dữ liệu ra riêng cho sạch
    private fun insertDefaultData(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO categories (title, iconResName, colorHex, type, isDefault) VALUES ('Ăn uống', 'ic_food', '#FF5733', 'SPEND', 1)")
        db.execSQL("INSERT INTO categories (title, iconResName, colorHex, type, isDefault) VALUES ('Mua sắm', 'ic_shop', '#3357FF', 'SPEND', 1)")
        db.execSQL("INSERT INTO categories (title, iconResName, colorHex, type, isDefault) VALUES ('Lương', 'ic_money', '#FFD700', 'INCOME', 1)")
        db.execSQL("INSERT INTO categories (title, iconResName, colorHex, type, isDefault) VALUES ('Di chuyển', 'ic_car', '#4CAF50', 'SPEND', 1)")
    }
    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
