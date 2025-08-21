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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.ui.components.DataRow
import com.commerin.telemetri.ui.components.TelemetryDataCard
import com.commerin.telemetri.ui.viewmodels.EnvironmentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentalUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: EnvironmentalViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val audioData by viewModel.audioData.observeAsState()
    val environmentalData by viewModel.environmentalData.observeAsState()
    val sensorData by viewModel.sensorData.observeAsState()
    val locationData by viewModel.locationData.observeAsState()
    val networkData by viewModel.networkData.observeAsState()

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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Environmental Monitoring",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
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
                            text = "Environmental Sensing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCollecting) "Monitoring environment conditions" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (isCollecting) {
                                viewModel.stopCollection()
                            } else {
                                viewModel.startEnvironmentalCollection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCollecting) Color(0xFFF44336) else Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.Eco,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCollecting) "Stop" else "Start Monitoring")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Environmental Overview
        environmentalData?.let { env ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EnvironmentalStat("Temperature", "${String.format("%.1f", env.ambientTemperature)}°C", Icons.Default.Thermostat)
                    EnvironmentalStat("Light", "${String.format("%.0f", env.lightLevel)} lx", Icons.Default.WbSunny)
                    EnvironmentalStat("Humidity", "${String.format("%.0f", env.humidity)}%", Icons.Default.Water)
                    EnvironmentalStat("Pressure", "${String.format("%.0f", env.pressure)} hPa", Icons.Default.Speed)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Data Display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Audio Environment Analysis
            item {
                TelemetryDataCard(
                    title = "Acoustic Environment",
                    icon = Icons.Default.GraphicEq,
                    color = Color(0xFF9C27B0),
                    isActive = audioData != null
                ) {
                    audioData?.let { audio ->
                        DataRow("Noise Level", "${String.format("%.1f", audio.decibels)} dB (${audio.noiseLevelCategory})")
                        DataRow("Dominant Frequency", "${String.format("%.0f", audio.dominantFrequency)} Hz")
                        DataRow("Sound Classification", audio.soundClassification.name)
                        DataRow("Voice Activity", if (audio.isVoiceDetected) "Detected" else "Not Detected")
                        DataRow("Spectral Centroid", "${String.format("%.0f", audio.spectralCentroid)} Hz")
                        DataRow("Zero Crossing Rate", "${String.format("%.3f", audio.zeroCrossingRate)}")
                    } ?: run {
                        Text("No audio data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Environmental Sensors
            item {
                TelemetryDataCard(
                    title = "Environmental Sensors",
                    icon = Icons.Default.Sensors,
                    color = Color(0xFF4CAF50),
                    isActive = environmentalData != null
                ) {
                    environmentalData?.let { env ->
                        DataRow("Temperature", "${String.format("%.2f", env.ambientTemperature)}°C")
                        DataRow("Light Level", "${String.format("%.1f", env.lightLevel)} lux")
                        DataRow("Atmospheric Pressure", "${String.format("%.2f", env.pressure)} hPa")
                        DataRow("Humidity", "${String.format("%.1f", env.humidity)}%")
                        DataRow("Proximity", "${String.format("%.1f", env.proximityDistance)} cm")
                        DataRow("Noise Level", "${String.format("%.1f", env.noiseLevel)} dB")
                    } ?: run {
                        Text("No environmental data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Location Context
            item {
                TelemetryDataCard(
                    title = "Location Context",
                    icon = Icons.Default.Place,
                    color = Color(0xFF2196F3),
                    isActive = locationData != null
                ) {
                    locationData?.let { location ->
                        DataRow("Coordinates", "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}")
                        DataRow("Altitude", "${String.format("%.0f", location.altitude)} m")
                        DataRow("GPS Accuracy", "${String.format("%.0f", location.accuracy)} m")
                        DataRow("Provider", location.provider)
                        DataRow("Timestamp", "${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(location.timestamp))}")
                    } ?: run {
                        Text("No location data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Network Environment
            item {
                TelemetryDataCard(
                    title = "Network Environment",
                    icon = Icons.Default.Wifi,
                    color = Color(0xFFFF9800),
                    isActive = networkData != null
                ) {
                    networkData?.let { network ->
                        DataRow("Network Type", network.networkType.name)
                        DataRow("Signal Quality", "${String.format("%.1f", network.signalQuality * 100)}%")
                        DataRow("Signal Strength", "${network.signalStrength} dBm")
                        DataRow("WiFi Networks", "${network.wifiInfo?.nearbyNetworks?.size ?: 0} detected")
                        DataRow("Bluetooth Devices", "${network.bluetoothDevices.size} nearby")
                        DataRow("Connection Speed", "${network.connectionSpeed / 1000} kbps")
                    } ?: run {
                        Text("No network data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Raw Sensor Data
            item {
                TelemetryDataCard(
                    title = "Raw Sensor Readings",
                    icon = Icons.Default.Memory,
                    color = Color(0xFF795548),
                    isActive = !sensorData.isNullOrEmpty()
                ) {
                    sensorData?.takeLast(5)?.forEach { sensor ->
                        when (sensor.sensorType) {
                            com.commerin.telemetri.domain.model.SensorType.AMBIENT_TEMPERATURE -> {
                                DataRow("Temperature Sensor", "${String.format("%.2f", sensor.values[0])}°C")
                            }
                            com.commerin.telemetri.domain.model.SensorType.LIGHT -> {
                                DataRow("Light Sensor", "${String.format("%.0f", sensor.values[0])} lux")
                            }
                            com.commerin.telemetri.domain.model.SensorType.PRESSURE -> {
                                DataRow("Pressure Sensor", "${String.format("%.2f", sensor.values[0])} hPa")
                            }
                            com.commerin.telemetri.domain.model.SensorType.RELATIVE_HUMIDITY -> {
                                DataRow("Humidity Sensor", "${String.format("%.1f", sensor.values[0])}%")
                            }
                            com.commerin.telemetri.domain.model.SensorType.PROXIMITY -> {
                                DataRow("Proximity Sensor", "${String.format("%.1f", sensor.values[0])} cm")
                            }
                            else -> {
                                // Skip non-environmental sensors
                            }
                        }
                    } ?: run {
                        Text("No sensor data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvironmentalStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
