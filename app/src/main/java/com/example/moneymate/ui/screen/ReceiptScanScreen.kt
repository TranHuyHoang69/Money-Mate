package com.example.moneymate.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymate.domain.model.ReceiptScanResult
import com.example.moneymate.viewmodel.ReceiptScanViewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    onBackClick: () -> Unit,
    onUseResult: (ReceiptScanResult) -> Unit,
    viewModel: ReceiptScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var permissionError by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        permissionError = ""
        viewModel.scanReceipt(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            permissionError = ""
            viewModel.scanReceipt(pendingCameraUri)
        } else {
            permissionError = "Bạn chưa chụp ảnh hóa đơn"
        }
    }

    fun launchCamera() {
        val uri = createReceiptImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            permissionError = "Bạn cần cấp quyền camera để chụp hóa đơn"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quét hóa đơn",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Chọn ảnh có sẵn hoặc chụp hóa đơn để OCR đọc nội dung và tự parse thông tin giao dịch.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    launchCamera()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chụp ảnh")
                        }

                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImageSearch,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chọn ảnh")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            permissionError = ""
                            viewModel.clearResult()
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Xóa kết quả")
                    }
                }
            }

            ReceiptImagePreview(imageUri = uiState.selectedImageUri)

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Đang quét hóa đơn...")
                }
            }

            if (permissionError.isNotBlank()) {
                Text(
                    text = permissionError,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            if (uiState.error.isNotBlank()) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            uiState.parsedResult?.let { result ->
                var selectedAmount by remember(result.rawText, result.amountCandidates) {
                    mutableStateOf(result.amount)
                }
                ReceiptParsedResultCard(
                    amount = selectedAmount,
                    amountCandidates = result.amountCandidates,
                    dateMillis = result.dateMillis,
                    merchantName = result.merchantName,
                    suggestedCategoryTitle = result.suggestedCategoryTitle,
                    note = result.note,
                    confidence = result.confidence,
                    isLearningApplied = result.isLearningApplied,
                    isAiCategorySuggested = result.isAiCategorySuggested,
                    amountWarning = result.amountWarning,
                    onAmountSelected = { selectedAmount = it },
                    onForgetLearning = { viewModel.forgetLearningForCurrentResult() },
                    onUseResult = {
                        val finalResult = result.copy(amount = selectedAmount)
                        viewModel.saveUserSelection(finalResult)
                        onUseResult(finalResult)
                    }
                )
            }

            if (uiState.rawText.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Raw OCR text",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.rawText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptImagePreview(imageUri: Uri?) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        bitmap = imageUri?.let { loadBitmapFromUri(context, it) }
    }

    val imageBitmap = bitmap ?: return

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            bitmap = imageBitmap.asImageBitmap(),
            contentDescription = "Ảnh hóa đơn đã chọn",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ReceiptParsedResultCard(
    amount: Double?,
    amountCandidates: List<Double>,
    dateMillis: Long?,
    merchantName: String?,
    suggestedCategoryTitle: String,
    note: String,
    confidence: Float,
    isLearningApplied: Boolean,
    isAiCategorySuggested: Boolean,
    amountWarning: String?,
    onAmountSelected: (Double) -> Unit,
    onForgetLearning: () -> Unit,
    onUseResult: () -> Unit
) {
    val vietnameseLocale = Locale.forLanguageTag("vi-VN")
    val currencyFormat = NumberFormat.getCurrencyInstance(vietnameseLocale)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", vietnameseLocale)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kết quả parse",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ReceiptResultRow("Số tiền", amount?.let { currencyFormat.format(it) } ?: "Chưa nhận diện")
            if (amountCandidates.isNotEmpty()) {
                Text(
                    text = "Chọn lại số tiền nếu OCR nhận sai",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    amountCandidates.forEach { candidate ->
                        val isSelected = candidate == amount
                        OutlinedButton(
                            onClick = { onAmountSelected(candidate) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSelected) {
                                    "Đề xuất: ${currencyFormat.format(candidate)}"
                                } else {
                                    currencyFormat.format(candidate)
                                }
                            )
                        }
                    }
                }
            }
            if (!amountWarning.isNullOrBlank()) {
                Text(
                    text = amountWarning,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
            ReceiptResultRow("Ngày", dateMillis?.let { dateFormat.format(Date(it)) } ?: "Chưa nhận diện")
            ReceiptResultRow("Cửa hàng", merchantName ?: "Chưa nhận diện")
            ReceiptResultRow("Danh mục gợi ý", suggestedCategoryTitle)
            if (isAiCategorySuggested && !isLearningApplied) {
                Text(
                    text = "Danh mục được Gemini gợi ý từ nội dung OCR.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            ReceiptResultRow("Ghi chú", note)
            ReceiptResultRow("Độ tin cậy", "${(confidence * 100).toInt()}%")
            if (isLearningApplied) {
                Text(
                    text = "Đã ưu tiên kết quả bạn từng chọn cho mẫu hóa đơn này.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onForgetLearning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bỏ mẫu đã học")
                }
            }
            if (confidence < LOW_CONFIDENCE_THRESHOLD) {
                Text(
                    text = "Độ tin cậy thấp. Hãy kiểm tra lại số tiền, ngày và danh mục trước khi dùng kết quả.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUseResult,
                modifier = Modifier.fillMaxWidth(),
                enabled = amount != null
            ) {
                Text("Dùng kết quả này")
            }
        }
    }
}

@Composable
private fun ReceiptResultRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun createReceiptImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "receipt_images").apply {
        mkdirs()
    }
    val imageFile = File.createTempFile(
        "receipt_${System.currentTimeMillis()}_",
        ".jpg",
        imagesDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    }.getOrNull()
}

private const val LOW_CONFIDENCE_THRESHOLD = 0.75f
