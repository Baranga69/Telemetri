package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.domain.model.ActivityType
import com.commerin.telemetri.ui.components.DataRow
import com.commerin.telemetri.ui.components.TelemetryDataCard
import com.commerin.telemetri.ui.components.TransparentAppBar
import com.commerin.telemetri.ui.components.charts.SignalStrengthGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedGauge
import com.commerin.telemetri.ui.components.charts.VehicleSpeedometerWidget
import com.commerin.telemetri.ui.components.charts.SpeedUnit
import com.commerin.telemetri.ui.viewmodels.AutomotiveViewModel
import java.util.Locale

// Data class for vehicle testing statistics
data class SpeedStatistics(
    val maxSpeed: Float,
    val avgSpeed: Float,
    val sampleCount: Int,
    val totalDistance: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomotiveUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: AutomotiveViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val locationData by viewModel.locationData.observeAsState()
    val motionData by viewModel.motionData.observeAsState()

    // Speed recording state
    var isSpeedRecording by remember { mutableStateOf(false) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KPH) }

    // Enhanced speed calculation using sensor fusion with improved accuracy
    val currentSpeed = remember(motionData, locationData) {
        val motion = motionData
        val location = locationData

        when {
            // Enhanced vehicle detection with multiple criteria
            motion?.activityType == ActivityType.IN_VEHICLE && motion.vehicleSpeed > 2f -> {
                // Use sensor speed for in-vehicle detection with minimum threshold
                motion.vehicleSpeed
            }
            // High-accuracy GPS speed with confidence check
            location?.speed != null && location.speed!! > 0.5f &&
            location.accuracy != null && location.accuracy!! < 10f -> {
                // Only use GPS if accuracy is good (< 10 meters) and speed > 0.5 m/s
                location.speed!!
            }
            // Fallback to any available GPS speed for low speeds
            location?.speed != null && location.speed!! > 0f -> {
                location.speed!!
            }
            // Default to 0 if no reliable speed data
            else -> 0f
        }
    }

    // Add speed statistics for vehicle testing
    val speedStats = remember { mutableStateOf<SpeedStatistics?>(null) }

    // Calculate running statistics when collecting data
    LaunchedEffect(currentSpeed, isCollecting) {
        if (isCollecting && currentSpeed > 0f) {
            val current = speedStats.value
            if (current == null) {
                speedStats.value = SpeedStatistics(
                    maxSpeed = currentSpeed,
                    avgSpeed = currentSpeed,
                    sampleCount = 1,
                    totalDistance = 0f
                )
            } else {
                val newSampleCount = current.sampleCount + 1
                val newAvgSpeed = ((current.avgSpeed * current.sampleCount) + currentSpeed) / newSampleCount
                speedStats.value = current.copy(
                    maxSpeed = maxOf(current.maxSpeed, currentSpeed),
                    avgSpeed = newAvgSpeed,
                    sampleCount = newSampleCount
                )
            }
        } else if (!isCollecting) {
            speedStats.value = null // Reset when not collecting
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent app bar
        TransparentAppBar(
            title = "Automotive Telemetry",
            onBackPressed = onBackPressed
        )

        // Main content with padding for transparent app bar
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 120.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Automotive Collection",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isCollecting) "High-precision vehicle tracking active" else "Stopped",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCollecting)
                                        MaterialTheme.colorScheme.tertiary else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (isCollecting) {
                                        viewModel.stopCollection()
                                    } else {
                                        viewModel.startAutomotiveCollection()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCollecting)
                                        MaterialTheme.colorScheme.errorContainer else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isCollecting)
                                        MaterialTheme.colorScheme.onErrorContainer else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCollecting) "Stop" else "Start",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Vehicle Speedometer Widget
            item {
                VehicleSpeedometerWidget(
                    currentSpeed = currentSpeed,
                    isRecording = isSpeedRecording,
                    speedUnit = speedUnit,
                    onToggleRecording = {
                        isSpeedRecording = !isSpeedRecording
                        if (isSpeedRecording && !isCollecting) {
                            viewModel.startAutomotiveCollection()
                        }
                    },
                    onToggleUnit = {
                        speedUnit = if (speedUnit == SpeedUnit.KPH) SpeedUnit.MPH else SpeedUnit.KPH
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Speed Statistics Card for Vehicle Testing
            speedStats.value?.let { stats ->
                item {
                    TelemetryDataCard(
                        title = "Speed Test Statistics",
                        icon = Icons.Default.Speed,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        val unitLabel = if (speedUnit == SpeedUnit.KPH) "km/h" else "mph"
                        val convertedMaxSpeed = if (speedUnit == SpeedUnit.KPH) stats.maxSpeed * 3.6f else stats.maxSpeed * 2.237f
                        val convertedAvgSpeed = if (speedUnit == SpeedUnit.KPH) stats.avgSpeed * 3.6f else stats.avgSpeed * 2.237f

                        DataRow("Max Speed", String.format(Locale.US, "%.1f %s", convertedMaxSpeed, unitLabel))
                        DataRow("Avg Speed", String.format(Locale.US, "%.1f %s", convertedAvgSpeed, unitLabel))
                        DataRow("Data Points", "${stats.sampleCount}")
                        DataRow("Test Duration", "${(stats.sampleCount * 1).coerceAtMost(3600)} seconds") // Estimate based on 1-second updates
                    }
                }
            }

            // Motion Data for Sensor-Based Speed
            item {
                TelemetryDataCard(
                    title = "Motion Analysis",
                    icon = Icons.Default.DirectionsCar,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    motionData?.let { motion ->
                        DataRow("Activity", motion.activityType.name)
                        DataRow("Confidence", String.format(Locale.US, "%.1f%%", motion.confidence * 100))
                        DataRow("Sensor Speed", "${String.format(Locale.US, "%.1f", motion.vehicleSpeed)} m/s")
                        DataRow("Acceleration", String.format(Locale.US, "%.2f m/s²", motion.accelerationMagnitude))
                        DataRow("Gyroscope", String.format(Locale.US, "%.2f rad/s", motion.gyroscopeMagnitude))
                    } ?: Text(
                        "No motion data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Location Data
            item {
                TelemetryDataCard(
                    title = "Location Tracking",
                    icon = Icons.Default.LocationOn,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    locationData?.let { location ->
                        DataRow("Latitude", String.format(Locale.US, "%.6f", location.latitude))
                        DataRow("Longitude", String.format(Locale.US, "%.6f", location.longitude))
                        DataRow("Speed", "${location.speed?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A"} m/s")
                        DataRow("Accuracy", "${location.accuracy?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A"} m")
                        DataRow("Provider", location.provider)
                    } ?: Text(
                        "No location data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
