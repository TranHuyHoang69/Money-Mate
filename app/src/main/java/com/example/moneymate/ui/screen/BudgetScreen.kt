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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymate.StringRes
import com.example.moneymate.domain.model.BudgetProgress
import com.example.moneymate.domain.model.Category
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.BudgetViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val budgetCurrencyFormatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = '.'
})

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BudgetProgress?>(null) }

    if (showAddDialog) {
        BudgetEditorDialog(
            categories = state.categories,
            onDismiss = { showAddDialog = false },
            onSave = { category, amount ->
                viewModel.saveBudget(category, amount)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { progress ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa ngân sách?") },
            text = {
                Text("Ngân sách ${progress.budget.categoryTitle} của tháng này sẽ bị xóa.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBudget(progress)
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm ngân sách",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BudgetHeader(onOpenDrawer = onOpenDrawer)

            MonthSwitcher(
                displayMonth = state.displayMonth,
                onPrevious = viewModel::movePreviousMonth,
                onNext = viewModel::moveNextMonth
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val totalBudget = state.budgets.sumOf { it.budget.amount }
                val totalSpent = state.budgets.sumOf { it.spentAmount }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BudgetSummaryCard(
                            totalBudget = totalBudget,
                            totalSpent = totalSpent
                        )
                    }

                    if (state.budgets.isEmpty()) {
                        item {
                            EmptyBudgetState()
                        }
                    } else {
                        items(
                            items = state.budgets,
                            key = { it.budget.id }
                        ) { progress ->
                            BudgetProgressCard(
                                progress = progress,
                                onDelete = { pendingDelete = progress }
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
private fun BudgetHeader(onOpenDrawer: () -> Unit) {
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
                text = "Ngân sách",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MonthSwitcher(
    displayMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = displayMonth,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double
) {
    val remaining = totalBudget - totalSpent
    val isExceeded = remaining < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tổng quan tháng",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            BudgetSummaryRow("Tổng ngân sách", totalBudget, MaterialTheme.colorScheme.primary)
            BudgetSummaryRow("Đã chi", totalSpent, MaterialTheme.colorScheme.error)
            BudgetSummaryRow(
                label = if (isExceeded) "Vượt ngân sách" else "Còn lại",
                amount = kotlin.math.abs(remaining),
                color = if (isExceeded) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun BudgetSummaryRow(
    label: String,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(
            text = formatBudgetCurrency(amount),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun BudgetProgressCard(
    progress: BudgetProgress,
    onDelete: () -> Unit
) {
    val budget = progress.budget
    val progressValue = progress.progress.coerceIn(0f, 1f)
    val progressColor = when {
        progress.isExceeded -> MaterialTheme.colorScheme.error
        progress.progress >= 0.8f -> Color(0xFFFF9800)
        else -> Color(0xFF2E7D32)
    }
    val categoryColor = remember(budget.categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(budget.categoryColorHex))
        } catch (e: Exception) {
            Color(0xFF4CB080)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(42.dp)
                        .background(categoryColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.categoryTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatBudgetCurrency(progress.spentAmount)} / ${formatBudgetCurrency(budget.amount)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa ngân sách",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val remainingText = if (progress.isExceeded) {
                "Vượt ${formatBudgetCurrency(kotlin.math.abs(progress.remainingAmount))}"
            } else {
                "Còn lại ${formatBudgetCurrency(progress.remainingAmount)}"
            }
            Text(
                text = "${(progress.progress * 100).toInt()}% - $remainingText",
                fontSize = 13.sp,
                color = progressColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyBudgetState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Chưa có ngân sách nào cho tháng này",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category, String) -> Unit
) {
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Thêm ngân sách",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Số tiền ngân sách") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Danh mục",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        CategoryBudgetOption(
                            category = category,
                            isSelected = isSelected,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val category = selectedCategory ?: return@Button
                    onSave(category, amountText)
                },
                enabled = selectedCategory != null && amountText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun CategoryBudgetOption(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex))
        } catch (e: Exception) {
            Color(0xFF4CB080)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(categoryColor, RoundedCornerShape(100.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = category.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatBudgetCurrency(amount: Double): String {
    return "${budgetCurrencyFormatter.format(amount)} đ"
}
