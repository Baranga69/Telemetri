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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun SignalStrengthGauge(
    signalStrength: Int, // Signal strength in dBm (e.g., -30 to -100)
    modifier: Modifier = Modifier,
    title: String = "Signal Strength",
    minValue: Int = -100,
    maxValue: Int = -30
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Gauge
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawSignalGauge(
                    signalStrength = signalStrength,
                    minValue = minValue,
                    maxValue = maxValue
                )
            }
        }

        // Value display
        Text(
            text = "${signalStrength} dBm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = getSignalColor(signalStrength)
        )

        Text(
            text = getSignalQualityText(signalStrength),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawSignalGauge(
    signalStrength: Int,
    minValue: Int,
    maxValue: Int
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) / 2 * 0.8f

    // Background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.3f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )

    // Calculate progress (0 to 1)
    val progress = ((signalStrength - minValue).toFloat() / (maxValue - minValue).toFloat())
        .coerceIn(0f, 1f)

    // Progress arc
    val sweepAngle = 180f * progress
    val color = getSignalColor(signalStrength)

    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw tick marks
    val tickCount = 6
    for (i in 0..tickCount) {
        val angle = 180f + (180f / tickCount) * i
        val tickRadius = radius + 8.dp.toPx()
        val tickEnd = radius - 8.dp.toPx()

        val startX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * tickEnd
        val startY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * tickEnd
        val endX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * tickRadius
        val endY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * tickRadius

        drawLine(
            color = Color.Gray,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun getSignalColor(signalStrength: Int): Color {
    return when {
        signalStrength >= -50 -> Color(0xFF4CAF50) // Excellent - Green
        signalStrength >= -60 -> Color(0xFF8BC34A) // Good - Light Green
        signalStrength >= -70 -> Color(0xFFFFEB3B) // Fair - Yellow
        signalStrength >= -80 -> Color(0xFFFF9800) // Poor - Orange
        else -> Color(0xFFF44336) // Very Poor - Red
    }
}

private fun getSignalQualityText(signalStrength: Int): String {
    return when {
        signalStrength >= -50 -> "Excellent"
        signalStrength >= -60 -> "Good"
        signalStrength >= -70 -> "Fair"
        signalStrength >= -80 -> "Poor"
        else -> "Very Poor"
    }
}

@Composable
fun NetworkSpeedGauge(
    speedBps: Long, // Speed in bits per second
    modifier: Modifier = Modifier,
    title: String = "Connection Speed"
) {
    val speedMbps = speedBps / 1_000_000f // Convert to Mbps

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Gauge
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawSpeedGauge(speedMbps)
            }
        }

        // Value display
        Text(
            text = formatSpeed(speedBps),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = getSpeedColor(speedMbps)
        )

        Text(
            text = getSpeedQualityText(speedMbps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawSpeedGauge(speedMbps: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) / 2 * 0.8f

    // Background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.3f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )

    // Calculate progress (0 to 1) based on logarithmic scale
    val maxSpeed = 100f // 100 Mbps max for scale
    val progress = (speedMbps / maxSpeed).coerceIn(0f, 1f)

    // Progress arc
    val sweepAngle = 180f * progress
    val color = getSpeedColor(speedMbps)

    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun getSpeedColor(speedMbps: Float): Color {
    return when {
        speedMbps >= 25f -> Color(0xFF4CAF50) // Fast - Green
        speedMbps >= 10f -> Color(0xFF8BC34A) // Good - Light Green
        speedMbps >= 5f -> Color(0xFFFFEB3B) // Moderate - Yellow
        speedMbps >= 1f -> Color(0xFFFF9800) // Slow - Orange
        else -> Color(0xFFF44336) // Very Slow - Red
    }
}

private fun getSpeedQualityText(speedMbps: Float): String {
    return when {
        speedMbps >= 25f -> "Fast"
        speedMbps >= 10f -> "Good"
        speedMbps >= 5f -> "Moderate"
        speedMbps >= 1f -> "Slow"
        else -> "Very Slow"
    }
}

private fun formatSpeed(speedBps: Long): String {
    return when {
        speedBps >= 1_000_000_000 -> "%.1f Gbps".format(speedBps / 1_000_000_000f)
        speedBps >= 1_000_000 -> "%.1f Mbps".format(speedBps / 1_000_000f)
        speedBps >= 1_000 -> "%.1f Kbps".format(speedBps / 1_000f)
        else -> "$speedBps bps"
    }
}
