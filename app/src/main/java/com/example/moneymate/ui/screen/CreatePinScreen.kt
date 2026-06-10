package com.example.moneymate.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.ui.component.PinPadComponent
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePinScreen(navController: NavController, viewModel: SecurityViewModel) {
    var step by remember { mutableStateOf(1) }
    var pinFirst by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(StringRes.setup_pin), fontSize = 20.sp, fontWeight = FontWeight.Bold) }, // ✅ Sửa lỗi compile nhãn id =
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(StringRes.back_btn) // ✅ Sửa lỗi compile nhãn id =
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // ✅ Thay đổi sang màu hệ thống Material 3 động thay vì hardcode mã HEX
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ✅ i18n: Tiêu đề hướng dẫn động dựa trên từng bước thiết lập
            Text(
                text = if (step == 1) {
                    stringResource(StringRes.enter_new_pin_hint) // ✅ Sửa lỗi compile nhãn id =
                } else {
                    stringResource(StringRes.confirm_new_pin_hint) // ✅ Sửa lỗi compile nhãn id =
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(StringRes.pin_purpose_desc), // ✅ Sửa lỗi compile nhãn id =
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Bàn phím số custom truyền dữ liệu vào xử lý logic
            PinPadComponent(
                currentInput = input,
                onInputChanged = {
                    input = it
                    if (input.length == 4) {
                        if (step == 1) {
                            pinFirst = input
                            input = ""
                            step = 2
                        } else {
                            if (input == pinFirst) {
                                viewModel.createPin(input)
                                // ✅ i18n AN TOÀN: Đọc chuỗi thông qua context.getString trong callback ngoài Composable
                                Toast.makeText(context, context.getString(StringRes.setup_pin_success), Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, context.getString(StringRes.pin_mismatch), Toast.LENGTH_SHORT).show()
                                input = ""
                                step = 1
                            }
                        }
                    }
                }
            )
        }
    }
}