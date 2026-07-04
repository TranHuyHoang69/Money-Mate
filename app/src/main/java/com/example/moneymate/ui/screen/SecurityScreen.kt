package com.example.moneymate.ui.screen

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.util.BiometricAuthHelper
import com.example.moneymate.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(navController: NavController, viewModel: SecurityViewModel, onOpenDrawer: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val securityState by viewModel.securityState.collectAsState()
    val context = LocalContext.current

    // Tải trước các chuỗi tài nguyên cho hộp thoại BiometricPrompt hệ thống (Sửa lỗi compile nhãn id =)
    val bioPromptTitle = stringResource(StringRes.security_bio_prompt_title)
    val bioPromptSubtitle = stringResource(StringRes.security_bio_prompt_subtitle)
    val bioPromptCancel = stringResource(StringRes.cancel_btn)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer, // ✅ Thay thế màu nạp cứng HEX bằng màu hệ thống linh hoạt
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .height(56.dp)
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
                        text = stringResource(StringRes.security_settings), // ✅ Sửa lỗi compile nhãn id =
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- PHẦN 1: QUẢN LÝ MÃ PIN ---
            Text(
                text = stringResource(StringRes.security_section_pin_title), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            if (!securityState.hasPin) {
                SecurityItemRow(
                    title = stringResource(StringRes.security_setup_pin_title), // ✅ Sửa lỗi compile nhãn id =
                    subtitle = stringResource(StringRes.security_setup_pin_subtitle), // ✅ Sửa lỗi compile nhãn id =
                    icon = Icons.Default.Lock,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("create_pin") }
                )
            } else {
                SecurityItemRow(
                    title = stringResource(StringRes.security_change_pin_title), // ✅ Sửa lỗi compile nhãn id =
                    subtitle = stringResource(StringRes.security_change_pin_subtitle), // ✅ Sửa lỗi compile nhãn id =
                    icon = Icons.Default.LockReset,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate(Screen.ChangePin.route) }
                )

                SecurityItemRow(
                    title = stringResource(StringRes.security_delete_pin_title), // ✅ Sửa lỗi compile nhãn id =
                    subtitle = stringResource(StringRes.security_delete_pin_subtitle), // ✅ Sửa lỗi compile nhãn id =
                    icon = Icons.Default.Delete,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { navController.navigate(Screen.DeletePin.route) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- PHẦN 2: QUẢN LÝ BIOMETRIC ---
            if (securityState.hasPin) {
                Text(
                    text = stringResource(StringRes.security_section_biometric_title), // ✅ Sửa lỗi compile nhãn id =
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = stringResource(StringRes.security_bio_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(StringRes.security_use_biometric_title), // ✅ Sửa lỗi compile nhãn id =
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(StringRes.security_use_biometric_subtitle), // ✅ Sửa lỗi compile nhãn id =
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = securityState.biometricEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary, // ✅ Gỡ White tĩnh, chuyển sang token tương phản động
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val biometricStatus = BiometricAuthHelper.canAuthenticate(context)
                                    if (!BiometricAuthHelper.isAvailable(biometricStatus)) {
                                        Toast.makeText(
                                            context,
                                            BiometricAuthHelper.statusMessage(biometricStatus),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val activity = BiometricAuthHelper.findFragmentActivity(context)
                                        if (activity == null) {
                                            Toast.makeText(
                                                context,
                                                "Khong the mo xac thuc sinh trac hoc. Vui long dung PIN.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val prompt = BiometricPrompt(
                                                activity,
                                                ContextCompat.getMainExecutor(context),
                                                object : BiometricPrompt.AuthenticationCallback() {
                                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                        viewModel.toggleBiometric(true)
                                                    }

                                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                        if (!BiometricAuthHelper.isUserCancelError(errorCode)) {
                                                            Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }

                                                    override fun onAuthenticationFailed() {
                                                        Toast.makeText(
                                                            context,
                                                            "Khong nhan dien duoc sinh trac hoc. Vui long thu lai hoac dung PIN.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            )
                                            val info = BiometricPrompt.PromptInfo.Builder()
                                                .setTitle(bioPromptTitle)
                                                .setSubtitle(bioPromptSubtitle)
                                                .setAllowedAuthenticators(BiometricAuthHelper.AUTHENTICATORS)
                                                .setNegativeButtonText(bioPromptCancel)
                                                .build()
                                            prompt.authenticate(info)
                                        }
                                    }
                                } else {
                                    viewModel.toggleBiometric(false)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(StringRes.navigation_arrow_desc), // ✅ Sửa lỗi compile nhãn id =
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
