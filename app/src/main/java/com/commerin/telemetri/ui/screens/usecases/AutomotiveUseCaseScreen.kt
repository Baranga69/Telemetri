package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.ui.components.DataRow
import com.commerin.telemetri.ui.components.TelemetryDataCard
import com.commerin.telemetri.ui.components.charts.SignalStrengthGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedGauge
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Automotive Telemetry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "High-Precision Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCollecting) "Active - Collecting data every second" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
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
                            containerColor = if (isCollecting) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCollecting) "Stop" else "Start")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Data Display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Network Signal Monitoring Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vehicle Connectivity Status",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Signal Strength Gauge
                            SignalStrengthGauge(
                                signalStrength = networkData?.signalStrength ?: -75,
                                modifier = Modifier.weight(1f),
                                title = "Signal Strength"
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Network Speed Gauge
                            NetworkSpeedGauge(
                                speedBps = networkData?.connectionSpeed ?: 15_000_000L, // 15 Mbps default
                                modifier = Modifier.weight(1f),
                                title = "Data Speed"
                            )
                        }

                        // Network type indicator
                        networkData?.let { data ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text(
                                        text = "${data.networkType.name} Connected",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Location Data Card
            locationData?.let { location ->
                item {
                    TelemetryDataCard(
                        title = "GPS Location",
                        icon = Icons.Default.LocationOn,
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        isActive = true
                    ) {
                        DataRow("Latitude", String.format(Locale.getDefault(), "%.6f", location.latitude))
                        DataRow("Longitude", String.format(Locale.getDefault(), "%.6f", location.longitude))
                        DataRow("Altitude", "${(location.altitude ?: 0.0).toInt()} m")
                        DataRow("Speed", "${(location.speed ?: 0f).toInt()} km/h")
                        DataRow("Accuracy", "${(location.accuracy ?: 0f).toInt()} m")
                        DataRow("Bearing", "${(location.bearing ?: 0f).toInt()}°")
                    }
                }
            }

            // Sensor Data Card
            sensorData?.let { sensors ->
                if (sensors.isNotEmpty()) {
                    val latestSensor = sensors.first() // Show the most recent sensor data
                    item {
                        TelemetryDataCard(
                            title = "Motion Sensors",
                            icon = Icons.Default.Sensors,
                            color = Color(0xFF2196F3).copy(alpha = 0.1f),
                            isActive = true
                        ) {
                            DataRow("Acceleration X", String.format(Locale.getDefault(), "%.2f m/s²", latestSensor.x))
                            DataRow("Acceleration Y", String.format(Locale.getDefault(), "%.2f m/s²", latestSensor.y))
                            DataRow("Acceleration Z", String.format(Locale.getDefault(), "%.2f m/s²", latestSensor.z))
                            DataRow("Sensor Type", latestSensor.sensorType.name)
                            DataRow("Accuracy", "${latestSensor.accuracy}")
                            DataRow("Active Sensors", "${sensors.size}")
                        }
                    }
                }
            }

            // Audio Environment Card
            audioData?.let { audio ->
                item {
                    TelemetryDataCard(
                        title = "Audio Environment",
                        icon = Icons.Default.GraphicEq,
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        isActive = true
                    ) {
                        DataRow("Sound Level", "${audio.decibels.toInt()} dB")
                        DataRow("Frequency", "${audio.dominantFrequency.toInt()} Hz")
                        DataRow("Amplitude", String.format(Locale.getDefault(), "%.2f", audio.amplitude))
                        DataRow("Classification", audio.soundClassification.name)
                        DataRow("Voice Detected", if (audio.isVoiceDetected) "Yes" else "No")
                        DataRow("Noise Level", audio.noiseLevelCategory)
                    }
                }
            }

            // Performance Data Card
            performanceData?.let { performance ->
                item {
                    TelemetryDataCard(
                        title = "Device Performance",
                        icon = Icons.Default.Speed,
                        color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                        isActive = true
                    ) {
                        DataRow("CPU Usage", "${performance.cpuUsage.toInt()}%")
                        DataRow("Memory Usage", "${(performance.memoryUsage.usedRam / 1024 / 1024).toInt()} MB")
                        DataRow("Battery Level", "${performance.batteryInfo.level}%")
                        DataRow("Battery Temp", "${performance.batteryInfo.temperature.toInt()}°C")
                    }
                }
            }
        }
    }
}
