package com.example.moneymate.ui.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.domain.model.UserPreferences
import com.example.moneymate.ui.component.AppDrawer
import com.example.moneymate.ui.screen.AddCategoryScreen
import com.example.moneymate.ui.screen.AddReminderScreen
import com.example.moneymate.ui.screen.AddScreen
import com.example.moneymate.ui.screen.AuthLockScreen
import com.example.moneymate.ui.screen.CategoryManagementScreen
import com.example.moneymate.ui.screen.ChangePinScreen
import com.example.moneymate.ui.screen.CreatePinScreen
import com.example.moneymate.ui.screen.CustomizationScreen
import com.example.moneymate.ui.screen.DeletePinScreen
import com.example.moneymate.ui.screen.DetailExpenseScreen
import com.example.moneymate.ui.screen.DetailListScreen
import com.example.moneymate.ui.screen.GroupedExpenseScreen
import com.example.moneymate.ui.screen.HomeScreen
import com.example.moneymate.ui.screen.LoginScreen
import com.example.moneymate.ui.screen.ProfileScreen
import com.example.moneymate.ui.screen.RegisterScreen
import com.example.moneymate.ui.screen.ReminderListScreen
import com.example.moneymate.ui.screen.SecurityScreen
import com.example.moneymate.ui.screen.UpdateReminderScreen
import com.example.moneymate.ui.screen.UpdateScreen
import com.example.moneymate.ui.theme.LocalLanguage
import com.example.moneymate.ui.theme.LocalUserPreferences
import com.example.moneymate.ui.theme.MoneyMateLocalizationProvider
import com.example.moneymate.ui.theme.MoneyMateTheme
import com.example.moneymate.viewmodel.AuthViewModel
import com.example.moneymate.viewmodel.CategoryViewModel
import com.example.moneymate.viewmodel.CustomizationUiState
import com.example.moneymate.viewmodel.CustomizationViewModel
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel
import com.example.moneymate.viewmodel.ReminderViewModel
import com.example.moneymate.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Lấy Context ở mức cao nhất (Đảm bảo là Activity Context chuẩn chưa bị wrap)
    val context = LocalContext.current

    // ✅ Ép kiểu biểu thức không-null một cách tường minh sang ViewModelStoreOwner
    val safeViewModelOwner = remember(context) {
        (context.findActivity() ?: context) as ViewModelStoreOwner
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Khởi tạo các ViewModel tầng gốc bằng safeViewModelOwner (Dùng chung cho cả app)
    val authViewModel: AuthViewModel = hiltViewModel(viewModelStoreOwner = safeViewModelOwner)
    val authUiState by authViewModel.uiState.collectAsState()

    val categoryViewModel: CategoryViewModel = hiltViewModel(viewModelStoreOwner = safeViewModelOwner)
    val securityViewModel: SecurityViewModel = hiltViewModel(viewModelStoreOwner = safeViewModelOwner)
    val securityUiState by securityViewModel.uiState.collectAsState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.SecuritySettings.route ||
            currentRoute == Screen.CreatePin.route ||
            currentRoute == Screen.ChangePin.route ||
            currentRoute == Screen.DeletePin.route) {
            securityViewModel.resetState()
        }
    }

    val allCategoriesEntity by categoryViewModel.allCategories.collectAsState(initial = emptyList<CategoryEntity>())

    val allCategoriesMapped = remember(allCategoriesEntity) {
        allCategoriesEntity.map { entity ->
            Category(
                id = entity.categoryId,
                title = entity.title,
                iconResName = entity.iconResName,
                colorHex = entity.colorHex,
                type = when(entity.type.toString().uppercase()) {
                    "INCOME" -> TransactionType.INCOME
                    else -> TransactionType.SPEND
                },
                isDefault = entity.isDefault
            )
        }
    }

    val sharedHomeViewModel: HomeViewModel = hiltViewModel(viewModelStoreOwner = safeViewModelOwner)
    val totalBalance by sharedHomeViewModel.totalBalance.collectAsState()

    val customizationViewModel: CustomizationViewModel = hiltViewModel(viewModelStoreOwner = safeViewModelOwner)
    val customizationState by customizationViewModel.uiState.collectAsState()
    val userPreferences = (customizationState as? CustomizationUiState.Success)?.preferences ?: UserPreferences()

    MoneyMateLocalizationProvider(
        languageCode = userPreferences.language
    ) {
        CompositionLocalProvider(
            LocalUserPreferences provides userPreferences,
            LocalLanguage provides userPreferences.language,
            // Ép các cấu trúc bọc ngoài sử dụng chung một ViewModelStoreOwner gốc từ Activity chuẩn
            LocalViewModelStoreOwner provides safeViewModelOwner
        ) {
            MoneyMateTheme(userPreferences = userPreferences) {
                AppDrawer(
                    drawerState = drawerState,
                    scope = scope,
                    authUiState = authUiState,
                    totalBalance = totalBalance,
                    themeColor = MaterialTheme.colorScheme.primary,
                    currentRoute = currentRoute,
                    categories = allCategoriesMapped,
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
                    },
                    onNavigateToAddCategory = { typeString ->
                        navController.navigate("add_category/$typeString")
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {

                        // --- MÀN HÌNH SPLASH ROUTER ---
                        composable("splash") {
                            val currentSecurityData by securityViewModel.securityState.collectAsState()

                            LaunchedEffect(currentSecurityData) {
                                val isPinEnabled = currentSecurityData.hasPin
                                val isBiometricEnabled = currentSecurityData.biometricEnabled

                                if (isPinEnabled || isBiometricEnabled) {
                                    navController.navigate(Screen.AuthLock.route) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.MainScreen.route) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // --- MÀN HÌNH TÙY CHỈNH CẤU HÌNH ---
                        composable(Screen.Customization.route) {
                            if (authUiState.isLoggedIn) {
                                CustomizationScreen(navController = navController, onOpenDrawer ={ scope.launch { drawerState.open() } } )
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        // --- MÀN HÌNH CỔNG CHẶN XÁC THỰC ---
                        composable(Screen.AuthLock.route) {
                            AuthLockScreen(
                                navController = navController,
                                viewModel = securityViewModel,
                                onAuthSuccess = {
                                    navController.navigate(Screen.MainScreen.route) {
                                        popUpTo(Screen.AuthLock.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- LOGIN & REGISTER ---
                        composable(Screen.Login.route) { LoginScreen(navController = navController) }
                        composable(Screen.Register.route) { RegisterScreen(navController = navController) }

                        // --- MAIN SCREEN (TRANG CHỦ) ---
                        composable(Screen.MainScreen.route) {
                            // Gọi hàm bổ trợ an toàn, không lo lỗi null mismatch
                            val historyViewModel: HistoryViewModel = safeHiltViewModel(safeViewModelOwner)
                            HomeScreen(
                                navController = navController,
                                historyViewModel = historyViewModel,
                                homeViewModel = sharedHomeViewModel,
                                authViewModel = authViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }

                        // --- THÔNG TIN CÁ NHÂN ---
                        composable(Screen.Profile.route) {
                            if (authUiState.isLoggedIn) {
                                ProfileScreen(
                                    authViewModel = authViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onLogoutOrDeleteSuccess = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        // --- CÀI ĐẶT BẢO MẬT & MÃ PIN ---
                        composable(Screen.SecuritySettings.route) {
                            if (authUiState.isLoggedIn) {
                                SecurityScreen(navController = navController, viewModel = securityViewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        composable(Screen.CreatePin.route) {
                            if (authUiState.isLoggedIn) {
                                CreatePinScreen(navController = navController, viewModel = securityViewModel)
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        composable(Screen.ChangePin.route) {
                            if (authUiState.isLoggedIn) {
                                ChangePinScreen(navController = navController, viewModel = securityViewModel)
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        composable(Screen.DeletePin.route) {
                            if (authUiState.isLoggedIn) {
                                DeletePinScreen(navController = navController, viewModel = securityViewModel)
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        // --- ADD EXPENSE ---
                        composable(
                            route = "add?expenseId={expenseId}",
                            arguments = listOf(navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L })
                        ) {
                            if (authUiState.isLoggedIn) AddScreen(navController = navController) else LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                        }

                        // --- DETAIL LIST MẶC ĐỊNH ---
                        composable(Screen.History.route) {
                            val historyViewModel: HistoryViewModel = safeHiltViewModel(safeViewModelOwner)
                            DetailListScreen(navController = navController, historyViewModel = historyViewModel, categoryId = 0L, categoryName = "Lịch sử", type = "CHI PHÍ", onOpenDrawer = { scope.launch { drawerState.open() } })
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

                            val historyViewModel: HistoryViewModel = safeHiltViewModel(safeViewModelOwner)

                            LaunchedEffect(calendarModeStr, startDate, endDate) {
                                try {
                                    val modeEnum = com.example.moneymate.viewmodel.CalendarMode.valueOf(calendarModeStr)
                                    historyViewModel.changeMode(modeEnum)
                                } catch (e: Exception) {
                                    android.util.Log.e("Navigation", "Error context: ${e.message}")
                                }
                                if (startDate > 0L && endDate > 0L) {
                                    historyViewModel.loadCustomRange(startDate, endDate)
                                }
                            }

                            GroupedExpenseScreen(navController = navController, historyViewModel = historyViewModel, categoryName = categoryName, totalAmount = totalAmount)
                        }

                        // --- DETAIL ONE EXPENSE ---
                        composable(Screen.DetailExpense.route, arguments = listOf(navArgument("firestoreDocId") { type = NavType.StringType })) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("firestoreDocId").orEmpty()
                            DetailExpenseScreen(navController = navController, firestoreDocId = docId)
                        }

                        // --- UPDATE EXPENSE ---
                        composable("update_screen/{firestoreDocId}", arguments = listOf(navArgument("firestoreDocId") { type = NavType.StringType })) { backStackEntry ->
                            val firestoreDocId = backStackEntry.arguments?.getString("firestoreDocId") ?: ""
                            UpdateScreen(navController = navController, firestoreDocId = firestoreDocId)
                        }

                        // --- ADD CATEGORY ---
                        composable("add_category/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: "SPEND"
                            val categoryViewModel: CategoryViewModel = safeHiltViewModel(safeViewModelOwner)
                            AddCategoryScreen(initialType = type, onBack = { navController.popBackStack() }, onSave = { entity ->
                                categoryViewModel.insertCategory(entity)
                                navController.popBackStack()
                            })
                        }

                        // --- CATEGORY MANAGEMENT ---
                        composable("category_management/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: "SPEND"
                            CategoryManagementScreen(navController = navController, initialType = type, onOpenDrawer ={ scope.launch { drawerState.open() } } )
                        }

                        // --- MÀN HÌNH DANH SÁCH NHẮC NHỞ ---
                        composable(Screen.Reminder.route) {
                            if (authUiState.isLoggedIn) {
                                val reminderViewModel: ReminderViewModel = safeHiltViewModel(safeViewModelOwner)
                                LaunchedEffect(Unit) { reminderViewModel.refreshUser() }
                                ReminderListScreen(viewModel = reminderViewModel, navController = navController, onOpenDrawer = { scope.launch { drawerState.open() } }, onAddReminderClick = { navController.navigate(Screen.ReminderAdd.route) })
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) { popUpTo(Screen.MainScreen.route) { saveState = true } } }
                            }
                        }

                        // --- MÀN HÌNH TẠO LỜI NHẮC MỚI ---
                        composable(Screen.ReminderAdd.route) {
                            if (authUiState.isLoggedIn) {
                                val reminderViewModel: ReminderViewModel = safeHiltViewModel(safeViewModelOwner)
                                AddReminderScreen(viewModel = reminderViewModel, onBackClick = { navController.popBackStack() })
                            } else {
                                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) }
                            }
                        }

                        // --- CẬP NHẬT LỜI NHẮC ---
                        composable("update_reminder_screen?reminderId={reminderId}", arguments = listOf(navArgument("reminderId") { type = NavType.IntType })) { backStackEntry ->
                            val reminderId = backStackEntry.arguments?.getInt("reminderId") ?: -1
                            val reminderViewModel: ReminderViewModel = safeHiltViewModel(safeViewModelOwner)
                            UpdateReminderScreen(reminderId = reminderId, viewModel = reminderViewModel, onBackClick = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

// Hàm unwrap Context đệ quy bằng vòng lặp, bóc tách chính xác ra tận FragmentActivity gốc (kế thừa từ ComponentActivity)
fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

// ✅ ĐÃ SỬA: Hàm bổ trợ xử lý null an toàn cho LocalViewModelStoreOwner.current bằng toán tử Elvis fallback
@Composable
inline fun <reified VM : androidx.lifecycle.ViewModel> safeHiltViewModel(fallbackOwner: ViewModelStoreOwner): VM {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Kiểm tra và gán an toàn, nếu LocalViewModelStoreOwner trả về null thì dùng fallbackOwner
    val owner = LocalViewModelStoreOwner.current ?: fallbackOwner

    return if (activity != null) {
        hiltViewModel(viewModelStoreOwner = owner)
    } else {
        hiltViewModel()
    }
}