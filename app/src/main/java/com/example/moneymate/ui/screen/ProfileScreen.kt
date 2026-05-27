package com.example.moneymate.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel, // 👈 Truyền đúng AuthViewModel vào đây
    onBackClick: () -> Unit,
    onLogoutOrDeleteSuccess: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current

    // Thu thập trạng thái UI từ AuthViewModel của bạn
    val uiState by authViewModel.uiState.collectAsState()
    val currentUser = uiState.user

    // Đọc thông tin từ đối tượng User Domain model của bạn
    val name = currentUser?.userName ?: "Người dùng MoneyMate"
    val email = currentUser?.email ?: "Chưa cập nhật Email"

    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- DIALOG XÁC NHẬN XÓA TÀI KHOẢN ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E3E2F),
            title = { Text("Xóa tài khoản?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hành động này không thể hoàn tác. Toàn bộ dữ liệu của bạn sẽ bị xóa vĩnh viễn khỏi hệ thống.",
                    color = Color(0xFF8E8E93)
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("HỦY", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteUserAccount(
                            onSuccess = {
                                Toast.makeText(context, "Đã xóa tài khoản thành công", Toast.LENGTH_SHORT).show()
                                onLogoutOrDeleteSuccess()
                            },
                            onFailure = {
                                Toast.makeText(context, "Vui lòng đăng nhập lại trước khi thực hiện hành động bảo mật này!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED5E5E))
                ) {
                    Text("XÓA VĨNH VIỄN", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF131A05)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- TOP HEADER BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1E3E2F),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Thông tin tài khoản",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- VÙNG THÔNG TIN CHI TIẾT ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileInfoCard(label = "Tên tài khoản", value = name)
                ProfileInfoCard(label = "Địa chỉ Email", value = email)
            }

            // --- KHU VỰC CÁC NÚT CHỨC NĂNG PHÍA DƯỚI ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nút Xóa tài khoản
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f))
                ) {
                    Text(text = "Xóa tài khoản", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E3E2F), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}