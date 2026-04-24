package com.example.moneymate.ui.navigation

sealed class Screen(val route: String){
    object MainScreen: Screen("main-screen")
    object Login : Screen("login")
    object Register: Screen("register")
    object Add: Screen("add")
    object DetailList : Screen("history-all")
    object DetailExpense : Screen("detail_expense/{expenseId}")
    object Update: Screen("update/{expenseId}")
    object GroupedExpense : Screen("grouped_expense/{categoryName}/{totalAmount}")
}