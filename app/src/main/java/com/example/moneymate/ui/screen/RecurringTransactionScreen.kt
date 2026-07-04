package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymate.StringRes
import com.example.moneymate.domain.model.Category
import com.example.moneymate.domain.model.RecurringTransaction
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.RecurringTransactionViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

private val recurringAmountFormatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = '.'
})

private val recurringDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private val recurringRepeatOptions = listOf(
    "Hàng ngày",
    "Hàng tuần",
    "Mỗi 2 tuần",
    "Mỗi 4 tuần",
    "Hàng tháng",
    "Mỗi 2 tháng",
    "Hàng quý",
    "Mỗi 6 tháng",
    "Mỗi năm"
)

@Composable
fun RecurringTransactionScreen(
    viewModel: RecurringTransactionViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    LaunchedEffect(state.generatedCount, state.error) {
        if (state.generatedCount > 0) {
            snackbarHostState.showSnackbar("Đã tạo ${state.generatedCount} giao dịch định kỳ đến hạn")
            viewModel.clearMessage()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa giao dịch định kỳ?") },
            text = {
                Text("Template ${transaction.categoryTitle} sẽ bị xóa và không tự tạo giao dịch trong các kỳ sau.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RecurringTransactionHeader(onOpenDrawer = onOpenDrawer)

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.transactions.isEmpty()) {
                        item { EmptyRecurringState() }
                    } else {
                        items(state.transactions, key = { it.id to it.firestoreDocId }) { transaction ->
                            RecurringTransactionCard(
                                transaction = transaction,
                                onEdit = {
                                    onEditClick(transaction.firestoreDocId.ifBlank { transaction.id.toString() })
                                },
                                onDelete = { pendingDelete = transaction },
                                onToggle = { active -> viewModel.toggleActive(transaction, active) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RecurringTransactionHeader(onOpenDrawer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(StringRes.menu_icon_desc),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Giao dịch định kỳ",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionEditorScreen(
    transactionKey: String? = null,
    viewModel: RecurringTransactionViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val transaction = state.transactions.firstOrNull {
        it.firestoreDocId == transactionKey || it.id.toString() == transactionKey
    }
    var initialized by remember(transactionKey) { mutableStateOf(false) }

    var type by remember { mutableStateOf(TransactionType.SPEND) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("Hàng tháng") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var active by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transaction, state.categories) {
        if (!initialized && state.categories.isNotEmpty()) {
            if (transactionKey != null && transaction == null) return@LaunchedEffect
            type = transaction?.type ?: TransactionType.SPEND
            amount = transaction?.amount?.toLong()?.takeIf { it > 0 }?.toString() ?: ""
            note = transaction?.note.orEmpty()
            repeat = transaction?.repeatInterval ?: "Hàng tháng"
            startDate = transaction?.startDate ?: System.currentTimeMillis()
            active = transaction?.isActive ?: true
            selectedCategory = state.categories.firstOrNull { it.id == transaction?.categoryId }
                ?: state.categories.firstOrNull { it.type == type }
            initialized = true
        }
    }

    val filteredCategories = state.categories.filter { it.type == type }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis ?: startDate
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (transactionKey == null) "Thêm giao dịch định kỳ" else "Sửa giao dịch định kỳ",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownField(
                    label = "Loại giao dịch",
                    value = if (type == TransactionType.SPEND) "Chi tiêu" else "Thu nhập",
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chi tiêu") },
                        onClick = {
                            type = TransactionType.SPEND
                            selectedCategory = state.categories.firstOrNull { it.type == TransactionType.SPEND }
                            typeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Thu nhập") },
                        onClick = {
                            type = TransactionType.INCOME
                            selectedCategory = state.categories.firstOrNull { it.type == TransactionType.INCOME }
                            typeExpanded = false
                        }
                    )
                }

                DropdownField(
                    label = "Danh mục",
                    value = selectedCategory?.title ?: "Chưa có danh mục",
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.title) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownField(
                    label = "Chu kỳ",
                    value = repeat,
                    expanded = repeatExpanded,
                    onExpandedChange = { repeatExpanded = it }
                ) {
                    recurringRepeatOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                repeat = option
                                repeatExpanded = false
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    OutlinedTextField(
                        value = recurringDateFormatter.format(startDate),
                        onValueChange = {},
                        label = { Text("Ngày bắt đầu") },
                        readOnly = true,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đang hoạt động", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val category = selectedCategory ?: return@Button
                        viewModel.saveTransaction(
                            existing = transaction,
                            type = type,
                            amountInput = amount,
                            category = category,
                            note = note,
                            repeatInterval = repeat,
                            startDate = startDate,
                            isActive = active,
                            onSaved = onBackClick
                        )
                    },
                    enabled = selectedCategory != null && amount.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Lưu", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EmptyRecurringState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Chưa có giao dịch định kỳ",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Thêm các khoản lặp lại như tiền nhà, lương, thuê bao.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RecurringTransactionCard(
    transaction: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val typeColor = if (transaction.type == TransactionType.INCOME) Color(0xFF168A4A) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.categoryTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${transaction.repeatInterval} • lần tới ${recurringDateFormatter.format(transaction.nextRunAt)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (transaction.isActive) "Bật" else "Tắt",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Switch(checked = transaction.isActive, onCheckedChange = onToggle)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${recurringAmountFormatter.format(transaction.amount)} đ",
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sửa")
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            }
            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTransactionEditorDialog(
    transaction: RecurringTransaction?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String, Category, String, String, Long, Boolean) -> Unit
) {
    var type by remember(transaction) { mutableStateOf(transaction?.type ?: TransactionType.SPEND) }
    var amount by remember(transaction) {
        mutableStateOf(transaction?.amount?.toLong()?.takeIf { it > 0 }?.toString() ?: "")
    }
    var note by remember(transaction) { mutableStateOf(transaction?.note ?: "") }
    var repeat by remember(transaction) { mutableStateOf(transaction?.repeatInterval ?: "Hàng tháng") }
    var startDate by remember(transaction) { mutableStateOf(transaction?.startDate ?: System.currentTimeMillis()) }
    var active by remember(transaction) { mutableStateOf(transaction?.isActive ?: true) }
    var typeExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == type }
    var selectedCategory by remember(transaction, categories, type) {
        mutableStateOf(
            filteredCategories.firstOrNull { it.id == transaction?.categoryId }
                ?: filteredCategories.firstOrNull()
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis ?: startDate
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Thêm giao dịch định kỳ" else "Sửa giao dịch định kỳ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    label = "Loại giao dịch",
                    value = if (type == TransactionType.SPEND) "Chi tiêu" else "Thu nhập",
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chi tiêu") },
                        onClick = {
                            type = TransactionType.SPEND
                            selectedCategory = categories.firstOrNull { it.type == TransactionType.SPEND }
                            typeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Thu nhập") },
                        onClick = {
                            type = TransactionType.INCOME
                            selectedCategory = categories.firstOrNull { it.type == TransactionType.INCOME }
                            typeExpanded = false
                        }
                    )
                }

                DropdownField(
                    label = "Danh mục",
                    value = selectedCategory?.title ?: "Chưa có danh mục",
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.title) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownField(
                    label = "Chu kỳ",
                    value = repeat,
                    expanded = repeatExpanded,
                    onExpandedChange = { repeatExpanded = it }
                ) {
                    recurringRepeatOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                repeat = option
                                repeatExpanded = false
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    OutlinedTextField(
                        value = recurringDateFormatter.format(startDate),
                        onValueChange = {},
                        label = { Text("Ngày bắt đầu") },
                        readOnly = true,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đang hoạt động", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val category = selectedCategory ?: return@Button
                    onSave(type, amount, category, note, repeat, startDate, active)
                },
                enabled = selectedCategory != null && amount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            content()
        }
    }
}
