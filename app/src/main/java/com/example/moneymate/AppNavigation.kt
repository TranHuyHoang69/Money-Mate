package com.example.moneymate.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymate.ui.component.AppDrawer
import com.example.moneymate.ui.screen.AddCategoryScreen
import com.example.moneymate.ui.screen.AddReminderScreen
import com.example.moneymate.ui.screen.AddScreen
import com.example.moneymate.ui.screen.CategoryManagementScreen
import com.example.moneymate.ui.screen.DetailExpenseScreen
import com.example.moneymate.ui.screen.DetailListScreen
import com.example.moneymate.ui.screen.GroupedExpenseScreen
import com.example.moneymate.ui.screen.HomeScreen
import com.example.moneymate.ui.screen.LoginScreen
import com.example.moneymate.ui.screen.RegisterScreen
import com.example.moneymate.ui.screen.ReminderListScreen
import com.example.moneymate.ui.screen.UpdateReminderScreen
import com.example.moneymate.ui.screen.UpdateScreen
import com.example.moneymate.viewmodel.AuthViewModel
import com.example.moneymate.viewmodel.CategoryViewModel
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel
import com.example.moneymate.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    // ✅ ĐA SỬA: Biến HomeViewModel thành Shared ViewModel gắn liền với vòng đời của Activity hiện tại
    val sharedHomeViewModel: HomeViewModel = hiltViewModel(
        viewModelStoreOwner = context as ViewModelStoreOwner
    )

    // ✅ ĐA SỬA: Lắng nghe luồng StateFlow totalBalance động từ ViewModel dùng chung
    val totalBalance by sharedHomeViewModel.totalBalance.collectAsState()

    AppDrawer(
        drawerState = drawerState,
        scope = scope,
        authUiState = authUiState,
        totalBalance = totalBalance, // ✅ Truyền biến State động vào đây
        themeColor = Color(0xFF4CB080),
        currentRoute = currentRoute,
        onLogout = { authViewModel.logout(context) },
        onNavigate = { route ->
            if (route != currentRoute) {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.MainScreen.route
        ) {
            // --- LOGIN & REGISTER ---
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }

            composable(Screen.Register.route) {
                RegisterScreen(navController = navController)
            }

            // --- MAIN SCREEN (TRANG CHỦ) ---
            composable(Screen.MainScreen.route) {
                val historyViewModel: HistoryViewModel = hiltViewModel()

                HomeScreen(
                    navController = navController,
                    historyViewModel = historyViewModel,
                    homeViewModel = sharedHomeViewModel, // ✅ ĐA SỬA: Dùng chung thực thể với AppDrawer
                    authViewModel = authViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // --- ADD EXPENSE ---
            composable(
                route = "add?expenseId={expenseId}",
                arguments = listOf(
                    navArgument("expenseId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                if (authUiState.isLoggedIn) {
                    AddScreen(navController = navController)
                } else {
                    LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                }
            }

            // --- DETAIL LIST MẶC ĐỊNH (TỪ DRAWER) ---
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                DetailListScreen(
                    navController = navController,
                    historyViewModel = historyViewModel,
                    categoryId = 0L,
                    categoryName = "Lịch sử",
                    type = "CHI PHÍ",
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // --- XEM CỤM GIAO DỊCH THEO DANH MỤC ---
            composable(
                route = "grouped_expense/{categoryName}/{totalAmount}/{calendarMode}/{startDate}/{endDate}",
                arguments = listOf(
                    navArgument("categoryName") { type = NavType.StringType },
                    navArgument("totalAmount") { type = NavType.FloatType },
                    navArgument("calendarMode") { type = NavType.StringType },
                    navArgument("startDate") { type = NavType.LongType },
                    navArgument("endDate") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName").orEmpty()
                val totalAmount = backStackEntry.arguments?.getFloat("totalAmount")?.toDouble() ?: 0.0

                val calendarModeStr = backStackEntry.arguments?.getString("calendarMode").orEmpty()
                val startDate = backStackEntry.arguments?.getLong("startDate") ?: 0L
                val endDate = backStackEntry.arguments?.getLong("endDate") ?: 0L

                val historyViewModel: HistoryViewModel = hiltViewModel()

                LaunchedEffect(calendarModeStr, startDate, endDate) {
                    try {
                        val modeEnum = com.example.moneymate.viewmodel.CalendarMode.valueOf(calendarModeStr)
                        historyViewModel.changeMode(modeEnum)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Lỗi ép kiểu CalendarMode: ${e.message}")
                    }

                    if (startDate > 0L && endDate > 0L) {
                        historyViewModel.loadCustomRange(startDate, endDate)
                    }
                }

                GroupedExpenseScreen(
                    navController = navController,
                    historyViewModel = historyViewModel,
                    categoryName = categoryName,
                    totalAmount = totalAmount
                )
            }

            // --- DETAIL ONE EXPENSE ---
            composable(
                route = Screen.DetailExpense.route,
                arguments = listOf(
                    navArgument("firestoreDocId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("firestoreDocId").orEmpty()

                DetailExpenseScreen(
                    navController = navController,
                    firestoreDocId = docId
                )
            }

            // --- UPDATE EXPENSE ---
            composable(
                route = "update_screen/{firestoreDocId}",
                arguments = listOf(
                    navArgument("firestoreDocId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val firestoreDocId = backStackEntry.arguments?.getString("firestoreDocId") ?: ""
                UpdateScreen(navController = navController, firestoreDocId = firestoreDocId)
            }

            // --- ADD CATEGORY ---
            composable(
                route = "add_category/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "SPEND"
                val categoryViewModel: CategoryViewModel = hiltViewModel()

                AddCategoryScreen(
                    initialType = type,
                    onBack = { navController.popBackStack() },
                    onSave = { entity ->
                        categoryViewModel.insertCategory(entity)
                        navController.popBackStack()
                    }
                )
            }

            // --- CATEGORY MANAGEMENT ---
            composable(
                route = "category_management/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "SPEND"
                CategoryManagementScreen(
                    navController = navController,
                    type = type
                )
            }

            // --- MÀN HÌNH DANH SÁCH NHẮC NHỞ ---
            composable(Screen.Reminder.route) {
                if (authUiState.isLoggedIn) {
                    val reminderViewModel: ReminderViewModel = hiltViewModel()
                    LaunchedEffect(Unit) { reminderViewModel.refreshUser() }

                    ReminderListScreen(
                        viewModel = reminderViewModel,
                        navController = navController,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onAddReminderClick = {
                            navController.navigate(Screen.ReminderAdd.route)
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.MainScreen.route) { saveState = true }
                        }
                    }
                }
            }

            // --- MÀN HÌNH TẠO LỜI NHẮC MỚI ---
            composable(Screen.ReminderAdd.route) {
                if (authUiState.isLoggedIn) {
                    val reminderViewModel: ReminderViewModel = hiltViewModel()
                    AddReminderScreen(
                        viewModel = reminderViewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route)
                    }
                }
            }
            composable(
                route = "update_reminder_screen?reminderId={reminderId}",
                arguments = listOf(
                    navArgument("reminderId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getInt("reminderId") ?: -1
                val reminderViewModel: ReminderViewModel = hiltViewModel()

                UpdateReminderScreen(
                    reminderId = reminderId,
                    viewModel = reminderViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}