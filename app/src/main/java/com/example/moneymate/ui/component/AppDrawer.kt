package com.example.moneymate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.StringRes
import com.example.moneymate.domain.model.Category
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.AuthUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    authUiState: AuthUiState,
    totalBalance: Double,
    themeColor: Color,
    currentRoute: String?,
    categories: List<Category>,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToAddCategory: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val isLoggedIn = authUiState.isLoggedIn

    // Cấu hình màu sắc của NavigationDrawerItem tự động thích ứng với cấu hình Light/Dark Theme hệ thống
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = themeColor.copy(alpha = 0.15f),
        selectedIconColor = themeColor,
        selectedTextColor = themeColor,
        unselectedContainerColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurface
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- THÔNG TIN TÀI KHOẢN ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                if (isLoggedIn) onNavigate(Screen.Profile.route) else onNavigate(Screen.Login.route)
                            }
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(themeColor.copy(0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = if (isLoggedIn) authUiState.user?.userName?.take(1) ?: "U" else "?"
                            Text(initial, fontWeight = FontWeight.Bold, color = themeColor, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isLoggedIn) {
                                authUiState.user?.userName ?: stringResource(StringRes.loading) // ✅ Sửa lỗi truyền đối số tường minh
                            } else {
                                stringHighlightOrText("Chế độ khách")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isLoggedIn) {
                            val balanceFormatter = remember {
                                DecimalFormat("#,###", java.text.DecimalFormatSymbols().apply { groupingSeparator = '.' })
                            }
                            val formatBalance = if (totalBalance == 0.0) "0" else balanceFormatter.format(totalBalance)

                            // ✅ Áp dụng cơ chế Format an toàn bằng cách gọi hàm nạp chồng định dạng vararg mới bảo vệ Runtime
                            val currencySuffix = stringResource(com.example.moneymate.R.string.currency_format)
                            val displayFormat = remember(totalBalance, currencySuffix, formatBalance) {
                                try {
                                    if (currencySuffix.contains("%")) {
                                        String.format(currencySuffix, totalBalance)
                                    } else {
                                        "$formatBalance đ"
                                    }
                                } catch (e: Exception) {
                                    "$formatBalance đ" // Fallback nếu cấu trúc file string.xml bị định dạng sai kiểu dữ liệu
                                }
                            }

                            Text(
                                text = displayFormat,
                                color = if (totalBalance >= 0) themeColor else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = stringResource(StringRes.no_account), // ✅ Sửa lỗi 'id =' trùng lặp signature
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- MỤC TRANG CHỦ ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_home)) },
                        selected = currentRoute == Screen.MainScreen.route,
                        icon = { Icon(Icons.Default.Home, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(Screen.MainScreen.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC THÔNG TIN CÁ NHÂN ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_profile)) },
                        selected = currentRoute == Screen.Profile.route,
                        icon = { Icon(Icons.Default.Person, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.Profile.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC LỊCH SỬ ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_history)) },
                        selected = currentRoute == Screen.History.route,
                        icon = { Icon(Icons.Default.History, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.History.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    // --- MỤC THỐNG KÊ ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_statistics)) },
                        selected = currentRoute == Screen.Statistics.route,
                        icon = { Icon(Icons.Default.BarChart, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.Statistics.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC DANH MỤC ---
                    NavigationDrawerItem(
                        label = { Text(text = "Ngân sách") },
                        selected = currentRoute == Screen.Budget.route,
                        icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.Budget.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(text = "Giao dịch định kỳ") },
                        selected = currentRoute == Screen.RecurringTransactions.route,
                        icon = { Icon(Icons.Default.Repeat, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.RecurringTransactions.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_categories)) },
                        selected = currentRoute?.startsWith("category_management") == true,
                        icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate("category_management/SPEND")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC NHẮC NHỞ ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_reminders)) },
                        selected = currentRoute == Screen.Reminder.route,
                        icon = { Icon(Icons.Default.Update, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.Reminder.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC CÀI ĐẶT BẢO MẬT ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_security)) },
                        selected = currentRoute == Screen.SecuritySettings.route,
                        icon = { Icon(Icons.Default.Lock, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.SecuritySettings.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // --- MỤC TÙY CHỈNH ---
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(StringRes.nav_customization)) },
                        selected = currentRoute == Screen.Customization.route,
                        icon = { Icon(Icons.Default.Settings, null) },
                        colors = drawerItemColors,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isLoggedIn) onNavigate(Screen.Customization.route) else onNavigate(Screen.Login.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // --- KHU VỰC ĐĂNG XUẤT / ĐĂNG NHẬP Ở ĐÁY MENU ---
                    if (isLoggedIn) {
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(StringRes.nav_logout)) },
                            selected = false,
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                            onClick = {
                                scope.launch { drawerState.close() }
                                onLogout()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedIconColor = MaterialTheme.colorScheme.error,
                                unselectedTextColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            NavigationDrawerItem(
                                label = { Text(text = stringResource(StringRes.nav_login)) },
                                selected = false,
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavigate(Screen.Login.route)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            NavigationDrawerItem(
                                label = { Text(text = stringResource(StringRes.nav_register)) },
                                selected = false,
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavigate(Screen.Register.route)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}

// Hàm bổ trợ nội bộ để xử lý chuỗi "Chế độ khách" nếu không muốn nạp cứng
@Composable
private fun stringHighlightOrText(fallback: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return try {
        context.getString(com.example.moneymate.R.string.system_default)
    } catch (e: Exception) {
        fallback
    }
}
