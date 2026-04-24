package com.example.moneymate.ui.screen

import android.content.Context
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.viewmodel.AddExpenseViewModel
import java.util.Calendar

@Composable
fun AddScreen(
    navController: NavController,
    viewModel: AddExpenseViewModel = hiltViewModel() // Tích hợp ViewModel
) {
    // Lấy trạng thái dữ liệu từ ViewModel
    val categoriesState by viewModel.categories.collectAsState()
    val themeColor = if (viewModel.selectedType == "CHI PHÍ") Color(0xFF4B8361) else Color(0xFF2E5B8B)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        HeaderAdd(
            themeColor = themeColor,
            selectedType = viewModel.selectedType,
            onTabSelected = { viewModel.selectedType = it },
            onBack = { navController.popBackStack() }
        )

        // Truyền ViewModel và CategoriesState xuống Form
        FormSection(
            viewModel = viewModel,
            themeColor = themeColor,
            categoriesState = categoriesState,
            onSuccess = { navController.popBackStack() }
        )
    }
}

@Composable
fun HeaderAdd(
    themeColor: Color,
    selectedType: String,
    onTabSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(themeColor)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Thêm giao dịch", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FormSection(
    viewModel: AddExpenseViewModel,
    themeColor: Color,
    categoriesState: Result<List<Category>>,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Text("Số tiền", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.onAmountChange(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor
                ),
                placeholder = { Text("0.00", fontSize = 28.sp, color = Color.LightGray) },
                trailingIcon = { Text("$", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColor) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = Color.LightGray.copy(0.5f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Danh mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            when (categoriesState) {
                is Result.Success -> {
                    CategoryGrid(
                        categories = categoriesState.data,
                        themeColor = themeColor,
                        selectedCategory = viewModel.selectedCategory,
                        onCategorySelect = { viewModel.selectedCategory = it }
                    )
                }
                is Result.Loading -> {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = themeColor)
                    }
                }
                else -> { Text("Lỗi tải danh mục", color = Color.Red) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // PHẦN CHỌN NGÀY GIAO DỊCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePicker(context) { timestamp ->
                                    viewModel.selectedDate = timestamp
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ngày giao dịch", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTimestamp(viewModel.selectedDate),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = themeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Ghi chú", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = viewModel.note,
                        onValueChange = { viewModel.note = it },
                        placeholder = { Text("Nhập ghi chú...") },
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
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("XÁC NHẬN", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    themeColor: Color,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(180.dp),
        userScrollEnabled = false
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory?.id == category.id
            CategoryItem(
                category = category,
                themeColor = themeColor,
                isSelected = isSelected,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    themeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) themeColor else themeColor.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = if (isSelected) Color.White else themeColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            fontSize = 11.sp,
            color = if (isSelected) themeColor else Color.DarkGray,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}


fun showDatePicker(
    context: Context,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val resultCalendar = Calendar.getInstance()
            resultCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(resultCalendar.timeInMillis)
        },
        year, month, day
    )

    // GIỚI HẠN: Chỉ cho phép chọn từ ngày hiện tại trở về trước
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    datePickerDialog.show()
}