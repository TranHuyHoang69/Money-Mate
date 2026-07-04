package com.example.moneymate.viewmodel

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.repository.ExpenseRepository
import com.example.moneymate.domain.usecase.ExpenseUseCases
import com.example.moneymate.domain.usecase.category.AddCategoryUseCase
import com.example.moneymate.domain.usecase.category.DeleteCategoryUseCase
import com.example.moneymate.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymate.domain.usecase.category.UpdateCategoryUseCase
import com.example.moneymate.domain.usecase.expense.AddExpenseUseCase
import com.example.moneymate.domain.usecase.expense.DeleteExpenseUseCase
import com.example.moneymate.domain.usecase.expense.GetAllExpensesUseCase
import com.example.moneymate.domain.usecase.expense.GetExpenseByFirestoreIdUseCase
import com.example.moneymate.domain.usecase.expense.GetExpenseByIdUseCase
import com.example.moneymate.domain.usecase.expense.GetExpensesByPeriodUseCase
import com.example.moneymate.domain.usecase.expense.UpdateExpenseUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveExpense_whenRepositoryReturnsSuccess_emitsSuccess() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = createViewModel(repository)

        fillValidExpenseForm(viewModel)
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Save)
        advanceUntilIdle()

        assertTrue(event.await() is AddExpenseUiEvent.SaveSuccess)
    }

    @Test
    fun saveExpense_whenRepositoryReturnsError_emitsErrorOnly() = runTest {
        val repository = FakeExpenseRepository().apply {
            insertResult = Result.Error("Save failed")
        }
        val viewModel = createViewModel(repository)

        fillValidExpenseForm(viewModel)
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Save)
        advanceUntilIdle()

        val emitted = event.await()
        assertTrue(emitted is AddExpenseUiEvent.ShowError)
        assertEquals("Save failed", (emitted as AddExpenseUiEvent.ShowError).message)
    }

    @Test
    fun saveExpense_whenRepositoryThrows_emitsErrorOnly() = runTest {
        val repository = FakeExpenseRepository().apply {
            throwOnInsert = true
        }
        val viewModel = createViewModel(repository)

        fillValidExpenseForm(viewModel)
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Save)
        advanceUntilIdle()

        val emitted = event.await()
        assertTrue(emitted is AddExpenseUiEvent.ShowError)
        assertEquals("Insert exploded", (emitted as AddExpenseUiEvent.ShowError).message)
    }

    @Test
    fun deleteExpense_whenRepositoryReturnsSuccess_emitsSuccess() = runTest {
        val repository = FakeExpenseRepository().apply {
            expenseForFirestoreId = expense(firestoreDocId = "expense-1")
        }
        val viewModel = createViewModel(repository)
        loadExpenseForDelete(viewModel)
        advanceUntilIdle()
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Delete)
        advanceUntilIdle()

        assertTrue(event.await() is AddExpenseUiEvent.SaveSuccess)
    }

    @Test
    fun deleteExpense_whenRepositoryReturnsError_emitsErrorOnly() = runTest {
        val repository = FakeExpenseRepository().apply {
            expenseForFirestoreId = expense(firestoreDocId = "expense-1")
            deleteResult = Result.Error("Delete failed")
        }
        val viewModel = createViewModel(repository)
        loadExpenseForDelete(viewModel)
        advanceUntilIdle()
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Delete)
        advanceUntilIdle()

        val emitted = event.await()
        assertTrue(emitted is AddExpenseUiEvent.ShowError)
        assertEquals("Delete failed", (emitted as AddExpenseUiEvent.ShowError).message)
    }

    @Test
    fun deleteExpense_whenRepositoryThrows_emitsErrorOnly() = runTest {
        val repository = FakeExpenseRepository().apply {
            expenseForFirestoreId = expense(firestoreDocId = "expense-1")
            throwOnDelete = true
        }
        val viewModel = createViewModel(repository)
        loadExpenseForDelete(viewModel)
        advanceUntilIdle()
        val event = async { nextEvent(viewModel) }

        viewModel.onEvent(AddExpenseEvent.Delete)
        advanceUntilIdle()

        val emitted = event.await()
        assertTrue(emitted is AddExpenseUiEvent.ShowError)
        assertEquals("Delete exploded", (emitted as AddExpenseUiEvent.ShowError).message)
    }

    private suspend fun nextEvent(viewModel: AddExpenseViewModel): AddExpenseUiEvent {
        return withTimeout(1_000) {
            viewModel.eventFlow.first()
        }
    }

    private fun createViewModel(repository: FakeExpenseRepository): AddExpenseViewModel {
        return AddExpenseViewModel(
            ExpenseUseCases(
                getAllExpenses = GetAllExpensesUseCase(repository),
                getExpensesByPeriod = GetExpensesByPeriodUseCase(repository),
                getExpenseById = GetExpenseByIdUseCase(repository),
                addExpense = AddExpenseUseCase(repository),
                updateExpense = UpdateExpenseUseCase(repository),
                deleteExpense = DeleteExpenseUseCase(repository),
                getExpenseByFirestoreId = GetExpenseByFirestoreIdUseCase(repository),
                getAllCategories = GetAllCategoriesUseCase(repository),
                addCategory = AddCategoryUseCase(repository),
                updateCategory = UpdateCategoryUseCase(repository),
                deleteCategory = DeleteCategoryUseCase(repository)
            )
        )
    }

    private fun fillValidExpenseForm(viewModel: AddExpenseViewModel) {
        viewModel.onEvent(AddExpenseEvent.ChangeAmount("1000"))
        viewModel.onEvent(AddExpenseEvent.SelectCategory(testCategory))
    }

    private fun loadExpenseForDelete(viewModel: AddExpenseViewModel) {
        viewModel.onEvent(AddExpenseEvent.LoadDetailsByFirestoreId("expense-1"))
    }

    private fun expense(
        amount: Double = 1000.0,
        firestoreDocId: String = ""
    ): Expense {
        return Expense(
            firestoreDocId = firestoreDocId,
            type = TransactionType.SPEND,
            amount = amount,
            category = testCategory,
            timestamp = 1_700_000_000_000L,
            note = "Test"
        )
    }

    private class FakeExpenseRepository : ExpenseRepository {
        var insertResult: Result<Unit> = Result.Success(Unit)
        var deleteResult: Result<Unit> = Result.Success(Unit)
        var throwOnInsert = false
        var throwOnDelete = false
        var expenseForFirestoreId: Expense? = null

        override fun getAllExpenses(): Flow<Result<List<Expense>>> = flowOf(Result.Success(emptyList()))

        override fun getExpensesByPeriod(start: Long, end: Long): Flow<Result<List<Expense>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun insertExpense(expense: Expense): Result<Unit> {
            if (throwOnInsert) throw IllegalStateException("Insert exploded")
            return insertResult
        }

        override suspend fun updateExpense(expense: Expense): Result<Unit> = Result.Success(Unit)

        override suspend fun deleteExpense(firestoreId: String): Result<Unit> {
            if (throwOnDelete) throw IllegalStateException("Delete exploded")
            return deleteResult
        }

        override suspend fun syncPendingExpenses(): Result<Unit> = Result.Success(Unit)

        override fun getExpenseById(id: Long): Flow<Result<Expense?>> = flowOf(Result.Success(null))

        override fun getExpenseByFirestoreId(firestoreId: String): Flow<Result<Expense?>> =
            flowOf(Result.Success(expenseForFirestoreId))

        override fun getAllCategories(): Flow<Result<List<Category>>> =
            flowOf(Result.Success(listOf(testCategory)))

        override fun getCategoriesByType(type: String): Flow<Result<List<Category>>> =
            flowOf(Result.Success(listOf(testCategory)))

        override suspend fun insertCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun updateCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun deleteCategory(category: Category): Result<Unit> = Result.Success(Unit)

        override suspend fun clearAllLocalData(): Result<Unit> = Result.Success(Unit)
    }

    class MainDispatcherRule(
        private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description) {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    private companion object {
        val testCategory = Category(
            id = 1,
            title = "Food",
            iconResName = "ic_food",
            colorHex = "#FF0000",
            type = TransactionType.SPEND
        )
    }
}
