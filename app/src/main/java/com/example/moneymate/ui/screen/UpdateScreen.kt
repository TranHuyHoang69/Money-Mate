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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.viewmodel.AddExpenseEvent
import com.example.moneymate.viewmodel.AddExpenseViewModel

@Composable
fun UpdateScreen(
    navController: NavController,
    expenseId: Long = -1L,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    LaunchedEffect(expenseId) {
        if (expenseId != -1L) {
            viewModel.onEvent(AddExpenseEvent.LoadDetails(expenseId))
        }
    }

    val state = viewModel.uiState

    // Tối ưu hóa việc chọn màu chủ đạo, chỉ cập nhật khi Type thay đổi thực sự
    val themeColor = remember(state.selectedType) {
        if (state.selectedType == TransactionType.SPEND) Color(0xFF4B8361) else Color(0xFF2E5B8B)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        HeaderUpdate(
            themeColor = themeColor,
            selectedType = state.selectedType,
            onTabSelected = { viewModel.onEvent(AddExpenseEvent.ChangeType(it)) },
            onBack = { navController.popBackStack() },
            isEditMode = expenseId != -1L
        )

        FormSectionUpdate(
            viewModel = viewModel,
            themeColor = themeColor,
            onSuccess = { navController.popBackStack() }
        )
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
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
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
fun FormSectionUpdate(
    viewModel: AddExpenseViewModel,
    themeColor: Color,
    onSuccess: () -> Unit
) {
    val state = viewModel.uiState

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // 1. Ô Nhập số tiền cô lập (Đọc giá trị amount gián tiếp qua Lambda)
        item(contentType = "AmountInput") {
            Text("Số tiền", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            AmountInputUpdateField(
                amount = state.amount,
                themeColor = themeColor,
                onAmountChange = { viewModel.onEvent(AddExpenseEvent.ChangeAmount(it)) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. Phần Danh Mục Giao Dịch
        item(contentType = "CategoryGrid") {
            Text("Danh mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            when (val categoriesResult = state.categories) {
                is Result.Success -> {
                    CategoryOptimizedGrid(
                        categories = categoriesResult.data,
                        themeColor = themeColor,
                        selectedCategory = state.selectedCategory,
                        onCategorySelect = { viewModel.onEvent(AddExpenseEvent.SelectCategory(it)) }
                    )
                }
                is Result.Loading -> {
                    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                        CircularProgressIndicator(color = themeColor)
                    }
                }
                is Result.Error -> {
                    Text("Lỗi: ${categoriesResult.message}", color = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Ô ghi chú độc lập
        item(contentType = "NoteInput") {
            NoteInputField(
                note = state.note,
                onNoteChange = { viewModel.onEvent(AddExpenseEvent.ChangeNote(it)) }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 4. Nút Xác Nhận hành động
        item(contentType = "SubmitButton") {
            Button(
                onClick = { viewModel.onEvent(AddExpenseEvent.Save(onSuccess)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("XÁC NHẬN", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// --- CÁC COMPOSABLE THÀNH PHẦN ĐƯỢC PHÂN RÃ ĐỂ CÔ LẬP TRẠNG THÁI ---

@Composable
fun AmountInputUpdateField(amount: String, themeColor: Color, onAmountChange: (String) -> Unit) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = themeColor),
        placeholder = { Text("0.00", color = Color.LightGray) },
        trailingIcon = { Text("₫", fontWeight = FontWeight.Bold, color = themeColor) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NoteInputField(note: String, onNoteChange: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ghi chú", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Tối ưu hóa Grid danh mục bằng cách chia hàng qua Row thay vì lồng LazyVerticalGrid vào LazyColumn.
 * Cách tiếp cận này loại bỏ việc đo đạc lại layout vô tận, sửa lỗi skip frame triệt để.
 */
@Composable
fun CategoryOptimizedGrid(
    categories: List<Category>,
    themeColor: Color,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit
) {
    val chunkedCategories = remember(categories) { categories.chunked(4) }

    Column(modifier = Modifier.fillMaxWidth()) {
        chunkedCategories.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                rowItems.forEach { category ->
                    val isSelected = selectedCategory?.id == category.id
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryItemUpdate(
                            category = category,
                            themeColor = themeColor,
                            isSelected = isSelected,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }
                // Thêm các khoảng trống giả lập nếu hàng cuối không đủ 4 phần tử để căn chỉnh đều layout
                if (rowItems.size < 4) {
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemUpdate(
    category: Category,
    themeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) themeColor else themeColor.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bookmark, null,
                tint = if (isSelected) Color.White else themeColor
            )
        }
        Text(category.title, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}