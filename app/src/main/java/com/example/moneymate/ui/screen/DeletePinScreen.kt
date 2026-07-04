package com.example.moneymate.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.ui.theme.AppTopBarColor
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePinScreen(
    navController: NavController,
    viewModel: SecurityViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val pinLength = 4
    var pinInput by remember { mutableStateOf("") }

    // Theo dõi trạng thái xóa từ ViewModel
    LaunchedEffect(uiState.success, uiState.error) {
        if (pinInput.length == pinLength) {
            if (uiState.success) {
                // ✅ i18n AN TOÀN: Sử dụng context.getString trong LaunchedEffect ngoài phạm vi Composable
                Toast.makeText(context, context.getString(StringRes.delete_pin_success), Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            uiState.error?.let {
                Toast.makeText(context, "${context.getString(StringRes.error)}: $it", Toast.LENGTH_SHORT).show()
                pinInput = ""
            }
        }
    }

    fun onNumberClick(number: String) {
        if (pinInput.length >= pinLength) return

        pinInput += number

        if (pinInput.length == pinLength) {
            viewModel.deletePin(pin = pinInput)
        }
    }

    fun onBackspaceClick() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(StringRes.delete_pin), fontSize = 20.sp, fontWeight = FontWeight.Bold) }, // ✅ Sửa lỗi compile nhãn id =
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(StringRes.back_btn) // ✅ Sửa lỗi compile nhãn id =
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tiêu đề & Cảnh báo hủy mã PIN
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        text = stringResource(StringRes.confirm_delete_pin_heading), // ✅ Sửa lỗi compile nhãn id =
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error // ✅ Sử dụng màu Error hệ thống thay vì mã đỏ nạp cứng
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(StringRes.delete_pin_warning_desc), // ✅ Sửa lỗi compile nhãn id =
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Hiển thị các ô chấm tròn nhập liệu
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until pinLength) {
                            val isFilled = i < pinInput.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)) // ✅ Đồng bộ theo màu error hệ thống
                                    .border(1.dp, if (isFilled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }

                // Bàn phím số ảo Custom
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "backspace")

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(keys) { key ->
                            when (key) {
                                "" -> Box(modifier = Modifier.size(70.dp))
                                "backspace" -> {
                                    Box(
                                        modifier = Modifier.size(70.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { onBackspaceClick() },
                                            modifier = Modifier.size(70.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = stringResource(StringRes.delete_btn), // ✅ Sửa lỗi compile nhãn id =
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .clickable { onNumberClick(key) }
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.error) // ✅ Loader đồng bộ theo tone đỏ cảnh báo hệ thống
                }
            }
        }
    }
}
