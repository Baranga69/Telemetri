package com.commerin.telemetri.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

enum class SpeedUnit {
    KPH, MPH
}

@Composable
fun VehicleSpeedometerWidget(
    currentSpeed: Float, // Speed in m/s from location data
    isRecording: Boolean,
    speedUnit: SpeedUnit = SpeedUnit.KPH,
    onToggleRecording: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
    maxSpeed: Float = if (speedUnit == SpeedUnit.KPH) 200f else 125f
) {
    // Convert m/s to selected unit
    val convertedSpeed = when (speedUnit) {
        SpeedUnit.KPH -> currentSpeed * 3.6f // m/s to km/h
        SpeedUnit.MPH -> currentSpeed * 2.237f // m/s to mph
    }

    // Animate the speed value
    val animatedSpeed by animateFloatAsState(
        targetValue = if (isRecording) convertedSpeed else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "speed_animation"
    )

    // Animate needle rotation (240 degrees total sweep, starting from -210 degrees)
    val needleAngle by animateFloatAsState(
        targetValue = -210f + (animatedSpeed / maxSpeed) * 240f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "needle_animation"
    )

    // Pulsing animation for recording indicator
    val recordingPulse by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with title and unit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vehicle Speed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onToggleUnit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = speedUnit.name,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Gauge
            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawSpeedometer(
                        speed = animatedSpeed,
                        maxSpeed = maxSpeed,
                        needleAngle = needleAngle,
                        speedUnit = speedUnit,
                        isRecording = isRecording,
                        recordingPulse = recordingPulse
                    )
                }

                // Digital speed display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 40.dp)
                ) {
                    Text(
                        text = "${animatedSpeed.toInt()}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = speedUnit.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Recording status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        drawCircle(
                            color = if (isRecording) Color.Red else Color.Gray,
                            alpha = if (isRecording) recordingPulse else 0.5f
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Recording" else "Stopped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Record button
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording)
                            MaterialTheme.colorScheme.errorContainer else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isRecording)
                            MaterialTheme.colorScheme.onErrorContainer else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Stop" else "Start",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSpeedometer(
    speed: Float,
    maxSpeed: Float,
    needleAngle: Float,
    speedUnit: SpeedUnit,
    isRecording: Boolean,
    recordingPulse: Float
) {
    val center = size.center
    val radius = size.minDimension / 2 * 0.8f
    val strokeWidth = 12.dp.toPx()

    // Draw outer ring with gradient
    val gradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF4CAF50), // Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFF9800), // Orange
            Color(0xFFF44336)  // Red
        ),
        center = center
    )

    drawArc(
        brush = gradient,
        startAngle = -210f,
        sweepAngle = 240f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Draw background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.2f),
        startAngle = -210f,
        sweepAngle = 240f,
        useCenter = false,
        style = Stroke(width = strokeWidth * 0.3f),
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Draw speed markings
    for (i in 0..8) {
        val angle = -210f + (i * 30f)
        val markSpeed = (maxSpeed / 8) * i
        val startRadius = radius - strokeWidth
        val endRadius = radius - strokeWidth - 20.dp.toPx()

        val startX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * startRadius
        val startY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * startRadius
        val endX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * endRadius
        val endY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * endRadius

        drawLine(
            color = Color.White,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Draw needle with animation
    rotate(degrees = needleAngle, pivot = center) {
        val needleLength = radius * 0.7f
        val needleWidth = 4.dp.toPx()

        // Needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = center.copy(x = center.x + 2.dp.toPx(), y = center.y + 2.dp.toPx()),
            end = Offset(
                center.x + needleLength + 2.dp.toPx(),
                center.y + 2.dp.toPx()
            ),
            strokeWidth = needleWidth,
            cap = StrokeCap.Round
        )

        // Main needle
        drawLine(
            color = if (isRecording) Color.Red else Color.White,
            start = center,
            end = Offset(center.x + needleLength, center.y),
            strokeWidth = needleWidth,
            cap = StrokeCap.Round
        )
    }

    // Draw center dot with pulsing effect for recording
    val centerDotRadius = 8.dp.toPx()
    val pulseRadius = if (isRecording) centerDotRadius * (1f + recordingPulse * 0.5f) else centerDotRadius

    if (isRecording) {
        drawCircle(
            color = Color.Red.copy(alpha = 0.3f),
            radius = pulseRadius,
            center = center
        )
    }

    drawCircle(
        color = if (isRecording) Color.Red else Color.White,
        radius = centerDotRadius,
        center = center
    )
}
