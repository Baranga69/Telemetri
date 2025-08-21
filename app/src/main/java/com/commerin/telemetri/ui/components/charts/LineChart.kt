package com.commerin.telemetri.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

data class ChartDataPoint(
    val x: Float,
    val y: Float,
    val label: String = ""
)

@Composable
fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    title: String = "",
    xAxisLabel: String = "",
    yAxisLabel: String = "",
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineWidth: Float = 3f,
    showPoints: Boolean = true,
    showGrid: Boolean = true,
    animate: Boolean = false
) {
    val density = LocalDensity.current

    Column(modifier = modifier) {
        // Title
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (data.isNotEmpty()) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawLineChart(
                        data = data,
                        lineColor = lineColor,
                        lineWidth = lineWidth,
                        showPoints = showPoints,
                        showGrid = showGrid
                    )
                }
            } else {
                Text(
                    text = "No data available",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (xAxisLabel.isNotEmpty()) {
                Text(
                    text = xAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawLineChart(
    data: List<ChartDataPoint>,
    lineColor: Color,
    lineWidth: Float,
    showPoints: Boolean,
    showGrid: Boolean
) {
    if (data.isEmpty()) return

    val padding = 40.dp.toPx()
    val chartWidth = size.width - (padding * 2)
    val chartHeight = size.height - (padding * 2)

    // Calculate data bounds
    val minX = data.minOf { it.x }
    val maxX = data.maxOf { it.x }
    val minY = data.minOf { it.y }
    val maxY = data.maxOf { it.y }

    val xRange = maxX - minX
    val yRange = maxY - minY

    // Draw grid if enabled
    if (showGrid) {
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        val gridLines = 5

        // Vertical grid lines
        for (i in 0..gridLines) {
            val x = padding + (chartWidth / gridLines) * i
            drawLine(
                color = gridColor,
                start = Offset(x, padding),
                end = Offset(x, size.height - padding),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Horizontal grid lines
        for (i in 0..gridLines) {
            val y = padding + (chartHeight / gridLines) * i
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

    // Convert data points to screen coordinates
    val points = data.map { point ->
        val x = if (xRange > 0) {
            padding + ((point.x - minX) / xRange) * chartWidth
        } else {
            padding + chartWidth / 2
        }
        val y = if (yRange > 0) {
            size.height - padding - ((point.y - minY) / yRange) * chartHeight
        } else {
            size.height - padding - chartHeight / 2
        }
        Offset(x, y)
    }

    // Draw line connecting points
    if (points.size > 1) {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Draw points if enabled
    if (showPoints) {
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = point
            )
        }
    }

    // Draw axes
    val axisColor = Color.Gray
    val axisWidth = 2.dp.toPx()

    // X-axis
    drawLine(
        color = axisColor,
        start = Offset(padding, size.height - padding),
        end = Offset(size.width - padding, size.height - padding),
        strokeWidth = axisWidth
    )

    // Y-axis
    drawLine(
        color = axisColor,
        start = Offset(padding, padding),
        end = Offset(padding, size.height - padding),
        strokeWidth = axisWidth
    )
}
