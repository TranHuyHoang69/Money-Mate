
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.screen.AddCategoryScreen
import com.example.moneymate.ui.screen.AddScreen
import com.example.moneymate.ui.screen.CategoryManagementScreen
import com.example.moneymate.ui.screen.DetailExpenseScreen
import com.example.moneymate.ui.screen.DetailListScreen
import com.example.moneymate.ui.screen.GroupedExpenseScreen
import com.example.moneymate.ui.screen.HomeScreen
import com.example.moneymate.ui.screen.LoginScreen
import com.example.moneymate.ui.screen.RegisterScreen
import com.example.moneymate.ui.screen.UpdateScreen
import com.example.moneymate.viewmodel.AuthViewModel
import com.example.moneymate.viewmodel.CategoryViewModel
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Chỉ giữ AuthViewModel ở đây để check trạng thái Login toàn cục
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()


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

        // --- MAIN SCREEN ---
        composable(Screen.MainScreen.route) {
            // Chỉ khởi tạo ViewModel khi người dùng đã vào Home
            val homeViewModel: HomeViewModel = hiltViewModel()
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HomeScreen(
                navController = navController,
                historyViewModel = historyViewModel,
                homeViewModel = homeViewModel,
                authViewModel = authViewModel
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
            if(authUiState.isLoggedIn){
                AddScreen(navController = navController)
            }else{
                LaunchedEffect(Unit) {navController.navigate(Screen.Login.route) }
            }
        }

        // --- DETAIL LIST ---
        composable("history_all") {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            DetailListScreen(
                navController = navController,
                historyViewModel = historyViewModel,
                categoryId = 0L,
                categoryName = "Lịch sử",
                type = "CHI PHÍ"
            )
        }

        // --- GROUPED EXPENSE ---
        composable(
            route = Screen.GroupedExpense.route,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("totalAmount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("categoryName") ?: ""
            val total = backStackEntry.arguments?.getString("totalAmount")?.toDoubleOrNull() ?: 0.0

            // ViewModel này sẽ được giải phóng khi thoát màn hình này
            val historyViewModel: HistoryViewModel = hiltViewModel()

            GroupedExpenseScreen(
                navController = navController,
                historyViewModel = historyViewModel,
                categoryName = name,
                totalAmount = total
            )
        }

        // --- DETAIL ONE EXPENSE ---
        composable(
            route = Screen.DetailExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            DetailExpenseScreen(navController = navController, expenseId = id)
        }

        // --- UPDATE EXPENSE ---
        composable(
            route = Screen.Update.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("expenseId") ?: -1L
            UpdateScreen(navController = navController, expenseId = id)
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
    }
}