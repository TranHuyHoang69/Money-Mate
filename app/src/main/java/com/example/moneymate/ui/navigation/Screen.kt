package com.example.moneymate.ui.navigation

sealed class Screen(val route: String){
    object MainScreen: Screen("main-screen")
    object Login : Screen("login")
    object Register: Screen("register")
    object Add: Screen("add")
    object History : Screen("history_all")

    // 🟢 ĐỔI THÀNH STRING ID: Nhận firestoreDocId thay thế cho expenseId kiểu Long cũ
    object DetailExpense : Screen("detail_expense/{firestoreDocId}")

    object Update: Screen("update/{expenseId}")

    // 🟢 Đồng bộ lại định tuyến danh mục theo ID và Type an toàn
    object GroupedExpense : Screen("grouped_expense/{categoryId}/{type}")

    object AddCategory: Screen("add_category/{type}")
    object CategoryManagement: Screen("category_management/{type}")
    object Reminder: Screen("reminder")
    object ReminderAdd : Screen("reminder_add")
    object Profile: Screen("profile")

}