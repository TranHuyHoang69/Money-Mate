package com.example.moneymate.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private var roomDatabase: AppDatabase? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        roomDatabase?.close()
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrateFromVersion1To15_preservesCategoryAndExpense() {
        createLegacyDatabase(version = 1) {
            createVersion1Schema()
            insertCategoryV1(
                categoryId = 1,
                title = "An uong",
                iconResName = "ic_food",
                colorHex = "#FF5733",
                isDefault = 1
            )
            insertExpenseV1(id = 1, categoryId = 1, note = "Lunch")
        }

        val db = openMigratedDatabase()

        db.query(
            "SELECT type, userId, stableId FROM categories WHERE categoryId = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("SPEND", cursor.getString(0))
            assertEquals("system", cursor.getString(1))
            assertEquals("spend_food", cursor.getString(2))
        }
        db.query(
            "SELECT firestoreDocId, categoryStableId, syncStatus, isDeleted FROM expenses WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy_local_1", cursor.getString(0))
            assertEquals("spend_food", cursor.getString(1))
            assertEquals("SYNCED", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
        }
    }

    @Test
    fun migrateFromVersion2To15_assignsUniqueLegacyFirestoreIds() {
        createLegacyDatabase(version = 2) {
            createVersion2Schema()
            insertCategoryV2(
                categoryId = 2,
                title = "Salary",
                iconResName = "ic_money",
                colorHex = "#FFD700",
                type = "INCOME",
                isDefault = 1
            )
            insertExpenseV1(id = 1, categoryId = 2, note = "May salary")
            insertExpenseV1(id = 2, categoryId = 2, note = "June salary")
        }

        val db = openMigratedDatabase()

        db.query(
            "SELECT COUNT(DISTINCT firestoreDocId) FROM expenses WHERE id IN (1, 2)"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        db.query(
            "SELECT stableId FROM categories WHERE categoryId = 2"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("income_salary", cursor.getString(0))
        }
    }

    @Test
    fun migrateFromVersion7To15_preservesReminderAndAddsCategoryUserId() {
        createLegacyDatabase(version = 7) {
            createVersion7Schema()
            insertCategoryV2(
                categoryId = 3,
                title = "Transport",
                iconResName = "ic_car",
                colorHex = "#4CAF50",
                type = "SPEND",
                isDefault = 1
            )
            insertExpenseV7(id = 1, firestoreDocId = "remote-expense-1", categoryId = 3)
            insertReminder(id = 1, userId = "user-1", title = "Pay bill")
        }

        val db = openMigratedDatabase()

        db.query(
            "SELECT userId, stableId FROM categories WHERE categoryId = 3"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("system", cursor.getString(0))
            assertEquals("spend_transport", cursor.getString(1))
        }
        db.query(
            "SELECT title FROM reminders WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Pay bill", cursor.getString(0))
        }
    }

    @Test
    fun migrateFromVersion9To15_preservesExistingUserCategory() {
        createLegacyDatabase(version = 9) {
            createVersion9Schema()
            insertCategoryV9(
                categoryId = 4,
                userId = "user-1",
                title = "Custom",
                iconResName = "ic_custom",
                colorHex = "#006C4C",
                type = "SPEND",
                isDefault = 0
            )
            insertExpenseV7(id = 1, firestoreDocId = "remote-expense-2", categoryId = 4)
        }

        val db = openMigratedDatabase()

        db.query(
            "SELECT userId, stableId FROM categories WHERE categoryId = 4"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("user-1", cursor.getString(0))
            assertEquals("legacy_category_4", cursor.getString(1))
        }
    }

    private fun createLegacyDatabase(version: Int, block: SQLiteDatabase.() -> Unit) {
        val dbFile = context.getDatabasePath(TEST_DB)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.block()
            db.version = version
        }
    }

    private fun openMigratedDatabase(): SupportSQLiteDatabase {
        roomDatabase = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        return roomDatabase!!.openHelper.writableDatabase
    }

    private fun SQLiteDatabase.createVersion1Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `iconResName` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createBaseExpenseTable(includeFirestoreDocId = false)
        execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
    }

    private fun SQLiteDatabase.createVersion2Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `iconResName` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createBaseExpenseTable(includeFirestoreDocId = false)
        execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
    }

    private fun SQLiteDatabase.createVersion7Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `iconResName` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createBaseExpenseTable(includeFirestoreDocId = true)
        execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_firestoreDocId` ON `expenses` (`firestoreDocId`)")
        createReminderTable()
    }

    private fun SQLiteDatabase.createVersion9Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `iconResName` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createBaseExpenseTable(includeFirestoreDocId = true)
        execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_firestoreDocId` ON `expenses` (`firestoreDocId`)")
        createReminderTable()
    }

    private fun SQLiteDatabase.createBaseExpenseTable(includeFirestoreDocId: Boolean) {
        val firestoreColumn = if (includeFirestoreDocId) "`firestoreDocId` TEXT NOT NULL," else ""
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                $firestoreColumn
                `type` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`categoryId`) ON UPDATE NO ACTION ON DELETE SET DEFAULT
            )
            """.trimIndent()
        )
    }

    private fun SQLiteDatabase.createReminderTable() {
        execSQL(
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

    private fun SQLiteDatabase.insertCategoryV1(
        categoryId: Long,
        title: String,
        iconResName: String,
        colorHex: String,
        isDefault: Int
    ) {
        execSQL(
            "INSERT INTO categories (categoryId, title, iconResName, colorHex, isDefault) VALUES (?, ?, ?, ?, ?)",
            arrayOf(categoryId, title, iconResName, colorHex, isDefault)
        )
    }

    private fun SQLiteDatabase.insertCategoryV2(
        categoryId: Long,
        title: String,
        iconResName: String,
        colorHex: String,
        type: String,
        isDefault: Int
    ) {
        execSQL(
            "INSERT INTO categories (categoryId, title, iconResName, colorHex, type, isDefault) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(categoryId, title, iconResName, colorHex, type, isDefault)
        )
    }

    private fun SQLiteDatabase.insertCategoryV9(
        categoryId: Long,
        userId: String,
        title: String,
        iconResName: String,
        colorHex: String,
        type: String,
        isDefault: Int
    ) {
        execSQL(
            "INSERT INTO categories (categoryId, userId, title, iconResName, colorHex, type, isDefault) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(categoryId, userId, title, iconResName, colorHex, type, isDefault)
        )
    }

    private fun SQLiteDatabase.insertExpenseV1(id: Long, categoryId: Long, note: String) {
        execSQL(
            "INSERT INTO expenses (id, type, amount, categoryId, timestamp, note) VALUES (?, 'SPEND', 1000.0, ?, 1717200000000, ?)",
            arrayOf(id, categoryId, note)
        )
    }

    private fun SQLiteDatabase.insertExpenseV7(
        id: Long,
        firestoreDocId: String,
        categoryId: Long
    ) {
        execSQL(
            "INSERT INTO expenses (id, firestoreDocId, type, amount, categoryId, timestamp, note) VALUES (?, ?, 'SPEND', 2000.0, ?, 1717200000000, 'Remote')",
            arrayOf(id, firestoreDocId, categoryId)
        )
    }

    private fun SQLiteDatabase.insertReminder(id: Int, userId: String, title: String) {
        execSQL(
            "INSERT INTO reminders (id, userId, title, note, reminderDateTime, repeatInterval, isActive) VALUES (?, ?, ?, '', 1717200000000, 'ONCE', 1)",
            arrayOf(id, userId, title)
        )
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
