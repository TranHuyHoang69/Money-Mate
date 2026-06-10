package com.example.moneymate.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.StringRes       // ✅ Bộ quản lý ID tài nguyên chuỗi tập trung
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.theme.stringResource // ✅ Đã sửa sang import hàm dịch i18n custom sạch crash
import com.example.moneymate.viewmodel.DetailExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val detailExpenseFormatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailExpenseScreen(
    navController: NavController,
    firestoreDocId: String,
    viewModel: DetailExpenseViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Kích hoạt nạp dữ liệu chi tiết từ Firestore ID
    LaunchedEffect(firestoreDocId) {
        viewModel.loadExpenseByFirestoreId(firestoreDocId)
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow.collect { _ ->
            navController.popBackStack()
        }
    }

    val expenseResult by viewModel.expenseState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // --- TOP BAR CUSTOM ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(StringRes.back_btn), // ✅ Sửa lỗi compile nhãn id =
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(StringRes.transaction_detail_title), // ✅ Sửa lỗi compile nhãn id =
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- XỬ LÝ TRẠNG THÁI DỮ LIỆU ---
            when (val result = expenseResult) {
                is Result.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // ✅ Thay đổi màu Loader hệ thống
                    }
                }
                is Result.Success -> {
                    val item = result.data

                    if (item != null) {
                        // Quyết định màu sắc đặc trưng động dựa trên loại giao dịch theo chuẩn M3
                        val spendThemeColor = MaterialTheme.colorScheme.primary
                        val incomeThemeColor = MaterialTheme.colorScheme.tertiary

                        val themeColor = remember(item.type, spendThemeColor, incomeThemeColor) {
                            if (item.type == TransactionType.SPEND) spendThemeColor else incomeThemeColor
                        }

                        // ✅ i18n AN TOÀN: Đọc chuỗi thông qua context.getString trong Composable
                        val formattedAmount = remember(item.amount) {
                            "${String.format("%,.0f", item.amount)} ${context.getString(StringRes.currency_unit)}"
                        }
                        val formattedTime = remember(item.timestamp) {
                            detailExpenseFormatter.format(Date(item.timestamp))
                        }

                        // ✅ i18n: Ánh xạ loại giao dịch động bằng chuỗi tài nguyên (Sửa lỗi nhãn id =)
                        val transactionTypeLabel = stringResource(
                            if (item.type == TransactionType.SPEND) StringRes.type_spend else StringRes.type_income
                        )

                        // ✅ i18n: Xử lý chuỗi ghi chú trống mặc định (Sửa lỗi nhãn id =)
                        val noteValue = item.note.ifEmpty { stringResource(StringRes.no_note_placeholder) }

                        // Lưới danh sách chi tiết chi tiêu
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item { DetailRow(stringResource(StringRes.label_amount), formattedAmount, valueColor = themeColor) }
                            item { DetailRow(stringResource(StringRes.label_category), item.category.title) }
                            item { DetailRow(stringResource(StringRes.label_type), transactionTypeLabel) }
                            item { DetailRow(stringResource(StringRes.label_time), formattedTime) }
                            item { DetailRow(stringResource(StringRes.label_note), noteValue) }
                        }

                        // --- HÀNG NÚT BẤM HÀNH ĐỘNG ---
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (firestoreDocId.isNotEmpty()) {
                                        navController.navigate("update_screen/$firestoreDocId")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                            ) {
                                Text(
                                    text = stringResource(StringRes.edit_btn), // ✅ Sửa lỗi compile nhãn id =
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.deleteExpense(item)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // ✅ Thay thế mã đỏ nạp cứng sang màu Error hệ thống
                            ) {
                                Text(
                                    text = stringResource(StringRes.delete_btn), // ✅ Sửa lỗi compile nhãn id =
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(StringRes.transaction_not_found_error), // ✅ Sửa lỗi compile nhãn id =
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${stringResource(StringRes.error)}: ${result.message}", // ✅ Sửa lỗi compile nhãn id =
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}