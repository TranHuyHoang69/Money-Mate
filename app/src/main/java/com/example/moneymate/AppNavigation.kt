
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.screen.AddScreen
import com.example.moneymate.ui.screen.DetailExpenseScreen
import com.example.moneymate.ui.screen.DetailListScreen
import com.example.moneymate.ui.screen.GroupedExpenseScreen
import com.example.moneymate.ui.screen.HomeScreen
import com.example.moneymate.ui.screen.LoginScreen
import com.example.moneymate.ui.screen.RegisterScreen
import com.example.moneymate.ui.screen.UpdateScreen
import com.example.moneymate.viewmodel.HistoryViewModel
import com.example.moneymate.viewmodel.HomeViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Khởi tạo các ViewModel ở cấp cao nhất để quản lý trạng thái chung (như ngày tháng được chọn)
    val homeViewModel: HomeViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
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
            HomeScreen(
                navController = navController,
                historyViewModel = historyViewModel,
                homeViewModel = homeViewModel
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
            AddScreen(navController = navController)
        }

        // --- DETAIL LIST (Danh sách phẳng) ---
        // Trong AppNavigation.kt
        composable("history_all") { // Đặt tên route đơn giản thôi
            DetailListScreen(
                navController = navController,
                historyViewModel = historyViewModel,
                categoryId = 0L,        // Mặc định 0L là xem "Tất cả"
                categoryName = "Lịch sử", // Tên hiển thị trên Header
                type = "CHI PHÍ"         // Tab mặc định khi mới mở
            )
        }

        // --- GROUPED EXPENSE (Màn hình gộp theo ngày bạn vừa yêu cầu) ---
        composable(
            route = Screen.GroupedExpense.route,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("totalAmount") { type = NavType.StringType } // Truyền String để Double dễ parse
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("categoryName") ?: ""
            val total = backStackEntry.arguments?.getString("totalAmount")?.toDoubleOrNull() ?: 0.0

            GroupedExpenseScreen(
                navController = navController,
                historyViewModel = historyViewModel, // Dùng chung để đồng bộ ngày tháng
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
    }
}