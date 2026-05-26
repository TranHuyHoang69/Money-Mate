package com.example.moneymate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
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
import com.example.moneymate.ui.navigation.Screen
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
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val isLoggedIn = authUiState.isLoggedIn
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                // --- THÔNG TIN TÀI KHOẢN ---
                Column(modifier = Modifier.padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(themeColor.copy(0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = if (isLoggedIn) authUiState.user?.userName?.take(1) ?: "U" else "?"
                        Text(initial, fontWeight = FontWeight.Bold, color = themeColor, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isLoggedIn) authUiState.user?.userName ?: "Người dùng" else "Chế độ khách",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoggedIn) {
                        val balanceFormatter = remember { DecimalFormat("#,###", java.text.DecimalFormatSymbols().apply {
                            groupingSeparator = '.'
                        })
                        }
                        val formatBalance = if(totalBalance == 0.0) "0" else balanceFormatter.format(totalBalance)
                        Text(
                            text = "$formatBalance đ",
                            color = if (totalBalance >= 0) themeColor else Color(0xFFDC3545),
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text("Đăng nhập để đồng bộ dữ liệu", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // --- MỤC TRANG CHỦ ---
                NavigationDrawerItem(
                    label = { Text("Trang chủ") },
                    selected = currentRoute == Screen.MainScreen.route,
                    icon = { Icon(Icons.Default.Home, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        // ✅ SỬA TẠI ĐÂY: Luôn cho phép về Trang chủ, không chặn bằng isLoggedIn nữa
                        onNavigate(Screen.MainScreen.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // --- MỤC LỊCH SỬ ---
                NavigationDrawerItem(
                    label = { Text("Lịch sử") },
                    selected = currentRoute == Screen.History.route,
                    icon = { Icon(Icons.Default.History, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        // ✅ SỬA TẠI ĐÂY: Nếu đã đăng nhập thì đi tiếp, chế độ khách thì chuyển sang Login
                        if (isLoggedIn) onNavigate(Screen.History.route) else onNavigate(Screen.Login.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // --- MỤC NHẮC NHỞ ---
                NavigationDrawerItem(
                    label = { Text("Nhắc nhở") },
                    selected = currentRoute == Screen.Reminder.route,
                    icon = { Icon(Icons.Default.Update, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        // ✅ ĐỒNG BỘ: Đảm bảo kiểm tra giống mục Lịch sử
                        if (isLoggedIn) onNavigate(Screen.Reminder.route) else onNavigate(Screen.Login.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                // --- KHU VỰC ĐĂNG XUẤT / ĐĂNG NHẬP Ở ĐÁY MENU ---
                if (isLoggedIn) {
                    NavigationDrawerItem(
                        label = { Text("Đăng xuất") },
                        selected = false,
                        icon = { Icon(Icons.Default.Logout, null) },
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = Color.Red,
                            unselectedTextColor = Color.Red
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        NavigationDrawerItem(
                            label = { Text("Đăng nhập") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigate(Screen.Login.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NavigationDrawerItem(
                            label = { Text("Đăng ký") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigate(Screen.Register.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        content = content
    )
}