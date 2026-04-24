package com.example.moneymate.domain.usecase.expense

import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Expense
import com.example.moneymate.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
){
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        if(expense.amount <= 0){
            return Result.Error("Số tiền không hợp lệ")
        }
        return expenseRepository.insertExpense(expense)

    }
}