package com.example.moneymate.di


import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneymate.data.local.AppDatabase
import com.example.moneymate.data.local.CategoryDao
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.data.local.ExpenseDao
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense-db"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = categoryDaoProvider.get()
                    val defaultCategories = listOf(
                        CategoryEntity(
                            title = "Ăn uống",
                            iconResName = "ic_food",
                            colorHex = "#FF5733",
                            isDefault = true
                        ),
                        CategoryEntity(
                            title = "Di chuyển",
                            iconResName = "ic_car",
                            colorHex = "#33FF57",
                            isDefault = true
                        ),
                        CategoryEntity(
                            title = "Mua sắm",
                            iconResName = "ic_shop",
                            colorHex = "#3357FF",
                            isDefault = true
                        ),
                        CategoryEntity(
                            title = "Lương",
                            iconResName = "ic_money",
                            colorHex = "#FFD700",
                            isDefault = true
                        )
                    )
                    defaultCategories.forEach { dao.insertCategory(it) }
                }
            }
        }).build()
    }

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}