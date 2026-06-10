package com.example.moneymate.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.R
import com.example.moneymate.StringRes
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var emailState by remember { mutableStateOf("") }
    var passwordState by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("GoogleAuth", "Activity result: ${result.resultCode}")
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)

            android.util.Log.d("GoogleAuth", "Google sign-in successful: ${account.email}")
            val idToken = account.idToken

            if (idToken != null) {
                android.util.Log.d("GoogleAuth", "ID Token obtained, signing in with Firebase...")
                viewModel.signInWithFirebase(idToken)
            } else {
                android.util.Log.e("GoogleAuth", "No ID token received")
            }
        } catch (e: ApiException) {
            android.util.Log.e("GoogleAuth", "Google sign-in failed: ${e.statusCode} - ${e.message}")
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.MainScreen.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            // Header Section
            Spacer(modifier = Modifier.weight(0.3f))
            Text(
                text = stringResource(StringRes.app_name), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary // ✅ Đồng bộ màu chữ trên nền Primary thích ứng
            )
            Text(
                text = stringResource(StringRes.login_slogan), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) // ✅ Đồng bộ màu chữ thích ứng hệ thống
            )
            Spacer(modifier = Modifier.weight(0.1f))

            // Form Content bọc bên trong Surface
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                    Text(
                        text = stringResource(StringRes.login_welcome_title), // ✅ Sửa lỗi compile nhãn id =
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // 1. Ô nhập Email cô lập Recomposition
                    EmailInputField(
                        value = emailState,
                        onValueChange = { emailState = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Ô nhập Mật khẩu cô lập Recomposition
                    PasswordInputField(
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

                    // 3. Nút Đăng nhập hệ thống
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(emailState, passwordState)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(text = stringResource(StringRes.login_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp) // ✅ Sửa lỗi compile nhãn id =
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Điều hướng sang màn hình đăng ký
                    Row {
                        Text(text = stringResource(StringRes.no_account), color = MaterialTheme.colorScheme.onSurfaceVariant) // ✅ Sửa lỗi compile nhãn id =
                        Text(
                            text = stringResource(StringRes.register_btn), // ✅ Sửa lỗi compile nhãn id =
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.navigate("register") }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(text = stringResource(StringRes.or_separator), modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) // ✅ Sửa lỗi compile nhãn id =
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Nút đăng nhập Google thích ứng
                    GoogleSignInButton(
                        onClick = {
                            focusManager.clearFocus()
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("458037840211-8591f2k268qa9cfr5q0uefuss5tcd0qh.apps.googleusercontent.com")
                                .requestEmail()
                                .build()

                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun EmailInputField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(StringRes.email_label)) }, // ✅ Sửa lỗi compile nhãn id =
        placeholder = { Text(text = stringResource(StringRes.email_hint)) }, // ✅ Sửa lỗi compile nhãn id =
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}

@Composable
fun PasswordInputField(value: String, onValueChange: (String) -> Unit) {
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

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth().height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = stringResource(StringRes.login_with_google), // ✅ Sửa lỗi compile nhãn id =
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(StringRes.continue_with_google_btn), // ✅ Sửa lỗi compile nhãn id =
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}