package com.example.moneymate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneymate.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType) = value.name

    @TypeConverter
    fun toTransactionType(value: String) = TransactionType.valueOf(value)
}

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        ReceiptLearningPatternEntity::class
    ],
    version = 16,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun receiptLearningPatternDao(): ReceiptLearningPatternDao

    companion object {
        const val DATABASE_NAME = "expense-db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `categories` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'SPEND'"
                )
            }
        }

        val MIGRATION_2_7 = object : Migration(2, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `firestoreDocId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE `expenses` SET `firestoreDocId` = 'legacy_local_' || `id` WHERE `firestoreDocId` = ''"
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_expenses_firestoreDocId`
                    ON `expenses` (`firestoreDocId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `reminderDateTime` INTEGER NOT NULL,
                        `repeatInterval` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_9 = object : Migration(7, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `categories` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'system'"
                )
                db.execSQL(
                    "UPDATE `expenses` SET `firestoreDocId` = 'legacy_local_' || `id` WHERE `firestoreDocId` = ''"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `firestoreDocId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `categoryTitle` TEXT NOT NULL,
                        `categoryColorHex` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_budgets_userId_categoryId_month_year`
                    ON `budgets` (`userId`, `categoryId`, `month`, `year`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `firestoreDocId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `categoryTitle` TEXT NOT NULL,
                        `categoryColorHex` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `repeatInterval` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `nextRunAt` INTEGER NOT NULL,
                        `lastGeneratedAt` INTEGER,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_recurring_transactions_firestoreDocId`
                    ON `recurring_transactions` (`firestoreDocId`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_expenses_categoryId`
                    ON `expenses` (`categoryId`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `receipt_learning_patterns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchantName` TEXT NOT NULL,
                        `textFingerprint` TEXT NOT NULL,
                        `selectedAmount` REAL NOT NULL,
                        `selectedCategoryTitle` TEXT NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_receipt_learning_patterns_merchantName_textFingerprint`
                    ON `receipt_learning_patterns` (`merchantName`, `textFingerprint`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `localUpdatedAt` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `lastSyncError` TEXT"
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `categories` ADD COLUMN `stableId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_food' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_food'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_shopping' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_shop'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_transport' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_car'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'income_salary' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'INCOME' AND `iconResName` = 'ic_money'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_health' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_cat_health_health'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_entertainment' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_cat_finance_wallet'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_coffee' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_cat_food_coffee'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'spend_gift' WHERE `stableId` = '' AND `isDefault` = 1 AND `type` = 'SPEND' AND `iconResName` = 'ic_cat_shop_gift'"
                )
                db.execSQL(
                    "UPDATE `categories` SET `stableId` = 'legacy_category_' || `categoryId` WHERE `stableId` = ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_categories_userId_stableId` ON `categories` (`userId`, `stableId`)"
                )

                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `categoryStableId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    UPDATE `expenses`
                    SET `categoryStableId` = (
                        SELECT `stableId`
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `expenses`.`categoryId`
                        LIMIT 1
                    )
                    WHERE EXISTS (
                        SELECT 1
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `expenses`.`categoryId`
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "UPDATE `expenses` SET `categoryStableId` = 'legacy_category_' || `categoryId` WHERE `categoryStableId` = ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_categoryStableId` ON `expenses` (`categoryStableId`)"
                )

                db.execSQL(
                    "ALTER TABLE `budgets` ADD COLUMN `categoryStableId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    UPDATE `budgets`
                    SET `categoryStableId` = (
                        SELECT `stableId`
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `budgets`.`categoryId`
                        LIMIT 1
                    )
                    WHERE EXISTS (
                        SELECT 1
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `budgets`.`categoryId`
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "UPDATE `budgets` SET `categoryStableId` = 'legacy_category_' || `categoryId` WHERE `categoryStableId` = ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_budgets_userId_categoryStableId_month_year` ON `budgets` (`userId`, `categoryStableId`, `month`, `year`)"
                )

                db.execSQL(
                    "ALTER TABLE `recurring_transactions` ADD COLUMN `categoryStableId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    UPDATE `recurring_transactions`
                    SET `categoryStableId` = (
                        SELECT `stableId`
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `recurring_transactions`.`categoryId`
                        LIMIT 1
                    )
                    WHERE EXISTS (
                        SELECT 1
                        FROM `categories`
                        WHERE `categories`.`categoryId` = `recurring_transactions`.`categoryId`
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "UPDATE `recurring_transactions` SET `categoryStableId` = 'legacy_category_' || `categoryId` WHERE `categoryStableId` = ''"
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `pendingOperation` TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `remoteUpdatedAt` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `lastSyncAttemptAt` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `retryCount` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE `expenses` SET `pendingOperation` = 'CREATE' WHERE `syncStatus` = 'PENDING_CREATE'"
                )
                db.execSQL(
                    "UPDATE `expenses` SET `pendingOperation` = 'UPDATE' WHERE `syncStatus` = 'PENDING_UPDATE'"
                )
                db.execSQL(
                    "UPDATE `expenses` SET `pendingOperation` = 'DELETE' WHERE `syncStatus` = 'PENDING_DELETE'"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_pendingOperation` ON `expenses` (`pendingOperation`)"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_7,
            MIGRATION_7_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
