package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.viewmodel.AddExpenseEvent
import com.example.moneymate.viewmodel.AddExpenseUiEvent
import com.example.moneymate.viewmodel.AddExpenseViewModel

@Composable
fun UpdateScreen(
    navController: NavController,
    firestoreDocId: String,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(firestoreDocId) {
        if (firestoreDocId.isNotEmpty()) {
            viewModel.onEvent(AddExpenseEvent.LoadDetailsByFirestoreId(firestoreDocId))
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AddExpenseUiEvent.SaveSuccess -> {
                    navController.popBackStack()
                }
            }
        }
    }

    val themeColor = remember(state.selectedType) {
        if (state.selectedType == TransactionType.SPEND) Color(0xFF4B8361) else Color(0xFF2E5B8B)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        HeaderUpdate(
            themeColor = themeColor,
            selectedType = state.selectedType,
            onTabSelected = { viewModel.onEvent(AddExpenseEvent.ChangeType(it)) },
            onBack = { navController.popBackStack() },
            isEditMode = firestoreDocId.isNotEmpty()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            // 1. Ô Nhập số tiền (Đồng bộ UI với AddScreen)
            item(key = "amount_input") {
                Text("Số tiền", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                AmountInputField(
                    amount = state.amount,
                    themeColor = themeColor,
                    onAmountChange = { viewModel.onEvent(AddExpenseEvent.ChangeAmount(it)) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Phần Danh Mục Giao Dịch (Hiển thị tối đa 8 item, ẩn nút Xem thêm)
            item(key = "category_section") {
                Text("Danh mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                when (val categoriesResult = state.categories) {
                    is Result.Success -> {
                        CategoryUpdateGrid(
                            categories = categoriesResult.data,
                            themeColor = themeColor,
                            selectedCategory = state.selectedCategory,
                            onCategorySelect = { viewModel.onEvent(AddExpenseEvent.SelectCategory(it)) }
                        )
                    }
                    is Result.Loading -> {
                        Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                            CircularProgressIndicator(color = themeColor)
                        }
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. Khối thông tin bổ sung & duy nhất một nút XÁC NHẬN lớn
            item(key = "additional_details") {
                TransactionDetailsCard(
                    selectedDate = state.selectedDate,
                    note = state.note,
                    themeColor = themeColor,
                    onDateClick = {
                        showDatePicker(context) { viewModel.onEvent(AddExpenseEvent.ChangeDate(it)) }
                    },
                    onNoteChange = { viewModel.onEvent(AddExpenseEvent.ChangeNote(it)) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ✅ ĐÃ SỬA: Loại bỏ hoàn toàn khối Row chia tỉ lệ nút Xóa, đưa nút Xác Nhận về full màn hình giống AddScreen
                Button(
                    onClick = { viewModel.onEvent(AddExpenseEvent.Save) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    enabled = state.amount.isNotBlank() && state.selectedCategory != null
                ) {
                    Text("XÁC NHẬN", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HeaderUpdate(
    themeColor: Color,
    selectedType: TransactionType,
    onTabSelected: (TransactionType) -> Unit,
    onBack: () -> Unit,
    isEditMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(themeColor)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ArrowBack, null, tint = Color.White,
                modifier = Modifier.size(28.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (isEditMode) "Cập nhật giao dịch" else "Thêm giao dịch",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(0.15f))
                .padding(4.dp)
        ) {
            listOf(TransactionType.SPEND to "CHI PHÍ", TransactionType.INCOME to "THU NHẬP").forEach { (type, title) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(0.25f) else Color.Transparent)
                        .clickable { onTabSelected(type) }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color.White.copy(0.6f),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryUpdateGrid(
    categories: List<Category>,
    themeColor: Color,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit
) {
    val gridRows = remember(categories) {
        val displayItems = categories.take(8)
        displayItems.chunked(4)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        gridRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                rowItems.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        CategoryItem(
                            category = category,
                            themeColor = themeColor,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }
                if (rowItems.size < 4) {
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}