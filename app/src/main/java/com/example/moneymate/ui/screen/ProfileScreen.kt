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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.StringRes
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onLogoutOrDeleteSuccess: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current

    val uiState by authViewModel.uiState.collectAsState()
    val currentUser = uiState.user

    // ✅ i18n: Giá trị mặc định khi thiếu thông tin người dùng (Sửa lỗi compile nhãn id =)
    val name = currentUser?.userName ?: stringResource(StringRes.profile_default_username)
    val email = currentUser?.email ?: stringResource(StringRes.profile_default_email)

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Thông báo Toast được tải trước từ tài nguyên hệ thống (Sửa lỗi compile nhãn id =)
    val deleteSuccessMsg = stringResource(StringRes.profile_delete_success_toast)
    val deleteFailureMsg = stringResource(StringRes.profile_delete_failure_toast)

    // --- DIALOG XÁC NHẬN XÓA TÀI KHOẢN (ĐỒNG BỘ THEO THEME) ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(StringRes.delete_account), // ✅ Sửa lỗi compile nhãn id =
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(StringRes.delete_account_confirm), // ✅ Sửa lỗi compile nhãn id =
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(StringRes.cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant) // ✅ Sửa lỗi compile nhãn id =
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteUserAccount(
                            onSuccess = {
                                Toast.makeText(context, deleteSuccessMsg, Toast.LENGTH_SHORT).show()
                                onLogoutOrDeleteSuccess()
                            },
                            onFailure = {
                                Toast.makeText(context, deleteFailureMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(StringRes.profile_delete_confirm_btn_upper), fontWeight = FontWeight.Bold) // ✅ Sửa lỗi compile nhãn id =
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
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
                        color = MaterialTheme.colorScheme.primaryContainer, // ✅ Sử dụng màu Container hệ thống thay vì nạp cứng HEX
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
                            contentDescription = stringResource(StringRes.menu_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(StringRes.user_info), // ✅ Sửa lỗi compile nhãn id =
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                ProfileInfoCard(label = stringResource(StringRes.profile_username_label), value = name) // ✅ Sửa lỗi compile nhãn id =
                ProfileInfoCard(label = stringResource(StringRes.profile_email_label), value = email)       // ✅ Sửa lỗi compile nhãn id =
            }

            // --- KHU VỰC CÁC NÚT CHỨC NĂNG PHÍA DƯỚI ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nút Xóa tài khoản (Outlined thích ứng nguy hiểm)
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Text(text = stringResource(StringRes.delete_account), fontSize = 16.sp, fontWeight = FontWeight.Normal) // ✅ Sửa lỗi compile nhãn id =
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
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}