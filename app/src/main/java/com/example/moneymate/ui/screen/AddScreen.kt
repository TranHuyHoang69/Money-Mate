package com.example.moneymate.ui.screen

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource // Sử dụng chuẩn hàm dịch i18n
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
import com.example.moneymate.StringRes
import com.example.moneymate.domain.Result
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.navigation.HomeNavKeys
import com.example.moneymate.ui.navigation.ReceiptScanNavKeys
import com.example.moneymate.ui.navigation.Screen
import com.example.moneymate.ui.theme.AppTopBarColor
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

    // ✅ ĐÃ SỬA: Ép cứng màu xanh thương hiệu cố định cho thanh Header, giống hoàn toàn với UpdateScreen
    val themeColor = AppTopBarColor
    val context = LocalContext.current

    // Luồng lắng nghe sự kiện Save thành công
    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow.collect { event ->
            android.util.Log.d("MONEYMATE_DEBUG", "Đã nhận sự kiện trên UI: $event")
            when(event){
                is AddExpenseUiEvent.SaveSuccess -> {
                    event.timestamp?.let { savedTimestamp ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(HomeNavKeys.SAVED_TRANSACTION_DATE_MILLIS, savedTimestamp)
                    }
                    navController.popBackStack()
                }
                is AddExpenseUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Luồng lắng nghe dữ liệu danh mục truyền ngược về thông qua SavedStateHandle
    val navBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val receiptAmountState = savedStateHandle
        ?.getStateFlow<String?>(ReceiptScanNavKeys.AMOUNT, null)
        ?.collectAsState()
    val receiptDateMillisState = savedStateHandle
        ?.getStateFlow<Long?>(ReceiptScanNavKeys.DATE_MILLIS, null)
        ?.collectAsState()
    val receiptNoteState = savedStateHandle
        ?.getStateFlow<String?>(ReceiptScanNavKeys.NOTE, null)
        ?.collectAsState()
    val receiptCategoryTitleState = savedStateHandle
        ?.getStateFlow<String?>(ReceiptScanNavKeys.CATEGORY_TITLE, null)
        ?.collectAsState()

    LaunchedEffect(
        navBackStackEntry,
        receiptAmountState?.value,
        receiptDateMillisState?.value,
        receiptNoteState?.value,
        receiptCategoryTitleState?.value
    ) {
        val categoryId = savedStateHandle?.get<Long>("selected_category_id")

        if (categoryId != null) {
            val stableId = savedStateHandle.get<String>("selected_category_stable_id").orEmpty()
            val title = savedStateHandle.get<String>("selected_category_title").orEmpty()
            val iconResName = savedStateHandle.get<String>("selected_category_icon").orEmpty()
            val colorHex = savedStateHandle.get<String>("selected_category_color").orEmpty()

            val returnedCategory = Category(
                id = categoryId,
                stableId = stableId,
                title = title,
                iconResName = iconResName,
                colorHex = colorHex,
                type = state.selectedType,
                isDefault = false
            )

            viewModel.onEvent(AddExpenseEvent.SelectCategory(returnedCategory))

            savedStateHandle.remove<Long>("selected_category_id")
            savedStateHandle.remove<String>("selected_category_stable_id")
            savedStateHandle.remove<String>("selected_category_title")
            savedStateHandle.remove<String>("selected_category_icon")
            savedStateHandle.remove<String>("selected_category_color")
        }

        val receiptAmount = receiptAmountState?.value
        val receiptDateMillis = receiptDateMillisState?.value
        val receiptNote = receiptNoteState?.value
        val receiptCategoryTitle = receiptCategoryTitleState?.value

        if (receiptAmount != null || receiptDateMillis != null || receiptNote != null || receiptCategoryTitle != null) {
            viewModel.onEvent(
                AddExpenseEvent.ApplyReceiptScanResult(
                    amount = receiptAmount,
                    dateMillis = receiptDateMillis,
                    note = receiptNote,
                    categoryTitle = receiptCategoryTitle
                )
            )

            savedStateHandle?.remove<String>(ReceiptScanNavKeys.AMOUNT)
            savedStateHandle?.remove<Long>(ReceiptScanNavKeys.DATE_MILLIS)
            savedStateHandle?.remove<String>(ReceiptScanNavKeys.NOTE)
            savedStateHandle?.remove<String>(ReceiptScanNavKeys.CATEGORY_TITLE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
    // ✅ ĐÃ SỬA: Đưa việc giải mã chuỗi i18n ra scope Composable an toàn trước khi nạp vào vòng lặp
    val expensesText = stringResource(id = StringRes.expenses).uppercase()
    val incomeText = stringResource(id = StringRes.income).uppercase()
    val backDescText = stringResource(id = StringRes.back_btn)
    val addTransactionText = stringResource(id = StringRes.add_transaction)

    val options = remember(expensesText, incomeText) {
        listOf(
            TransactionType.SPEND to expensesText,
            TransactionType.INCOME to incomeText
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(themeColor)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backDescText,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = addTransactionText,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
            options.forEach { (type, tabTitle) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(0.25f) else Color.Transparent)
                        .clickable { onTabSelected(type) }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tabTitle,
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

    // ✅ ĐÃ SỬA: Đưa toàn bộ các hàm stringResource lên đầu scope FormSection để bảo vệ tính đồng nhất của Context
    val amountLabel = stringResource(id = StringRes.amount_label)
    val categoryManagementLabel = stringResource(id = StringRes.category_management)
    val confirmBtnText = stringResource(id = StringRes.confirm_btn).uppercase()
    val viewMoreText = stringResource(id = StringRes.view_more)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // 1. Phần nhập tiền
        item(key = "amount_input") {
            Text(text = amountLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            AmountInputField(
                amount = state.amount,
                amountError = state.amountError,
                themeColor = themeColor,
                onAmountChange = { viewModel.onEvent(AddExpenseEvent.ChangeAmount(it)) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. Phần danh mục
        item(key = "receipt_scan_button") {
            Button(
                onClick = { navController.navigate(Screen.ReceiptScan.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor.copy(alpha = 0.12f),
                    contentColor = themeColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ImageSearch,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quét hóa đơn",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "category_section") {
            Text(text = categoryManagementLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            when (val categoriesResult = state.categories) {
                is Result.Success -> {
                    CategoryGrid(
                        categories = categoriesResult.data,
                        themeColor = themeColor,
                        selectedCategory = state.selectedCategory,
                        viewMoreText = viewMoreText, // Truyền chuỗi an toàn xuống dưới
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

        // 3. Các phần còn lại (Ngày tháng, Ghi chú, Nút lưu)
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
                enabled = state.isAmountValid && state.selectedCategory != null
            ) {
                Text(text = confirmBtnText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    themeColor: Color,
    selectedCategory: Category?,
    viewMoreText: String, // ✅ Đã sửa: Nhận String thuần từ trên truyền xuống
    onCategorySelect: (Category) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    val gridRows = remember(categories, selectedCategory) {
        val top7DefaultCategories = categories.take(7)

        val finalDisplayList = if (selectedCategory != null) {
            val isAlreadyInTop7 = top7DefaultCategories.any { it.id == selectedCategory.id }

            if (isAlreadyInTop7) {
                top7DefaultCategories
            } else {
                val remainingItems = top7DefaultCategories.take(6)
                listOf(selectedCategory) + remainingItems
            }
        } else {
            top7DefaultCategories
        }

        finalDisplayList.chunked(4)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        gridRows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                // ✅ ĐÃ SỬA: Căn các phần tử trong hàng thẳng hàng theo trục ngang
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                rowItems.forEach { category ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp), // Thêm chút padding đồng bộ nếu cần
                        contentAlignment = Alignment.Center // Giúp nội dung trong Box cân bằng
                    ) {
                        CategoryItem(
                            category = category,
                            themeColor = MaterialTheme.colorScheme.onSurface,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }

                // Trường hợp hàng cuối cùng chưa đủ 4 phần tử (Thêm nút Xem thêm vào chỗ trống)
                if (rowIndex == gridRows.lastIndex && rowItems.size < 4) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center // ✅ ĐÃ SỬA: Căn nút vào giữa Box
                    ) {
                        AddCategoryButton(viewMoreText, onSeeMoreClick)
                    }
                    // Giữ khoảng trống đều cho các ô còn lại
                    repeat(4 - rowItems.size - 1) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Trường hợp số lượng item chia hết cho 4 (Nút Xem thêm nằm riêng một hàng mới)
        val totalTaken = gridRows.flatten().size
        if (totalTaken > 0 && totalTaken % 4 == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // ✅ ĐÃ SỬA: Thống nhất trục dọc hàng mới
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center // ✅ ĐÃ SỬA: Căn nút vào giữa Box
                ) {
                    AddCategoryButton(viewMoreText, onSeeMoreClick)
                }
                repeat(3) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun Modifier.key(key: Any): Modifier = this

@Composable
fun AddCategoryButton(viewMoreText: String, onClick: () -> Unit) {
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
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = viewMoreText, fontSize = 11.sp, color = Color.Gray)
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
            color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Hàm format ngày (giữ nguyên tính Composable hợp lệ)
@Composable
fun formatLongToDateString(timestamp: Long): String {
    if (timestamp == 0L) return stringResource(id = StringRes.select_date_hint)
    return try {
        addScreenDateFormatter.format(Date(timestamp))
    } catch (e: Exception) {
        stringResource(id = StringRes.error_format)
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

@Composable
fun AmountInputField(
    amount: String,
    amountError: String?,
    themeColor: Color,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = themeColor),
        placeholder = { Text("0", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        trailingIcon = { Text("₫", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeColor) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = amountError != null,
        supportingText = amountError?.let { errorMessage ->
            { Text(text = errorMessage) }
        },
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
    // ✅ ĐÃ SỬA: Bóc chuỗi i18n của Card lên trên scope an toàn của hàm
    val transactionDateLabel = stringResource(id = StringRes.transaction_date)
    val reminderNoteLabel = stringResource(id = StringRes.reminder_note)
    val reminderNoteHint = stringResource(id = StringRes.reminder_note_hint)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
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
                    Text(text = transactionDateLabel, color = Color.Gray, fontSize = 14.sp)
                    Text(text = formatLongToDateString(selectedDate), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(Icons.Default.DateRange, null, tint = themeColor)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

            Text(text = reminderNoteLabel, color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text(text = reminderNoteHint) },
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
