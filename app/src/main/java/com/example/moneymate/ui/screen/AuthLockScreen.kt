package com.example.moneymate.ui.screen

import android.widget.Toast
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.util.BiometricAuthHelper
import com.example.moneymate.viewmodel.SecurityViewModel

@Composable
fun AuthLockScreen(
    navController: NavController,
    viewModel: SecurityViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val securityState by viewModel.securityState.collectAsState()

    val pinLength = 4
    var pinInput by remember { mutableStateOf("") }
    var biometricPromptShown by remember { mutableStateOf(false) }
    var biometricUnavailableNotified by remember { mutableStateOf(false) }
    val isBiometricEnabled = securityState.biometricEnabled
    val biometricStatus = remember(context, isBiometricEnabled) {
        if (isBiometricEnabled) {
            BiometricAuthHelper.canAuthenticate(context)
        } else {
            null
        }
    }
    val isBiometricAvailable = biometricStatus?.let(BiometricAuthHelper::isAvailable) == true

    fun showBiometricPrompt() {
        val status = BiometricAuthHelper.canAuthenticate(context)
        if (!BiometricAuthHelper.isAvailable(status)) {
            Toast.makeText(context, BiometricAuthHelper.statusMessage(status), Toast.LENGTH_SHORT).show()
            return
        }

        val activity = BiometricAuthHelper.findFragmentActivity(context)
        if (activity == null) {
            Toast.makeText(
                context,
                "Khong the mo xac thuc sinh trac hoc. Vui long dung PIN.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (!BiometricAuthHelper.isUserCancelError(errorCode)) {
                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        context,
                        "Khong nhan dien duoc sinh trac hoc. Vui long thu lai hoac dung PIN.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // ✅ i18n AN TOÀN: Đọc string trực tiếp từ context thuần do không nằm trong phạm vi vẽ giao diện Composable
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(StringRes.auth_title))
            .setSubtitle(context.getString(StringRes.auth_subtitle))
            .setAllowedAuthenticators(BiometricAuthHelper.AUTHENTICATORS)
            .setNegativeButtonText(context.getString(StringRes.use_pin_fallback))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(isBiometricEnabled, isBiometricAvailable) {
        if (isBiometricEnabled && isBiometricAvailable && !biometricPromptShown) {
            biometricPromptShown = true
            showBiometricPrompt()
        } else if (
            isBiometricEnabled &&
            !isBiometricAvailable &&
            biometricStatus != null &&
            !biometricUnavailableNotified
        ) {
            biometricUnavailableNotified = true
            Toast.makeText(context, BiometricAuthHelper.statusMessage(biometricStatus), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.isUnlocked, uiState.error) {
        if (uiState.isUnlocked) {
            onAuthSuccess()
            viewModel.resetState()
        }

        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            pinInput = ""
            viewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        pinInput = ""
        viewModel.resetState()
    }

    fun onNumberClick(number: String) {
        if (pinInput.length >= pinLength) return
        pinInput += number

        if (pinInput.length == pinLength) {
            viewModel.verifyPin(pin = pinInput)
        }
    }

    fun onBackspaceClick() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = stringResource(StringRes.enter_pin_hint), // ✅ Sửa lỗi compile nhãn id =
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Các ô chấm tròn nhập PIN
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
                                .background(if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }

            // Bàn phím số Custom
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "biometric", "0", "backspace")

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(keys) { key ->
                        when (key) {
                            "biometric" -> {
                                Box(
                                    modifier = Modifier.size(70.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBiometricEnabled && isBiometricAvailable) {
                                        IconButton(
                                            onClick = { showBiometricPrompt() },
                                            modifier = Modifier.size(70.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = stringResource(StringRes.biometric_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
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
    }
}
