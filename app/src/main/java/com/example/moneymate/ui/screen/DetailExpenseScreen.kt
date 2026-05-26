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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

private val detailExpenseFormatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailExpenseScreen(
    navController: NavController,
    firestoreDocId: String,
    viewModel: DetailExpenseViewModel = hiltViewModel()
) {
    // Kích hoạt nạp dữ liệu chi tiết từ Firestore ID
    LaunchedEffect(firestoreDocId) {
        viewModel.loadExpenseByFirestoreId(firestoreDocId)
    }

    // ✅ ĐÃ THÊM: Lắng nghe sự kiện Xóa thành công từ ViewModel qua Channel để đóng màn hình an toàn,
    // giải quyết dứt điểm lỗi nút Xóa bấm bị trơ do nghẽn luồng lambda.
    // (Lưu ý: Đảm bảo trong DetailExpenseViewModel bạn đã cấu hình eventFlow / UI Event tương tự như Add Screen)
    LaunchedEffect(key1 = Unit) {
        // Giả định ViewModel chi tiết của bạn có kênh eventFlow tương tự, nếu chưa có, hãy bổ sung Channel vào DetailExpenseViewModel nhé!
        viewModel.eventFlow.collect { event ->
            // Khi nhận tín hiệu xoá thành công từ DB/Firestore ngầm, tự động lùi màn hình
            navController.popBackStack()
        }
    }

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
                android.util.Log.d("DetailScreen", "Dữ liệu đang tải (Loading)...")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4B8361))
                }
            }
            is Result.Success -> {
                val item = result.data
                android.util.Log.d("DetailScreen", "Tải thành công! Dữ liệu item: $item")

                if (item != null) {
                    val themeColor = remember(item.type) {
                        if (item.type == TransactionType.SPEND) Color(0xFF4B8361) else Color(0xFF2E5B8B)
                    }
                    val formattedAmount = remember(item.amount) {
                        "${String.format("%,.0f", item.amount)} đ"
                    }
                    val formattedTime = remember(item.timestamp) {
                        detailExpenseFormatter.format(Date(item.timestamp))
                    }
                    val transactionTypeLabel = remember(item.type) {
                        if (item.type == TransactionType.SPEND) "Chi phí" else "Thu nhập"
                    }

                    // Tối ưu hóa giao diện bằng LazyColumn để tránh lỗi lồng cuộn và cô lập hiển thị
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item { DetailRow("Số tiền", formattedAmount, valueColor = themeColor) }
                        item { DetailRow("Danh mục", item.category.title) }
                        item { DetailRow("Loại", transactionTypeLabel) }
                        item { DetailRow("Thời gian", formattedTime) }
                        item { DetailRow("Ghi chú", item.note.ifEmpty { "Không có ghi chú" }) }
                    }

                    // --- HÀNG NÚT BẤM HÀNH ĐỘNG ---
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                // ✅ ĐÃ SỬA: Không check item.id nữa, truyền thẳng chuỗi firestoreDocId sang màn hình sửa
                                if (firestoreDocId.isNotEmpty()) {
                                    navController.navigate("update_screen/$firestoreDocId")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text("Sửa", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // ✅ ĐÃ SỬA: Chỉ ra lệnh xóa thuần túy, việc phản hồi điều hướng giao cho LaunchedEffect gom qua Channel xử lý
                                viewModel.deleteExpense(item)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Xóa", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy thông tin giao dịch", textAlign = TextAlign.Center, color = Color.Gray)
                    }
                }
            }
            is Result.Error -> {
                android.util.Log.e("DetailScreen", "Lỗi tải dữ liệu: ${result.message}")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lỗi: ${result.message}", color = Color.Red, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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