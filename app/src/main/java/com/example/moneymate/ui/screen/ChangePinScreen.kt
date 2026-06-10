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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
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
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.SecurityViewModel

private enum class ChangePinStep {
    OLD_PIN, NEW_PIN, CONFIRM_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    navController: NavController,
    viewModel: SecurityViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val pinLength = 4

    var currentStep by remember { mutableStateOf(ChangePinStep.OLD_PIN) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }

    val currentInput = when (currentStep) {
        ChangePinStep.OLD_PIN -> oldPin
        ChangePinStep.NEW_PIN -> newPin
        ChangePinStep.CONFIRM_PIN -> confirmNewPin
    }

    LaunchedEffect(uiState.success, uiState.error) {
        if (confirmNewPin.length == pinLength) {
            if (uiState.success) {
                // ✅ i18n AN TOÀN: Đọc chuỗi thông qua context.getString ngoài phạm vi Composable
                Toast.makeText(context, context.getString(StringRes.change_pin_success), Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            uiState.error?.let {
                Toast.makeText(context, "${context.getString(StringRes.enable_pin)}: $it", Toast.LENGTH_SHORT).show()
                currentStep = ChangePinStep.OLD_PIN
                oldPin = ""
                newPin = ""
                confirmNewPin = ""
            }
        }
    }

    fun onNumberClick(number: String) {
        if (currentInput.length >= pinLength) return

        val updatedInput = currentInput + number

        when (currentStep) {
            ChangePinStep.OLD_PIN -> {
                oldPin = updatedInput
                if (oldPin.length == pinLength) {
                    currentStep = ChangePinStep.NEW_PIN
                }
            }
            ChangePinStep.NEW_PIN -> {
                newPin = updatedInput
                if (newPin.length == pinLength) {
                    currentStep = ChangePinStep.CONFIRM_PIN
                }
            }
            ChangePinStep.CONFIRM_PIN -> {
                confirmNewPin = updatedInput
                if (confirmNewPin.length == pinLength) {
                    if (newPin == confirmNewPin) {
                        viewModel.changePin(old = oldPin, new = newPin)
                    } else {
                        Toast.makeText(context, context.getString(StringRes.pin_mismatch), Toast.LENGTH_SHORT).show()
                        currentStep = ChangePinStep.NEW_PIN
                        newPin = ""
                        confirmNewPin = ""
                    }
                }
            }
        }
    }

    fun onBackspaceClick() {
        if (currentInput.isNotEmpty()) {
            val truncated = currentInput.dropLast(1)
            when (currentStep) {
                ChangePinStep.OLD_PIN -> oldPin = truncated
                ChangePinStep.NEW_PIN -> newPin = truncated
                ChangePinStep.CONFIRM_PIN -> confirmNewPin = truncated
            }
        } else {
            when (currentStep) {
                ChangePinStep.NEW_PIN -> currentStep = ChangePinStep.OLD_PIN
                ChangePinStep.CONFIRM_PIN -> currentStep = ChangePinStep.NEW_PIN
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(StringRes.change_pin), fontSize = 20.sp, fontWeight = FontWeight.Bold) }, // ✅ Sửa lỗi nhãn id =
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(StringRes.back_btn)) // ✅ Sửa lỗi nhãn id =
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // ✅ Chuyển màu động M3 thay vì hardcode mã HEX xanh
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                // Tiêu đề hướng dẫn từng bước
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        text = when (currentStep) {
                            ChangePinStep.OLD_PIN -> stringResource(StringRes.enter_old_pin_hint) // ✅ Sửa lỗi nhãn id =
                            ChangePinStep.NEW_PIN -> stringResource(StringRes.enter_new_pin_hint) // ✅ Sửa lỗi nhãn id =
                            ChangePinStep.CONFIRM_PIN -> stringResource(StringRes.confirm_new_pin_hint) // ✅ Sửa lỗi nhãn id =
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(StringRes.pin_too_short, pinLength), // ✅ Sửa lỗi nhãn id = và giữ tham số động
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Hiển thị các chấm tròn trạng thái nhập ký tự
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until pinLength) {
                            val isFilled = i < currentInput.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)) // ✅ Đồng bộ màu primary hệ thống
                                    .border(1.dp, if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }

                // Bàn phím số Custom
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
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = stringResource(StringRes.back_btn), // ✅ Sửa lỗi nhãn id =
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // ✅ Đồng bộ màu Loader hệ thống
                }
            }
        }
    }
}