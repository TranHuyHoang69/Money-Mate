package com.example.moneymate.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.viewmodel.DetailExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailExpenseScreen(
    navController: NavController,
    expenseId: Long,
    // Inject DetailExpenseViewModel thay vì HomeViewModel
    viewModel: DetailExpenseViewModel = hiltViewModel()
) {
    // Thu thập trạng thái từ DetailExpenseViewModel
    val expenseResult by viewModel.expenseState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // --- TOP BAR ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ArrowBack,
                null,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
            Spacer(Modifier.width(16.dp))
            Text("Chi tiết giao dịch", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Spacer(Modifier.height(32.dp))

        // --- XỬ LÝ TRẠNG THÁI DỮ LIỆU ---
        when (val result = expenseResult) {
            is Result.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4B8331))
                }
            }
            is Result.Success -> {
                val item = result.data
                if (item != null) {
                    // Hiển thị thông tin khi tìm thấy giao dịch
                    DetailRow("Số tiền", "${String.format("%,.0f", item.amount)} $")
                    DetailRow("Danh mục", item.category.title)
                    DetailRow("Loại", if(item.type == TransactionType.SPEND) "Chi phí" else "Thu nhập")
                    DetailRow("Thời gian", formatTimestamp(item.timestamp))
                    DetailRow("Ghi chú", item.note.ifEmpty { "Không có ghi chú" })

                    Spacer(modifier = Modifier.weight(1f))

                    // --- NÚT BẤM ---
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { navController.navigate("update/${item.id}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5B8B))
                        ) {
                            Text("Sửa")
                        }
                        Button(
                            onClick = {
                                // Gọi hàm xóa từ DetailViewModel
                                viewModel.deleteExpense(item) {
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Xóa")
                        }
                    }
                } else {
                    Text("Không tìm thấy thông tin giao dịch", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
            is Result.Error -> {
                Text("Lỗi: ${result.message}", color = Color.Red, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    return sdf.format(date)
}