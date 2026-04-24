package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.viewmodel.AddExpenseViewModel

@Composable
fun UpdateScreen(
    navController: NavController,
    expenseId: Long = -1L,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    // Tự động load dữ liệu khi vào màn hình
    LaunchedEffect(expenseId) {
        if (expenseId != -1L) {
            viewModel.loadExpenseDetails(expenseId)
        }
    }

    val categoriesState by viewModel.categories.collectAsState()
    val themeColor = if (viewModel.selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        HeaderUpdate(
            themeColor = themeColor,
            selectedType = viewModel.selectedType,
            onTabSelected = { viewModel.selectedType = it },
            onBack = { navController.popBackStack() },
            isEditMode = expenseId != -1L
        )

        FormSectionUpdate(
            viewModel = viewModel,
            themeColor = themeColor,
            categoriesState = categoriesState,
            onSuccess = { navController.popBackStack() }
        )
    }
}

@Composable
fun HeaderUpdate(
    themeColor: Color,
    selectedType: String,
    onTabSelected: (String) -> Unit,
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
            listOf("CHI PHÍ", "THU NHẬP").forEach { title ->
                val isSelected = selectedType == title
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(0.25f) else Color.Transparent)
                        .clickable { onTabSelected(title) }
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
    categoriesState: Result<List<Category>>,
    onSuccess: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Text("Số tiền", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.onAmountChange(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = themeColor),
                placeholder = { Text("0.00", color = Color.LightGray) },
                trailingIcon = { Text("$", fontWeight = FontWeight.Bold, color = themeColor) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Danh mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            when (categoriesState) {
                is Result.Success -> {
                    CategoryGridUpdate(
                        categories = categoriesState.data,
                        themeColor = themeColor,
                        selectedCategory = viewModel.selectedCategory,
                        onCategorySelect = { viewModel.selectedCategory = it }
                    )
                }
                else -> { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phần Ghi chú
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ghi chú", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = viewModel.note,
                        onValueChange = { viewModel.note = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.saveExpense(onSuccess) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("XÁC NHẬN", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CategoryGridUpdate(
    categories: List<Category>,
    themeColor: Color,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit
) {
    // Tính toán chiều cao dựa trên số lượng category để không bị lỗi scroll trong LazyColumn
    val rows = (categories.size + 3) / 4
    val gridHeight = (rows * 100).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.heightIn(max = gridHeight),
        userScrollEnabled = false
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory?.id == category.id
            CategoryItemUpdate(
                category = category,
                themeColor = themeColor,
                isSelected = isSelected,
                onClick = { onCategorySelect(category) }
            )
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
        Text(category.title, fontSize = 10.sp, maxLines = 1)
    }
}