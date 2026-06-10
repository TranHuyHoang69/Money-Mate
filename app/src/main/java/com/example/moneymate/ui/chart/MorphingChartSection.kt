package com.example.moneymate.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    morphProgress: Float, // Chạy từ 0.0f (Thuần Pie) đến 1.0f (Thuần Bar)
    dynamicHeight: Dp
) {
    // TỐI ƯU 1: Parse sẵn mã màu từ chuỗi String của chuỗi data ở ngoài, cấm parse trong Canvas
    val optimizedChartData = remember(chartData) {
        chartData.map { data ->
            val parsedColor = try {
                Color(android.graphics.Color.parseColor(data.color))
            } catch (e: Exception) {
                Color(0xFF4B8361)
            }
            Pair(data, parsedColor)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(dynamicHeight),
        color = MaterialTheme.colorScheme.surface, // 🌟 ĐỘNG: Gạt bỏ nền xám trắng cứng cũ, tự thích ứng màu nền khối sáng/tối
        shadowElevation = (morphProgress * 4).dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ẩn dần chữ "Tổng cộng" mượt mà dựa trên ma trận alpha tầng cứng đồ họa (graphicsLayer)
            if (morphProgress < 0.6f && optimizedChartData.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { alpha = 1f - morphProgress * 1.66f }
                ) {
                    Text(
                        text = "Tổng cộng",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 🌟 ĐỘNG: Nhãn text phụ phân cấp rõ nét
                    )
                    val total = remember(chartData) { chartData.sumOf { it.totalAmount } }
                    Text(
                        text = "${String.format("%,.0f", total)} đ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface // 🌟 ĐỘNG: Số tiền hiển thị tương phản sắc sảo (Trắng/Đen)
                    )
                }
            }

            if (optimizedChartData.isNotEmpty()) {
                MorphingCanvas(optimizedChartData, morphProgress)
            }
        }
    }
}

@Composable
private fun MorphingCanvas(
    optimizedData: List<Pair<ChartData, Color>>,
    progress: Float
) {
    // 🌟 ĐỘNG: Lấy dải màu outline của hệ thống để vẽ vòng tròn nền hướng dẫn thị giác trong Canvas
    val guideCircleColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Cấu hình kích thước hình học dựa trên trạng thái Progress tỷ lệ thuận
        val pieCenter = Offset(width / 2f, height / 2f)
        val targetBarY = height - 24.dp.toPx()
        val barHeight = 14.dp.toPx()
        val basePieRadius = 70.dp.toPx()
        val pieStrokeWidth = 32.dp.toPx()

        // Vẽ vòng tròn nền mờ hỗ trợ thị giác
        if (progress < 1f) {
            drawCircle(
                color = guideCircleColor.copy(alpha = (1f - progress) * 0.25f), // 🌟 ĐỘNG: Đồng bộ độ mờ tinh tế theo cấu trúc nền tối/sáng
                radius = basePieRadius,
                center = pieCenter,
                style = Stroke(width = pieStrokeWidth)
            )
        }

        var currentStartAngle = -90f
        var currentBarX = 0f

        optimizedData.forEachIndexed { index, (data, color) ->
            val sweepAngle = (data.percentage / 100f) * 360f
            val sectionBarWidth = (data.percentage / 100f) * width

            // 1. TÍNH TOÁN ĐƯỜNG ĐI CỦA TRẠNG THÁI PIE (CUNG TRÒN)
            val pieTopLeft = Offset(pieCenter.x - basePieRadius, pieCenter.y - basePieRadius)
            val pieSize = Size(basePieRadius * 2, basePieRadius * 2)

            // 2. TÍNH TOÁN ĐƯỜNG ĐI CỦA TRẠNG THÁI BAR (THANH NGANG)
            val barTopLeft = Offset(currentBarX, targetBarY)
            val barSize = Size(sectionBarWidth, barHeight)

            // 3. THUẬT TOÁN MORPHING NỘI SUY (INTERPOLATION)
            val morphTopLeft = Offset(
                x = lerp(pieTopLeft.x, barTopLeft.x, progress),
                y = lerp(pieTopLeft.y, barTopLeft.y, progress)
            )
            val morphSize = Size(
                width = lerp(pieSize.width, barSize.width, progress),
                height = lerp(pieSize.height, barSize.height, progress)
            )
            val currentStrokeWidth = lerp(pieStrokeWidth, barHeight, progress)

            if (progress < 0.85f) {
                // CHẾ ĐỘ BIẾN HÌNH CHỦ ĐẠO: Cung tròn bẹt và kéo dãn tọa độ ra biên màn hình
                drawArc(
                    color = color,
                    startAngle = lerp(currentStartAngle, 0f, progress),
                    sweepAngle = lerp(sweepAngle, 360f * (data.percentage / 100f), progress),
                    useCenter = false,
                    topLeft = morphTopLeft,
                    size = morphSize,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Butt)
                )
            } else {
                // CHẾ ĐỘ THANH NGANG THUẦN TÚY: Chuyển sang vẽ Rect để xử lý bo góc chuẩn Material 3 ở 2 đầu
                val isFirst = index == 0
                val isLast = index == optimizedData.lastIndex

                val cornerRadius = when {
                    isFirst && isLast -> CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    isFirst -> CornerRadius(6.dp.toPx(), 0f)
                    isLast -> CornerRadius(0f, 6.dp.toPx())
                    else -> CornerRadius.Zero
                }

                drawRoundRect(
                    color = color,
                    topLeft = barTopLeft,
                    size = barSize,
                    cornerRadius = cornerRadius
                )
            }

            currentStartAngle += sweepAngle
            currentBarX += sectionBarWidth
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}