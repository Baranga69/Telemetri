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
import com.commerin.telemetri.ui.viewmodels.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val telemetryData by viewModel.telemetryData.observeAsState()
    val alertLevel by viewModel.alertLevel.observeAsState("Normal")
    val dataQuality by viewModel.dataQuality.observeAsState("Unknown")
    val collectionStats by viewModel.collectionStats.observeAsState()

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
                text = "Security Monitoring",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alert Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (alertLevel) {
                    "Critical" -> Color(0xFFF44336).copy(alpha = 0.1f)
                    "High" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    "Medium" -> Color(0xFFFFEB3B).copy(alpha = 0.1f)
                    else -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (alertLevel) {
                        "Critical" -> Icons.Default.Warning
                        "High" -> Icons.Default.Error
                        "Medium" -> Icons.Default.Info
                        else -> Icons.Default.Shield
                    },
                    contentDescription = null,
                    tint = when (alertLevel) {
                        "Critical" -> Color(0xFFF44336)
                        "High" -> Color(0xFFFF9800)
                        "Medium" -> Color(0xFFFFEB3B)
                        else -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Security Alert Level: $alertLevel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Data Quality: $dataQuality",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f))
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
                            text = "Maximum Security Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCollecting) "Ultra-high frequency monitoring active" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (isCollecting) {
                                viewModel.stopCollection()
                            } else {
                                viewModel.startSecurityCollection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCollecting) Color(0xFF757575) else Color(0xFFF44336)
                        )
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.Security,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCollecting) "Stop" else "Secure Monitor")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collection Statistics
        collectionStats?.let { stats ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SecurityStat("Data Points", "${stats.totalDataPoints}", Icons.Default.DataUsage)
                    SecurityStat("Sensors", "${stats.activeSensors}", Icons.Default.Sensors)
                    SecurityStat("Uptime", "${stats.uptimeMinutes}m", Icons.Default.Timer)
                    SecurityStat("Events", "${stats.securityEvents}", Icons.Default.Event)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comprehensive Telemetry Display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            telemetryData?.let { telemetry ->
                // Sensor Matrix
                item {
                    TelemetryDataCard(
                        title = "Sensor Matrix (${telemetry.sensors.size} active)",
                        icon = Icons.Default.GridView,
                        color = Color(0xFF2196F3),
                        isActive = telemetry.sensors.isNotEmpty()
                    ) {
                        telemetry.sensors.takeLast(8).forEach { sensor ->
                            DataRow(
                                sensor.sensorType.name,
                                "Acc:${sensor.accuracy} ${String.format("%.3f", sensor.values.firstOrNull() ?: 0f)}"
                            )
                        }
                    }
                }

                // Location Intelligence
                item {
                    TelemetryDataCard(
                        title = "Location Intelligence",
                        icon = Icons.Default.GpsFixed,
                        color = Color(0xFF4CAF50),
                        isActive = telemetry.location != null
                    ) {
                        telemetry.location?.let { location ->
                            DataRow("Precision", "${String.format("%.1f", location.accuracy)}m accuracy")
                            DataRow("Movement", "${String.format("%.2f", location.speed?.times(3.6))} km/h")
                            DataRow("Elevation", "${String.format("%.0f", location.altitude)}m ASL")
                            DataRow("Provider", location.provider)
                            DataRow("Last Update", "${System.currentTimeMillis() - location.timestamp}ms ago")
                        } ?: run {
                            Text("No location data", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Audio Surveillance
                item {
                    TelemetryDataCard(
                        title = "Audio Surveillance",
                        icon = Icons.Default.RecordVoiceOver,
                        color = Color(0xFF9C27B0),
                        isActive = telemetry.environmental?.noiseLevel != null
                    ) {
                        telemetry.environmental?.let { env ->
                            DataRow("Ambient Noise", "${String.format("%.1f", env.noiseLevel)} dB")
                            DataRow("Environment", "Temperature: ${String.format("%.1f", env.ambientTemperature)}°C")
                            DataRow("Light Conditions", "${String.format("%.0f", env.lightLevel)} lux")
                            DataRow("Atmospheric", "${String.format("%.1f", env.pressure)} hPa")
                        } ?: run {
                            Text("No environmental data", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Motion Analytics
                item {
                    TelemetryDataCard(
                        title = "Motion Analytics",
                        icon = Icons.Default.Timeline,
                        color = Color(0xFFFF9800),
                        isActive = telemetry.motion != null
                    ) {
                        telemetry.motion?.let { motion ->
                            DataRow("Activity", "${motion.activityType.name} (${String.format("%.0f", motion.confidence * 100)}%)")
                            DataRow("Acceleration", "${String.format("%.3f", motion.accelerationMagnitude)} m/s²")
                            DataRow("Rotation", "${String.format("%.3f", motion.gyroscopeMagnitude)} rad/s")
                            DataRow("Magnetic Field", "${String.format("%.1f", motion.magneticFieldMagnitude)} µT")
                            DataRow("Steps", "${motion.stepCount} (${String.format("%.1f", motion.stepFrequency)}/min)")
                        } ?: run {
                            Text("No motion data", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Device Security
                item {
                    TelemetryDataCard(
                        title = "Device Security Status",
                        icon = Icons.Default.Security,
                        color = Color(0xFFF44336),
                        isActive = telemetry.deviceState != null
                    ) {
                        telemetry.deviceState?.let { device ->
                            DataRow("Battery Health", "${device.batteryLevel}% (${device.batteryTemperature}°C)")
                            DataRow("Charging", if (device.isCharging) "Connected: ${device.chargingType}" else "Disconnected")
                            DataRow("Network", "${device.networkType} (${device.signalStrength} dBm)")
                            DataRow("Screen", if (device.isScreenOn) "Active (${device.screenBrightness}%)" else "Locked")
                            DataRow("Connectivity", "WiFi:${if (device.isWifiEnabled) "ON" else "OFF"} BT:${if (device.isBluetoothEnabled) "ON" else "OFF"}")
                        } ?: run {
                            Text("No device state data", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Metadata Analysis
                item {
                    TelemetryDataCard(
                        title = "Session Metadata",
                        icon = Icons.Default.Analytics,
                        color = Color(0xFF607D8B),
                        isActive = true
                    ) {
                        DataRow("Session ID", telemetry.sessionId.takeLast(8))
                        DataRow("Event Type", telemetry.eventType.name)
                        DataRow("Event ID", telemetry.eventId.takeLast(8))
                        DataRow("Timestamp", java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date(telemetry.timestamp)))
                        telemetry.metadata.forEach { (key, value) ->
                            DataRow(key, value.toString())
                        }
                    }
                }
            } ?: run {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No telemetry data available",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Start security monitoring to begin data collection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStat(
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
            modifier = Modifier.size(20.dp),
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
