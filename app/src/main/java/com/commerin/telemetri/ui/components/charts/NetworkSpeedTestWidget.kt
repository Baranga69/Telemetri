package com.commerin.telemetri.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.*

enum class AppNetworkTestType {
    DOWNLOAD, UPLOAD, PING
}

data class NetworkSpeedData(
    val downloadSpeed: Float = 0f, // Mbps
    val uploadSpeed: Float = 0f,   // Mbps
    val ping: Float = 0f,          // ms
    val isTestRunning: Boolean = false,
    val currentTestType: AppNetworkTestType? = null,
    val progress: Float = 0f
)

@Composable
fun NetworkSpeedTestWidget(
    networkData: NetworkSpeedData,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for the main gauge
    val animatedDownloadSpeed by animateFloatAsState(
        targetValue = networkData.downloadSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "download_speed"
    )

    val animatedUploadSpeed by animateFloatAsState(
        targetValue = networkData.uploadSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "upload_speed"
    )

    val animatedPing by animateFloatAsState(
        targetValue = networkData.ping,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ping"
    )

    // Progress animation for test running
    val testProgress by animateFloatAsState(
        targetValue = if (networkData.isTestRunning) networkData.progress else 0f,
        animationSpec = tween(500),
        label = "test_progress"
    )

    // Pulsing animation for active test
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Signal wave animation
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_animation"
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Internet Speed Test",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (networkData.isTestRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(testProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Speed Gauge & Test Type Indicator
            // The Box is used to stack the gauge and indicator, but the indicator is no longer offset
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(if (networkData.isTestRunning) pulseScale else 1f)
                ) {
                    drawNetworkSpeedGauge(
                        downloadSpeed = animatedDownloadSpeed,
                        uploadSpeed = animatedUploadSpeed,
                        isTestRunning = networkData.isTestRunning,
                        currentTestType = networkData.currentTestType,
                        waveOffset = waveOffset,
                        progress = testProgress
                    )
                }
            }

            // This is the correct placement for the test type indicator
            if (networkData.isTestRunning && networkData.currentTestType != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = networkData.currentTestType.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed display below the gauge
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally),
                colors = CardDefaults.cardColors(
                    containerColor = if (networkData.isTestRunning)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    val currentSpeed = when (networkData.currentTestType) {
                        AppNetworkTestType.DOWNLOAD -> animatedDownloadSpeed
                        AppNetworkTestType.UPLOAD -> animatedUploadSpeed
                        else -> maxOf(animatedDownloadSpeed, animatedUploadSpeed)
                    }

                    Text(
                        text = if (networkData.currentTestType == AppNetworkTestType.PING) {
                            "${animatedPing.toInt()}"
                        } else {
                            "${currentSpeed.toInt()}"
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (networkData.isTestRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (networkData.currentTestType == AppNetworkTestType.PING) "ms" else "Mbps",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Speed Results Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NetworkMetricCard(
                    title = "Download",
                    value = "${animatedDownloadSpeed.toInt()}",
                    unit = "Mbps",
                    icon = Icons.Default.NetworkCheck,
                    isActive = networkData.currentTestType == AppNetworkTestType.DOWNLOAD
                )

                NetworkMetricCard(
                    title = "Upload",
                    value = "${animatedUploadSpeed.toInt()}",
                    unit = "Mbps",
                    icon = Icons.Default.NetworkCheck,
                    isActive = networkData.currentTestType == AppNetworkTestType.UPLOAD
                )

                NetworkMetricCard(
                    title = "Ping",
                    value = "${animatedPing.toInt()}",
                    unit = "ms",
                    icon = Icons.Default.NetworkCheck,
                    isActive = networkData.currentTestType == AppNetworkTestType.PING
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Button
            Button(
                onClick = if (networkData.isTestRunning) onStopTest else onStartTest,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (networkData.isTestRunning)
                        MaterialTheme.colorScheme.errorContainer else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (networkData.isTestRunning)
                        MaterialTheme.colorScheme.onErrorContainer else
                        MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (networkData.isTestRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (networkData.isTestRunning) "Stop Test" else "Start Test",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun NetworkMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawNetworkSpeedGauge(
    downloadSpeed: Float,
    uploadSpeed: Float,
    isTestRunning: Boolean,
    currentTestType: AppNetworkTestType?,
    waveOffset: Float,
    progress: Float
) {
    val center = size.center
    val radius = size.minDimension / 2 * 0.75f
    val strokeWidth = 8.dp.toPx()

    // Background circle
    drawCircle(
        color = Color.Gray.copy(alpha = 0.1f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // Progress ring when testing
    if (isTestRunning && progress > 0f) {
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }

    // Download speed arc (bottom half)
    val maxSpeed = 100f // Max 100 Mbps for display
    val downloadAngle = (downloadSpeed / maxSpeed) * 180f
    if (downloadSpeed > 0f) {
        drawArc(
            color = Color(0xFF2196F3), // Blue for download
            startAngle = 180f,
            sweepAngle = downloadAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
            size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f)
        )
    }

    // Upload speed arc (top half)
    val uploadAngle = (uploadSpeed / maxSpeed) * 180f
    if (uploadSpeed > 0f) {
        drawArc(
            color = Color(0xFFFF9800), // Orange for upload
            startAngle = 0f,
            sweepAngle = uploadAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
            size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f)
        )
    }

    // Animated signal waves when testing
    if (isTestRunning) {
        for (i in 0..2) {
            val waveRadius = radius * (0.3f + i * 0.15f)
            val alpha = 0.3f - (i * 0.1f)
            val animatedRadius = waveRadius + sin(Math.toRadians((waveOffset + i * 120).toDouble())).toFloat() * 20f

            drawCircle(
                color = when (currentTestType) {
                    AppNetworkTestType.DOWNLOAD -> Color(0xFF2196F3).copy(alpha = alpha)
                    AppNetworkTestType.UPLOAD -> Color(0xFFFF9800).copy(alpha = alpha)
                    AppNetworkTestType.PING -> Color(0xFF4CAF50).copy(alpha = alpha)
                    else -> Color(0xFF9C27B0).copy(alpha = alpha)
                },
                radius = animatedRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }

    // Center indicator
    drawCircle(
        color = if (isTestRunning) {
            when (currentTestType) {
                AppNetworkTestType.DOWNLOAD -> Color(0xFF2196F3)
                AppNetworkTestType.UPLOAD -> Color(0xFFFF9800)
                AppNetworkTestType.PING -> Color(0xFF4CAF50)
                else -> Color(0xFF9C27B0)
            }
        } else Color.Gray,
        radius = 12.dp.toPx(),
        center = center
    )
}
