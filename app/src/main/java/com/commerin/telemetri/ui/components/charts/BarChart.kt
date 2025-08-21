package com.commerin.telemetri.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color = Color.Blue
)

@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    title: String = "",
    maxValue: Float? = null,
    showValues: Boolean = true,
    barSpacing: Float = 8f
) {
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
                    drawBarChart(
                        data = data,
                        maxValue = maxValue ?: data.maxOf { it.value },
                        showValues = showValues,
                        barSpacing = barSpacing
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
    }
}

private fun DrawScope.drawBarChart(
    data: List<BarChartData>,
    maxValue: Float,
    showValues: Boolean,
    barSpacing: Float
) {
    if (data.isEmpty() || maxValue <= 0) return

    val padding = 40.dp.toPx()
    val chartWidth = size.width - (padding * 2)
    val chartHeight = size.height - (padding * 2)

    val totalSpacing = barSpacing.dp.toPx() * (data.size - 1)
    val barWidth = (chartWidth - totalSpacing) / data.size

    data.forEachIndexed { index, barData ->
        val barHeight = (barData.value / maxValue) * chartHeight
        val barLeft = padding + (barWidth + barSpacing.dp.toPx()) * index
        val barTop = size.height - padding - barHeight

        // Draw bar
        drawRect(
            color = barData.color,
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth, barHeight)
        )

        // Draw value text if enabled
        if (showValues) {
            // Note: In a real implementation, you'd use drawText with proper text measurement
            // For now, we'll just draw the bar without text overlay
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
