package com.example.moneymate.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.moneymate.StringRes
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.CategoryStatistics
import com.example.moneymate.viewmodel.OverviewStatistics
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer

@Composable
fun OverviewColumnChart(
    statistics: OverviewStatistics,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val incomeLabel = stringResource(StringRes.statistics_total_income)
    val expenseLabel = stringResource(StringRes.statistics_total_expense)
    val profitLabel = stringResource(StringRes.statistics_profit)
    val lossLabel = stringResource(StringRes.statistics_loss)

    LaunchedEffect(statistics) {
        modelProducer.runTransaction {
            columnSeries {
                series(statistics.income)
                series(statistics.expense)
                series(statistics.profit)
                series(statistics.loss)
            }
        }
    }

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)
    val profitColor = Color(0xFF2196F3)
    val lossColor = Color(0xFFFF9800)

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        listOf(
                            rememberLineComponent(color = incomeColor, thickness = 20.dp),
                            rememberLineComponent(color = expenseColor, thickness = 20.dp),
                            rememberLineComponent(color = profitColor, thickness = 20.dp),
                            rememberLineComponent(color = lossColor, thickness = 20.dp)
                        )
                    )
                ),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _, _ ->
                        when (value.toInt()) {
                            0 -> incomeLabel
                            1 -> expenseLabel
                            2 -> profitLabel
                            3 -> lossLabel
                            else -> ""
                        }
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
fun CategoryColumnChart(
    categories: List<CategoryStatistics>,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(categories) {
        modelProducer.runTransaction {
            columnSeries {
                categories.forEach { category ->
                    series(category.totalAmount)
                }
            }
        }
    }

    val columnLineComponents = categories.map { category ->
        val parsedColor = try {
            Color(android.graphics.Color.parseColor(category.category.colorHex))
        } catch (e: Exception) {
            Color(0xFF9C27B0)
        }

        rememberLineComponent(
            color = parsedColor,
            thickness = 24.dp
        )
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(columnLineComponents)
                ),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _, _ ->
                        val index = value.toInt()
                        if (index in categories.indices) {
                            categories[index].category.title.take(10)
                        } else {
                            ""
                        }
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}
