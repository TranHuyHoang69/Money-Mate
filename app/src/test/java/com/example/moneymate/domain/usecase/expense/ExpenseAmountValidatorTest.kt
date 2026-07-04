package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseAmountValidatorTest {

    @Test
    fun parseValidAmount_rejectsInvalidAmounts() {
        val invalidAmounts = listOf(".", "0", "0.0", "-1000", "", "   ", "abc", "1.2.3")

        invalidAmounts.forEach { amount ->
            assertNull("Expected invalid amount: $amount", ExpenseAmountValidator.parseValidAmount(amount))
            assertEquals(ExpenseAmountValidator.ERROR_MESSAGE, ExpenseAmountValidator.errorOrNull(amount))
        }
    }

    @Test
    fun parseValidAmount_acceptsPositiveAmounts() {
        assertEquals(1.0, ExpenseAmountValidator.parseValidAmount("1") ?: 0.0, 0.0)
        assertEquals(1000.0, ExpenseAmountValidator.parseValidAmount("1000") ?: 0.0, 0.0)
        assertEquals(100000.0, ExpenseAmountValidator.parseValidAmount("100000") ?: 0.0, 0.0)
        assertEquals(1000.5, ExpenseAmountValidator.parseValidAmount("1000.5") ?: 0.0, 0.0)
    }

    @Test
    fun addExpenseUseCase_doesNotCallRepositoryForInvalidAmount() = runBlocking {
        val repository = FakeExpenseRepository()
        val result = AddExpenseUseCase(repository)(expense(amount = 0.0))

        assertTrue(result is Result.Error)
        assertFalse(repository.insertCalled)
    }

    @Test
    fun addExpenseUseCase_callsRepositoryForValidAmount() = runBlocking {
        val repository = FakeExpenseRepository()
        val result = AddExpenseUseCase(repository)(expense(amount = 1000.0))

        assertTrue(result is Result.Success)
        assertTrue(repository.insertCalled)
        assertEquals(1000.0, repository.insertedExpense?.amount ?: 0.0, 0.0)
    }

    @Test
    fun updateExpenseUseCase_doesNotCallRepositoryForInvalidAmount() = runBlocking {
        val repository = FakeExpenseRepository()
        val result = UpdateExpenseUseCase(repository)(expense(amount = -1000.0))

        assertTrue(result is Result.Error)
        assertFalse(repository.updateCalled)
    }

    private fun expense(amount: Double): Expense {
        return Expense(
            firestoreDocId = "expense-1",
            type = TransactionType.SPEND,
            amount = amount,
            category = Category(
                id = 1,
                title = "Food",
                iconResName = "ic_food",
                colorHex = "#FF0000",
                type = TransactionType.SPEND
            ),
            timestamp = 1_700_000_000_000L,
            note = "Test"
        )
    }

    private class FakeExpenseRepository : ExpenseRepository {
        var insertCalled = false
            private set
        var updateCalled = false
            private set
        var insertedExpense: Expense? = null
            private set

        override fun getAllExpenses(): Flow<Result<List<Expense>>> = flowOf(Result.Success(emptyList()))

        override fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun insertExpense(expense: Expense): Result<Unit> {
            insertCalled = true
            insertedExpense = expense
            return Result.Success(Unit)
        }

        override suspend fun updateExpense(expense: Expense): Result<Unit> {
            updateCalled = true
            return Result.Success(Unit)
        }

        override suspend fun deleteExpense(firestoreId: String): Result<Unit> = Result.Success(Unit)

        override suspend fun syncPendingExpenses(): Result<Unit> = Result.Success(Unit)

        override fun getExpenseById(id: Long): Flow<Result<Expense?>> = flowOf(Result.Success(null))

        override fun getExpenseByFirestoreId(firestoreId: String): Flow<Result<Expense?>> =
            flowOf(Result.Success(null))

        override fun getAllCategories(): Flow<Result<List<Category>>> = flowOf(Result.Success(emptyList()))

        override fun getCategoriesByType(type: String): Flow<Result<List<Category>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun insertCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun updateCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun deleteCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun clearAllLocalData(): Result<Unit> = Result.Success(Unit)
    }
}
