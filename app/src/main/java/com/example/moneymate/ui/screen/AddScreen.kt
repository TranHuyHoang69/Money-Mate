package com.example.moneymate.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Khởi tạo định dạng tĩnh duy nhất né rác bộ nhớ cho RAM (GC Overflow)
private val addScreenDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@Composable
fun AddScreen(
    navController: NavController,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    val themeColor = remember(state.selectedType) {
        if (state.selectedType == TransactionType.SPEND) Color(0xFF4B8361) else Color(0xFF2E5B8B)
    }

    // Luồng lắng nghe sự kiện Save thành công
    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow.collect { event ->
            android.util.Log.d("MONEYMATE_DEBUG", "Đã nhận sự kiện trên UI: $event")
            when(event){
                is AddExpenseUiEvent.SaveSuccess ->{
                    navController.popBackStack()
                }
            }
        }
    }

    // 🟢 ĐÃ ĐỒNG BỘ: Lắng nghe và hứng dữ liệu danh mục truyền ngược về thông qua SavedStateHandle
    val navBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        val savedStateHandle = navBackStackEntry?.savedStateHandle
        val categoryId = savedStateHandle?.get<Long>("selected_category_id")

        if (categoryId != null) {
            val title = savedStateHandle.get<String>("selected_category_title").orEmpty()
            val iconResName = savedStateHandle.get<String>("selected_category_icon").orEmpty()
            val colorHex = savedStateHandle.get<String>("selected_category_color").orEmpty()

            val returnedCategory = Category(
                id = categoryId,
                title = title,
                iconResName = iconResName,
                colorHex = colorHex,
                type = state.selectedType,
                isDefault = false
            )

            // Cập nhật danh mục vừa chọn vào ViewModel của màn AddScreen
            viewModel.onEvent(AddExpenseEvent.SelectCategory(returnedCategory))

            // Xóa dữ liệu trong SavedStateHandle để tránh việc Recompose nhận lại dữ liệu cũ khi cấu hình thay đổi
            savedStateHandle.remove<Long>("selected_category_id")
            savedStateHandle.remove<String>("selected_category_title")
            savedStateHandle.remove<String>("selected_category_icon")
            savedStateHandle.remove<String>("selected_category_color")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        HeaderAdd(
            themeColor = themeColor,
            selectedType = state.selectedType,
            onTabSelected = { viewModel.onEvent(AddExpenseEvent.ChangeType(it)) },
            onBack = { navController.popBackStack() }
        )

        FormSection(
            viewModel = viewModel,
            themeColor = themeColor,
            navController = navController,
            onSuccess = { navController.popBackStack() }
        )
    }
}

@Composable
fun HeaderAdd(
    themeColor: Color,
    selectedType: TransactionType,
    onTabSelected: (TransactionType) -> Unit,
    onBack: () -> Unit
) {
    val options = remember { listOf(TransactionType.SPEND to "CHI PHÍ", TransactionType.INCOME to "THU NHẬP") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(themeColor)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
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
            options.forEach { (type, displayName) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(0.25f) else Color.Transparent)
                        .clickable { onTabSelected(type) }
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
    onSuccess: () -> Unit
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Phần nhập tiền
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

        // Phần danh mục
        item(key = "category_section") {
            Text("Danh mục", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            when (val categoriesResult = state.categories) {
                is Result.Success -> {
                    CategoryGrid(
                        categories = categoriesResult.data,
                        themeColor = themeColor,
                        selectedCategory = state.selectedCategory,
                        onCategorySelect = { viewModel.onEvent(AddExpenseEvent.SelectCategory(it)) },
                        onSeeMoreClick = {
                            val encodedType = URLEncoder.encode(state.selectedType.name, "UTF-8")
                            navController.navigate("category_management/$encodedType")
                        }
                    )
                }
                is Result.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = themeColor)
                    }
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Các phần còn lại (Ngày tháng, Ghi chú, Nút lưu)
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

            Button(
                onClick = { viewModel.onEvent(AddExpenseEvent.Save) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                enabled = state.amount.isNotBlank() && state.selectedCategory != null
            ) {
                Text("XÁC NHẬN", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
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
    val gridRows = remember(categories, selectedCategory) {
        val top7DefaultCategories = categories.take(7)

        val finalDisplayList = if (selectedCategory != null) {
            val isAlreadyInTop7 = top7DefaultCategories.any { it.id == selectedCategory.id }

            if (isAlreadyInTop7) {
                // 👉 Nếu có trong top 7 mặc định: Giữ nguyên vị trí cũ không đảo lên đầu
                top7DefaultCategories
            } else {
                // 👉 Nếu KHÔNG nằm trong top 7: Đưa lên vị trí đầu tiên (Index 0)
                val remainingItems = top7DefaultCategories.take(6)
                listOf(selectedCategory) + remainingItems
            }
        } else {
            top7DefaultCategories
        }

        // Chia mảng thành các dòng, mỗi dòng 4 cột
        finalDisplayList.chunked(4)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        gridRows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                rowItems.forEach { category ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .key(category.id ?: 0L)
                    ) {
                        CategoryItem(
                            category = category,
                            themeColor = themeColor,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }

                // Xử lý dòng cuối cùng chứa nút "Xem thêm" nếu dòng đó chưa đủ 4 phần tử
                if (rowIndex == gridRows.lastIndex && rowItems.size < 4) {
                    Box(modifier = Modifier.weight(1f)) { AddCategoryButton(onSeeMoreClick) }
                    repeat(4 - rowItems.size - 1) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Nếu tổng danh mục lấy ra bằng 4 hoặc 8, nút "Xem thêm" tự nhảy xuống dòng mới riêng biệt
        val totalTaken = gridRows.flatten().size
        if (totalTaken > 0 && totalTaken % 4 == 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { AddCategoryButton(onSeeMoreClick) }
                repeat(3) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun Modifier.key(key: Any): Modifier = this

@Composable
fun AddCategoryButton(onClick: () -> Unit) {
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
                .background(Color.LightGray.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
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

    val categoryColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (e: Exception) { Color.Transparent }
    }.let { parsedColor ->
        if (parsedColor == Color.Transparent) themeColor else parsedColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
            .background(
                if (isSelected) categoryColor.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) categoryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(top = 8.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) categoryColor else categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else categoryColor
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            fontSize = 11.sp,
            color = if (isSelected) categoryColor else Color.DarkGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

fun formatLongToDateString(timestamp: Long): String {
    if (timestamp == 0L) return "Chọn ngày"
    return try {
        addScreenDateFormatter.format(Date(timestamp))
    } catch (e: Exception) { "Sai định dạng" }
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

@Composable
fun AmountInputField(amount: String, themeColor: Color, onAmountChange: (String) -> Unit) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = themeColor),
        placeholder = { Text("0", fontSize = 28.sp, color = Color.LightGray) },
        trailingIcon = { Text("₫", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColor) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
        visualTransformation = ThousandSeparatorTransformation()
    )
}

@Composable
fun TransactionDetailsCard(
    selectedDate: Long,
    note: String,
    themeColor: Color,
    onDateClick: () -> Unit,
    onNoteChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onDateClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ngày giao dịch", color = Color.Gray, fontSize = 14.sp)
                    Text(formatLongToDateString(selectedDate), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.DateRange, null, tint = themeColor)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFEEEEEE))

            Text("Ghi chú", color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("Nhập ghi chú...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
            )
        }
    }
}

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = StringBuilder()
        var count = 0
        for (i in originalText.indices.reversed()) {
            formattedText.append(originalText[i])
            count++
            if (count % 3 == 0 && i != 0) {
                formattedText.append('.')
            }
        }
        val out = formattedText.reverse().toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                var dots = 0
                val length = originalText.length
                for (i in 0 until offset) {
                    val revIdx = length - 1 - i
                    if ((length - revIdx) % 3 == 0 && revIdx != 0) {
                        dots++
                    }
                }
                val realDots = if (offset == length && length % 3 == 0 && length > 0) dots - 1 else dots
                val totalOffset = offset + (out.length - originalText.length) - (dots - realDots)
                return totalOffset.coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = offset
                for (i in 0 until offset) {
                    if (i < out.length && out[i] == '.') {
                        originalOffset--
                    }
                }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}