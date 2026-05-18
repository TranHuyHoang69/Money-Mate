package com.example.moneymate.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.viewmodel.ChartData

@Composable
fun MorphingChartSection(
    chartData: List<ChartData>,
    morphProgress: Float,
    dynamicHeight: Dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(dynamicHeight),
        color = Color(0xFFF8F9FA),
        shadowElevation = (morphProgress * 4).dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Hiển thị text "Tổng cộng" ở giữa khi là hình tròn (progress thấp)
            if (morphProgress < 0.6f && chartData.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { alpha = 1f - morphProgress * 2.5f }
                ) {
                    Text("Tổng cộng", fontSize = 11.sp, color = Color.Gray)
                    val total = chartData.sumOf { it.totalAmount }
                    Text(
                        text = "${String.format("%,.0f", total)} đ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (chartData.isNotEmpty()) {
                MorphingCanvas(chartData, morphProgress)
            }
        }
    }
}

@Composable
private fun MorphingCanvas(chartData: List<ChartData>, progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val pieRadius = 70.dp.toPx()
        val pieStrokeWidth = 36.dp.toPx()
        val barHeight = 14.dp.toPx()
        val barY = height - 20.dp.toPx()

        // Vẽ vòng tròn nền mờ khi ở chế độ Pie Chart
        if (progress < 0.8f) {
            drawCircle(
                color = Color.LightGray.copy(alpha = (1f - progress) * 0.2f),
                radius = pieRadius,
                center = Offset(width / 2, height / 2),
                style = Stroke(width = pieStrokeWidth)
            )
        }

        var currentStartAngle = -90f
        var currentBarX = 0f

        chartData.forEach { data ->
            val sweepAngle = (data.percentage / 100f) * 360f
            val sectionBarWidth = (data.percentage / 100f) * width
            val color = Color(android.graphics.Color.parseColor(data.color))

            // Vẽ cung tròn (Pie)
            if (progress < 0.99f) {
                drawArc(
                    color = color.copy(alpha = 1f - progress),
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(width / 2 - pieRadius, height / 2 - pieRadius),
                    size = Size(pieRadius * 2, pieRadius * 2),
                    style = Stroke(width = pieStrokeWidth * (1f - progress * 0.4f), cap = StrokeCap.Butt)
                )
            }

            // Vẽ thanh ngang (Bar)
            if (progress > 0.05f) {
                drawRoundRect(
                    color = color.copy(alpha = progress),
                    topLeft = Offset(currentBarX, barY),
                    size = Size(sectionBarWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
            currentStartAngle += sweepAngle
            currentBarX += sectionBarWidth
        }
    }
}