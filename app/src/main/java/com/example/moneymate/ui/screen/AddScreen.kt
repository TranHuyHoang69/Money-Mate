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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddScreen(
    navController: NavController,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
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

        FormSection(
            viewModel = viewModel,
            themeColor = themeColor,
            categoriesState = categoriesState,
            navController = navController,
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
    val options = listOf("SPEND" to "CHI PHÍ", "INCOME" to "THU NHẬP")
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
            options.forEach { (logicName, displayName) ->
                val isSelected = selectedType == logicName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(0.25f) else Color.Transparent)
                        .clickable { onTabSelected(logicName) }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = displayName,
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
    navController: NavController,
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
                trailingIcon = { Text("₫", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColor) },
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

            // SỬA LỖI: Sử dụng 'val state' để gán giá trị và smart-cast thành công
            when (val state = categoriesState) {
                is Result.Success -> {
                    CategoryGrid(
                        categories = state.data,
                        themeColor = themeColor,
                        selectedCategory = viewModel.selectedCategory,
                        onCategorySelect = { viewModel.selectedCategory = it },
                        onSeeMoreClick = {
                            val encodedType = URLEncoder.encode(viewModel.selectedType, "UTF-8")
                            navController.navigate("category_management/$encodedType") // Nhảy vào màn quản lý trước
                        }
                    )
                }
                is Result.Loading -> {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = themeColor)
                    }
                }
                is Result.Error -> {
                    // SỬA LỖI: Dùng state.message theo đúng file Result.kt của bạn
                    Text("Lỗi: ${state.message}", color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                                text = formatLongToDateString(viewModel.selectedDate), // Dùng tên hàm mới
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = themeColor)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
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
    onCategorySelect: (Category) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    // Sửa lỗi: Sử dụng Chiều cao cố định hoặc Modifier.heightIn
    Column(modifier = Modifier.fillMaxWidth()) {
        val displayItems = categories.take(7)

        // Chia item thành các hàng (mỗi hàng 4 cái)
        val rows = displayItems.chunked(4)

        rows.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        CategoryItem(
                            category = category,
                            themeColor = try { Color(android.graphics.Color.parseColor(category.colorHex)) } catch (e: Exception) { themeColor },
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }
                // Nếu hàng đầu tiên chưa đủ 4 cái, thêm nút "Cài đặt" vào cuối hàng đó
                if (rowItems.size < 4) {
                    Box(modifier = Modifier.weight(1f)) { AddCategoryButton(onSeeMoreClick) }
                    // Fill nốt khoảng trống nếu vẫn thiếu
                    repeat(4 - rowItems.size - 1) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Nếu hàng cuối cùng đã đủ 4, thì nút "Cài đặt" nằm ở hàng mới
        if (displayItems.size % 4 == 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { AddCategoryButton(onSeeMoreClick) }
                repeat(3) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AddCategoryButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp).clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(Color.LightGray.copy(0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = Color.Gray)
        }
        Text("Xem thêm", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun CategoryItem(
    category: Category,
    themeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resId = remember(category.iconResName) {
        context.resources.getIdentifier(category.iconResName, "drawable", context.packageName)
    }

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
            if (resId != 0) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else themeColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = if (isSelected) Color.White else themeColor)
            }
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

// SỬA LỖI: Đổi tên hàm thành formatLongToDateString để tránh trùng lặp
fun formatLongToDateString(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Sai định dạng"
    }
}

fun showDatePicker(context: Context, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    android.app.DatePickerDialog(
        context,
        { _, y, m, d ->
            val res = Calendar.getInstance()
            res.set(y, m, d)
            onDateSelected(res.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
        show()
    }
}