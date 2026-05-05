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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Xử lý sau khi đăng ký thành công
    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            navController.navigate("login") {
                // Xóa màn hình register khỏi backstack để không quay lại được khi nhấn back
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // GIAO DIỆN TĨNH VỚI GRADIENT ĐỒNG BỘ MÀN LOGIN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4B8361), // Xanh MoneyMate
                        Color(0xFF2D503B)
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
                text = "Tạo tài khoản",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Bắt đầu hành trình quản lý tài chính",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.weight(0.05f))

            // White Card Form
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = Color.White,
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

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username Input
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Tên người dùng") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Hiển thị lỗi nếu có
                    if (uiState.error.isNotEmpty()) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Button Register
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.register(email, userName, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B8361))
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("ĐĂNG KÝ NGAY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Back to Login
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Đã có tài khoản? ", color = Color.Gray)
                        Text(
                            text = "Đăng nhập",
                            color = Color(0xFF4B8361),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.popBackStack() } // Dùng popBackStack cho mượt
                        )
                    }
                }
            }
        }
    }
}