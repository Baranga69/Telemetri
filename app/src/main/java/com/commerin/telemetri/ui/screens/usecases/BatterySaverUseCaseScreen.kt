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
import com.commerin.telemetri.ui.viewmodels.BatterySaverViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatterySaverUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: BatterySaverViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val batteryInfo by viewModel.batteryInfo.observeAsState()
    val powerSavingStats by viewModel.powerSavingStats.observeAsState()
    val sensorData by viewModel.sensorData.observeAsState()
    val deviceState by viewModel.deviceState.observeAsState()

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
                text = "Battery Optimized",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Status
        batteryInfo?.let { battery ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        battery.level > 75 -> Color(0xFFE8F5E8).copy(alpha = 0.9f) // Soft pastel green
                        battery.level > 50 -> Color(0xFFFFF8E1).copy(alpha = 0.9f) // Soft pastel amber
                        battery.level > 25 -> Color(0xFFFFF3E0).copy(alpha = 0.9f) // Soft pastel orange
                        else -> Color(0xFFFFEBEE).copy(alpha = 0.9f) // Soft pastel red
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            battery.chargingState.name == "CHARGING" -> Icons.Default.BatteryChargingFull
                            battery.level > 75 -> Icons.Default.BatteryFull
                            battery.level > 50 -> Icons.Default.Battery6Bar
                            battery.level > 25 -> Icons.Default.Battery3Bar
                            else -> Icons.Default.Battery1Bar
                        },
                        contentDescription = null,
                        tint = when {
                            battery.level > 75 -> Color(0xFF388E3C)
                            battery.level > 50 -> Color(0xFFF57C00) // Deep amber
                            battery.level > 25 -> Color(0xFFE65100)
                            else -> Color(0xFFD32F2F)
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Battery: ${battery.level}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                battery.level > 75 -> Color(0xFF388E3C)
                                battery.level > 50 -> Color(0xFFF57C00)
                                battery.level > 25 -> Color(0xFFE65100)
                                else -> Color(0xFFD32F2F)
                            }
                        )
                        Text(
                            text = "${battery.chargingState.name} • ${String.format(Locale.US, "%.1f", battery.temperature)}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8E1).copy(alpha = 0.8f) // Soft pastel amber
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
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
                            text = "Power Optimized Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100) // Deep orange/amber for text
                        )
                        Text(
                            text = if (isCollecting) "Battery-friendly data collection" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFFF57C00) else Color(0xFF6B7280)
                        )
                    }

                    Button(
                        onClick = {
                            if (isCollecting) {
                                viewModel.stopCollection()
                            } else {
                                viewModel.startBatterySaverCollection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCollecting) Color(0xFFFFCDD2) else Color(0xFFFFE0B2), // Pastel red/amber
                            contentColor = if (isCollecting) Color(0xFFD32F2F) else Color(0xFFE65100)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.BatteryStd,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCollecting) "Stop" else "Start Eco Mode")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power Saving Statistics
        powerSavingStats?.let { stats ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PowerStat("Power Saved", "${stats.powerSavedPercent}%", Icons.Default.Eco)
                    PowerStat("Active Sensors", "${stats.activeSensorsCount}", Icons.Default.Sensors)
                    PowerStat("Sample Rate", "${stats.averageSampleRate}Hz", Icons.Default.Timeline)
                    PowerStat("Efficiency", stats.efficiencyScore, Icons.Default.BatteryChargingFull)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optimized Telemetry Display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Core Sensors (Essential only)
            item {
                TelemetryDataCard(
                    title = "Core Sensors (Power Optimized)",
                    icon = Icons.Default.Sensors,
                    color = Color(0xFF4CAF50),
                    isActive = !sensorData.isNullOrEmpty()
                ) {
                    val sensors = sensorData
                    if (!sensors.isNullOrEmpty()) {
                        Text(
                            text = "Collecting essential sensor data with adaptive sampling",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        sensors.takeLast(3).forEach { sensor ->
                            when (sensor.sensorType) {
                                com.commerin.telemetri.domain.model.SensorType.ACCELEROMETER -> {
                                    DataRow("Accelerometer", "X:${String.format(Locale.getDefault(), "%.2f", sensor.x)} Y:${String.format(Locale.getDefault(), "%.2f", sensor.y)} Z:${String.format(Locale.getDefault(), "%.2f", sensor.z)}")
                                }
                                com.commerin.telemetri.domain.model.SensorType.GYROSCOPE -> {
                                    DataRow("Gyroscope", "X:${String.format(Locale.getDefault(), "%.2f", sensor.x)} Y:${String.format(Locale.getDefault(), "%.2f", sensor.y)} Z:${String.format(Locale.getDefault(), "%.2f", sensor.z)}")
                                }
                                com.commerin.telemetri.domain.model.SensorType.MAGNETOMETER -> {
                                    DataRow("Magnetometer", "X:${String.format(Locale.getDefault(), "%.2f", sensor.x)} Y:${String.format(Locale.getDefault(), "%.2f", sensor.y)} Z:${String.format(Locale.getDefault(), "%.2f", sensor.z)}")
                                }
                                else -> {
                                    // Only show essential sensors in battery saver mode
                                }
                            }
                        }
                    } else {
                        Text("No sensor data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Device Health Monitoring
            item {
                TelemetryDataCard(
                    title = "Device Health Monitoring",
                    icon = Icons.Default.HealthAndSafety,
                    color = Color(0xFFFF9800),
                    isActive = deviceState != null
                ) {
                    deviceState?.let { device ->
                        DataRow("Battery Level", "${device.batteryLevel}%")
                        DataRow("Battery Temperature", "${String.format(Locale.getDefault(), "%.1f", device.batteryTemperature)}°C")
                        DataRow("Charging Status", if (device.isCharging) "Charging (${device.chargingType})" else "Not Charging")
                        DataRow("Memory Pressure", "${device.memoryUsage}MB used")
                        DataRow("Network", device.networkType)
                        DataRow("Screen", if (device.isScreenOn) "On (${device.screenBrightness}%)" else "Off")
                    } ?: run {
                        Text("No device state data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Power Optimization Features
            item {
                TelemetryDataCard(
                    title = "Power Optimization Features",
                    icon = Icons.Default.BatchPrediction,
                    color = Color(0xFF4CAF50),
                    isActive = true
                ) {
                    Column {
                        PowerFeatureRow("Adaptive Sampling", "Adjusts frequency based on activity", true)
                        PowerFeatureRow("Sensor Fusion", "Combines data from fewer sensors", true)
                        PowerFeatureRow("Background Optimization", "Reduces processing during idle", true)
                        PowerFeatureRow("Smart Scheduling", "Collects data at optimal intervals", true)
                        PowerFeatureRow("Location Limiting", "Minimal GPS usage", true)
                        PowerFeatureRow("Audio Analysis", "Disabled to save power", false)
                        PowerFeatureRow("Network Monitoring", "Disabled to save power", false)
                        PowerFeatureRow("Performance Tracking", "Disabled to save power", false)
                    }
                }
            }

            // Battery Optimization Tips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Optimization Tips",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("• Uses only essential sensors (accelerometer, gyroscope, magnetometer)", style = MaterialTheme.typography.bodySmall)
                        Text("• Adaptive sampling rates based on device activity", style = MaterialTheme.typography.bodySmall)
                        Text("• Minimal location updates (every 5 minutes)", style = MaterialTheme.typography.bodySmall)
                        Text("• Disables power-hungry features (audio, network monitoring)", style = MaterialTheme.typography.bodySmall)
                        Text("• Intelligent background processing optimization", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerStat(
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

@Composable
private fun PowerFeatureRow(
    feature: String,
    description: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) Color(0xFF4CAF50) else Color(0xFF757575)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
