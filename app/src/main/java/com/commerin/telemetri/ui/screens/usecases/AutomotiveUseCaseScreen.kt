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
import com.commerin.telemetri.ui.components.DataRow
import com.commerin.telemetri.ui.components.TelemetryDataCard
import com.commerin.telemetri.ui.components.TransparentAppBar
import com.commerin.telemetri.ui.components.charts.SignalStrengthGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedGauge
import com.commerin.telemetri.ui.components.charts.VehicleSpeedometerWidget
import com.commerin.telemetri.ui.components.charts.SpeedUnit
import com.commerin.telemetri.ui.viewmodels.AutomotiveViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomotiveUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: AutomotiveViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val locationData by viewModel.locationData.observeAsState()
    val sensorData by viewModel.sensorData.observeAsState()
    val audioData by viewModel.audioData.observeAsState()
    val networkData by viewModel.networkData.observeAsState()
    val performanceData by viewModel.performanceData.observeAsState()

    // Speed recording state
    var isSpeedRecording by remember { mutableStateOf(false) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KPH) }

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
                    currentSpeed = locationData?.speed ?: 0f,
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

            // Network Performance Gauges
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Network Performance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SignalStrengthGauge(
                                signalStrength = networkData?.signalStrength ?: -75,
                                modifier = Modifier.weight(1f),
                                title = "Signal Strength"
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            NetworkSpeedGauge(
                                speedBps = networkData?.connectionSpeed ?: 10_000_000L,
                                modifier = Modifier.weight(1f),
                                title = "Connection Speed"
                            )
                        }
                    }
                }
            }

            // Sensor Data
            item {
                TelemetryDataCard(
                    title = "Sensor Data",
                    icon = Icons.Default.Sensors,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    sensorData?.let { sensors ->
                        Text(
                            "Active Sensors: ${sensors.size}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        sensors.take(3).forEach { sensor ->
                            DataRow(
                                sensor.sensorType.name,
                                "X: ${String.format(Locale.US, "%.2f", sensor.x)}"
                            )
                        }
                        if (sensors.size > 3) {
                            Text(
                                "+ ${sensors.size - 3} more sensors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } ?: Text(
                        "No sensor data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio Data
            item {
                TelemetryDataCard(
                    title = "Audio Environment",
                    icon = Icons.Default.Mic,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ) {
                    audioData?.let { audio ->
                        DataRow("Sound Level", "${String.format(Locale.US, "%.1f", audio.decibels)} dB")
                        DataRow("Dominant Frequency", "${String.format(Locale.US, "%.0f", audio.dominantFrequency)} Hz")
                        DataRow("Classification", audio.soundClassification.name)
                        DataRow("Voice Detected", if (audio.isVoiceDetected) "Yes" else "No")
                    } ?: Text(
                        "No audio data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Performance Data
            item {
                TelemetryDataCard(
                    title = "System Performance",
                    icon = Icons.Default.Speed,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    performanceData?.let { performance ->
                        DataRow("CPU Usage", "${String.format(Locale.US, "%.1f", performance.cpuUsage)}%")
                        DataRow("Memory", "${performance.memoryUsage.appMemoryUsage / 1024 / 1024} MB")
                        DataRow("Battery", "${performance.batteryInfo.level}%")
                        DataRow("Temperature", "${String.format(Locale.US, "%.1f", performance.batteryInfo.temperature)}°C")
                    } ?: Text(
                        "No performance data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
