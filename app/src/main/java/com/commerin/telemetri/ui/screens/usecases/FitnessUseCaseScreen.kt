package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.commerin.telemetri.ui.viewmodels.FitnessViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessUseCaseScreen(
    onBackPressed: () -> Unit,
    viewModel: FitnessViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val motionData by viewModel.motionData.observeAsState()
    val locationData by viewModel.locationData.observeAsState()
    val performanceData by viewModel.performanceData.observeAsState()
    val activityStats by viewModel.activityStats.observeAsState()

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
                text = "Fitness Tracking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8).copy(alpha = 0.8f) // Soft pastel green
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            text = "Activity Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D3A) // Deeper green for text
                        )
                        Text(
                            text = if (isCollecting) "Monitoring movement and biometrics" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFF4CAF50) else Color(0xFF6B7280)
                        )
                    }

                    Button(
                        onClick = {
                            if (isCollecting) {
                                viewModel.stopCollection()
                            } else {
                                viewModel.startFitnessCollection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCollecting) Color(0xFFFFCDD2) else Color(0xFFC8E6C9), // Pastel red/green
                            contentColor = if (isCollecting) Color(0xFFD32F2F) else Color(0xFF388E3C)
                        )
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.FitnessCenter,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCollecting) "Stop" else "Start Workout")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Stats Overview
        activityStats?.let { stats ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Steps", "${stats.stepCount}", Icons.AutoMirrored.Filled.DirectionsWalk)
                    StatCard("Distance", "${String.format(Locale.getDefault(), "%.2f", stats.distance)} km", Icons.Default.Route)
                    StatCard("Calories", "${stats.calories}", Icons.Default.LocalFireDepartment)
                    StatCard("Duration", "${stats.duration} min", Icons.Default.Timer)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Data Display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Motion Analysis
            item {
                TelemetryDataCard(
                    title = "Motion Analysis",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    color = Color(0xFF4CAF50),
                    isActive = motionData != null
                ) {
                    motionData?.let { motion ->
                        DataRow("Activity", motion.activityType.name)
                        DataRow("Confidence", "${String.format(Locale.getDefault(), "%.0f", motion.confidence * 100)}%")
                        DataRow("Step Count", "${motion.stepCount}")
                        DataRow("Step Frequency", "${String.format(Locale.getDefault(), "%.1f", motion.stepFrequency)} /min")
                        DataRow("Acceleration", "${String.format(Locale.getDefault(), "%.2f", motion.accelerationMagnitude)} m/s²")
                        DataRow("Gyroscope", "${String.format(Locale.getDefault(), "%.2f", motion.gyroscopeMagnitude)} rad/s")
                    } ?: run {
                        Text("No motion data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Route Tracking
            item {
                TelemetryDataCard(
                    title = "Route Tracking",
                    icon = Icons.Default.Route,
                    color = Color(0xFF2196F3),
                    isActive = locationData != null
                ) {
                    locationData?.let { location ->
                        DataRow("Current Speed", "${String.format(Locale.getDefault(), "%.1f", (location.speed ?: 0f) * 3.6)} km/h")
                        DataRow("Altitude", "${String.format(Locale.getDefault(), "%.0f", location.altitude ?: 0.0)} m")
                        DataRow("GPS Accuracy", "${String.format(Locale.getDefault(), "%.0f", location.accuracy ?: 0f)} m")
                        DataRow("Bearing", "${String.format(Locale.getDefault(), "%.0f", location.bearing ?: 0f)}°")
                        DataRow("Coordinates", "${String.format(Locale.getDefault(), "%.6f", location.latitude)}, ${String.format(Locale.getDefault(), "%.6f", location.longitude)}")
                    } ?: run {
                        Text("No location data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Device Performance
            item {
                TelemetryDataCard(
                    title = "Device Health",
                    icon = Icons.Default.MonitorHeart,
                    color = Color(0xFFF44336),
                    isActive = performanceData != null
                ) {
                    performanceData?.let { performance ->
                        DataRow("Battery Level", "${performance.batteryInfo.level}%")
                        DataRow("Battery Temp", "${String.format(Locale.getDefault(), "%.1f", performance.batteryInfo.temperature)}°C")
                        DataRow("Memory Usage", performance.memoryUsage.memoryPressure)
                        DataRow("CPU Usage", "${String.format(Locale.getDefault(), "%.1f", performance.cpuUsage)}%")
                        DataRow("Thermal State", performance.thermalState.thermalState)
                    } ?: run {
                        Text("No performance data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
