package com.example.moneymate.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneymate.data.local.AppDatabase
import com.example.moneymate.data.local.BudgetDao
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.ExpenseDao
import com.example.moneymate.data.local.RecurringTransactionDao
import com.example.moneymate.data.local.ReceiptLearningPatternDao
import com.example.moneymate.data.local.ReminderDao
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "expense-db")
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 🟢 CHỈ CHÈN TẠI ONCREATE: Chạy duy nhất 1 lần đầu tiên khi cài app
                    CoroutineScope(Dispatchers.IO).launch {
                        insertDefaultData(db)
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // 🟢 ĐÃ XÓA: Bỏ toàn bộ khối Coroutine check COUNT(*) ở đây để triệt tiêu lỗi lặp dữ liệu khi chuyển Screen
                }
            })
            .build()
    }

    // 🟢 ĐÃ SỬA: Bổ sung tường minh cột 'userId' giá trị 'system' để Room không bị lỗi mapping NULL
    private fun insertDefaultData(db: SupportSQLiteDatabase) {
        try {
//            db.execSQL("INSERT INTO categories (userId, title, iconResName, colorHex, type, isDefault) VALUES ('system', 'Ăn uống', 'ic_food', '#FF5733', 'SPEND', 1)")
//            db.execSQL("INSERT INTO categories (userId, title, iconResName, colorHex, type, isDefault) VALUES ('system', 'Mua sắm', 'ic_shop', '#3357FF', 'SPEND', 1)")
//            db.execSQL("INSERT INTO categories (userId, title, iconResName, colorHex, type, isDefault) VALUES ('system', 'Lương', 'ic_money', '#FFD700', 'INCOME', 1)")
//            db.execSQL("INSERT INTO categories (userId, title, iconResName, colorHex, type, isDefault) VALUES ('system', 'Di chuyển', 'ic_car', '#4CAF50', 'SPEND', 1)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    @Singleton
    fun provideRecurringTransactionDao(database: AppDatabase): RecurringTransactionDao =
        database.recurringTransactionDao()

    @Provides
    @Singleton
    fun provideReceiptLearningPatternDao(database: AppDatabase): ReceiptLearningPatternDao =
        database.receiptLearningPatternDao()
}
