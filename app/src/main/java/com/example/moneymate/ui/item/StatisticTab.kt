package com.example.moneymate.ui.item

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.domain.model.TransactionType
import com.example.moneymate.ui.chart.CategoryColumnChart
import com.example.moneymate.ui.chart.OverviewColumnChart
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.CategoryStatistics
import com.example.moneymate.viewmodel.StatisticsUiState
import com.example.moneymate.viewmodel.StatisticsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * TAB CHUNG - Hiển thị tổng quan các chỉ số thu chi
 */
@Composable
fun OverviewTabContent(
    statistics: StatisticsUiState.Success,
    navController: NavController,
    viewModel: StatisticsViewModel
) {
    val totalIncomeText = stringResource(StringRes.statistics_total_income)
    val totalExpenseText = stringResource(StringRes.statistics_total_expense)
    val profitText = stringResource(StringRes.statistics_profit)
    val lossText = stringResource(StringRes.statistics_loss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OverviewColumnChart(
            statistics = statistics.overview,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SummaryInfoCard(
            title = totalIncomeText,
            amount = statistics.overview.income,
            color = Color(0xFF4CAF50),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SummaryInfoCard(
            title = totalExpenseText,
            amount = statistics.overview.expense,
            color = Color(0xFFF44336),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SummaryInfoCard(
            title = profitText,
            amount = statistics.overview.profit,
            color = Color(0xFF2196F3),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SummaryInfoCard(
            title = lossText,
            amount = statistics.overview.loss,
            color = Color(0xFFFF9800),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * TAB CHI PHÍ - Hiển thị biểu đồ theo danh mục chi phí
 */
@Composable
fun ExpenseTabContent(
    statistics: StatisticsUiState.Success,
    navController: NavController,
    viewModel: StatisticsViewModel
) {
    val categoryDetailText = stringResource(StringRes.statistics_category_detail)
    val noExpenseText = stringResource(StringRes.statistics_no_expense)
    val totalExpenseText = stringResource(StringRes.statistics_total_expense)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (statistics.expenseByCategory.isEmpty()) {
            EmptyStateMessage(text = noExpenseText)
        } else {
            CategoryColumnChart(
                categories = statistics.expenseByCategory,
                onCategoryClick = { categoryId ->
                    statistics.expenseByCategory
                        .firstOrNull { it.category.id == categoryId }
                        ?.let {
                            openCategoryTransactions(
                                navController = navController,
                                viewModel = viewModel,
                                statistics = statistics,
                                category = it,
                                type = TransactionType.SPEND
                            )
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = categoryDetailText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            statistics.expenseByCategory.forEach { category ->
                CategoryInfoItem(
                    categoryName = category.category.title,
                    totalAmount = category.totalAmount,
                    percentage = category.percentage,
                    transactionCount = category.transactionCount,
                    categoryColor = try {
                        Color(android.graphics.Color.parseColor(category.category.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF9C27B0)
                    },
                    onClick = {
                        openCategoryTransactions(
                            navController = navController,
                            viewModel = viewModel,
                            statistics = statistics,
                            category = category,
                            type = TransactionType.SPEND
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TotalAmountCard(
                title = totalExpenseText,
                amount = statistics.expenseByCategory.sumOf { it.totalAmount },
                color = Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * TAB THU NHẬP - Hiển thị biểu đồ theo danh mục thu nhập
 */
@Composable
fun IncomeTabContent(
    statistics: StatisticsUiState.Success,
    navController: NavController,
    viewModel: StatisticsViewModel
) {
    val categoryDetailText = stringResource(StringRes.statistics_category_detail)
    val noIncomeText = stringResource(StringRes.statistics_no_income)
    val totalIncomeText = stringResource(StringRes.statistics_total_income)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (statistics.incomeByCategory.isEmpty()) {
            EmptyStateMessage(text = noIncomeText)
        } else {
            CategoryColumnChart(
                categories = statistics.incomeByCategory,
                onCategoryClick = { categoryId ->
                    statistics.incomeByCategory
                        .firstOrNull { it.category.id == categoryId }
                        ?.let {
                            openCategoryTransactions(
                                navController = navController,
                                viewModel = viewModel,
                                statistics = statistics,
                                category = it,
                                type = TransactionType.INCOME
                            )
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = categoryDetailText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            statistics.incomeByCategory.forEach { category ->
                CategoryInfoItem(
                    categoryName = category.category.title,
                    totalAmount = category.totalAmount,
                    percentage = category.percentage,
                    transactionCount = category.transactionCount,
                    categoryColor = try {
                        Color(android.graphics.Color.parseColor(category.category.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF4CAF50)
                    },
                    onClick = {
                        openCategoryTransactions(
                            navController = navController,
                            viewModel = viewModel,
                            statistics = statistics,
                            category = category,
                            type = TransactionType.INCOME
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TotalAmountCard(
                title = totalIncomeText,
                amount = statistics.incomeByCategory.sumOf { it.totalAmount },
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Thẻ hiển thị thông tin chỉ số tổng hợp
 */
private fun openCategoryTransactions(
    navController: NavController,
    viewModel: StatisticsViewModel,
    statistics: StatisticsUiState.Success,
    category: CategoryStatistics,
    type: TransactionType
) {
    val categoryTransactions = statistics.allExpenses.filter {
        it.category.id == category.category.id && it.type == type
    }

    if (categoryTransactions.size == 1) {
        val docId = categoryTransactions.first().firestoreDocId
        if (docId.isNotEmpty()) {
            navController.navigate("detail_expense/$docId")
            return
        }
    }

    val (start, end) = viewModel.getCurrentPeriodRange()
    val encodedCategoryName = Uri.encode(category.category.title)
    navController.navigate(
        "grouped_expense/$encodedCategoryName/${category.totalAmount.toFloat()}/${statistics.timeMode.name}/$start/$end"
    )
}

@Composable
private fun SummaryInfoCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatCurrency(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Thành phần hiển thị chi tiết cho từng dòng danh mục
 */
@Composable
private fun CategoryInfoItem(
    categoryName: String,
    totalAmount: Double,
    percentage: Float,
    transactionCount: Int,
    categoryColor: Color,
    onClick: () -> Unit
) {
    val transactionCountFormat = stringResource(
        StringRes.statistics_transactions_count,
        transactionCount
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(categoryColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$transactionCountFormat • ${"%.1f".format(percentage)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatCurrency(totalAmount),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Thẻ hiển thị tổng tiền làm nổi bật cuối màn hình
 */
@Composable
private fun TotalAmountCard(
    title: String,
    amount: Double,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatCurrency(amount),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Trạng thái trống khi không tìm thấy bản ghi giao dịch nào
 */
@Composable
private fun EmptyStateMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )
    }
}

/**
 * Định dạng tiền tệ VND đồng bộ chuẩn hóa hệ thống
 */
private fun formatCurrency(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,###", symbols)
    return "${formatter.format(amount)}đ"
}
