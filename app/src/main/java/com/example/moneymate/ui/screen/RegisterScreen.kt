package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var emailState by remember { mutableStateOf("") }
    var userNameState by remember { mutableStateOf("") }
    var passwordState by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,       // ✅ Đồng bộ dải màu gradient thích ứng động
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Spacer(modifier = Modifier.weight(0.15f))

            Text(
                text = stringResource(StringRes.register_header_title), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary // ✅ Sử dụng màu chữ chuẩn tương phản trên nền primary
            )
            Text(
                text = stringResource(StringRes.register_slogan), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.weight(0.05f))

            // Khung Form nhập liệu bọc bên trong Surface thích ứng
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Ô nhập Email cô lập Recomposition
                    RegisterEmailField(
                        value = emailState,
                        onValueChange = { emailState = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Ô nhập Username cô lập Recomposition
                    RegisterUsernameField(
                        value = userNameState,
                        onValueChange = { userNameState = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Ô nhập Mật khẩu cô lập Recomposition
                    RegisterPasswordField(
                        value = passwordState,
                        onValueChange = { passwordState = it }
                    )

                    if (uiState.error.isNotEmpty()) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Nút Đăng ký (Đồng bộ theo ColorScheme của hệ thống)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.register(emailState, userNameState, passwordState)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(text = stringResource(StringRes.register_btn_upper), fontWeight = FontWeight.Bold, fontSize = 16.sp) // ✅ Sửa lỗi compile nhãn id =
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quay lại màn hình đăng nhập
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(StringRes.already_account), // ✅ Sửa lỗi compile nhãn id =
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(StringRes.login_btn), // ✅ Sửa lỗi compile nhãn id =
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// --- CÁC THÀNH PHẦN ĐƯỢC TÁCH BIỆT ĐỒNG BỘ THEO MATERIALTHEME ---

@Composable
fun RegisterEmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(StringRes.email_label)) }, // ✅ Sửa lỗi compile nhãn id =
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}

@Composable
fun RegisterUsernameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(StringRes.username_label)) }, // ✅ Sửa lỗi compile nhãn id =
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun RegisterPasswordField(value: String, onValueChange: (String) -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(StringRes.password_label)) }, // ✅ Sửa lỗi compile nhãn id =
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(StringRes.toggle_password_visibility_desc) // ✅ Sửa lỗi compile nhãn id =
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}