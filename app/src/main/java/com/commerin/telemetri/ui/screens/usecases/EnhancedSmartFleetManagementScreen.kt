package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.domain.model.*
import com.commerin.telemetri.ui.components.*
import com.commerin.telemetri.ui.viewmodels.EnhancedSmartFleetViewModel
import com.commerin.telemetri.ui.viewmodels.TelematicsSessionViewModel
import com.commerin.telemetri.ui.viewmodels.LiveAnalytics
import com.commerin.telemetri.ui.viewmodels.PhoneUsageAnalytics
import com.commerin.telemetri.ui.viewmodels.SpeedingAnalytics
import com.commerin.telemetri.ui.viewmodels.BatteryStats
import com.commerin.telemetri.ui.viewmodels.PowerState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Smart Fleet Management Screen with sophisticated driving analytics
 * Showcases multi-sensor phone detection, Kenyan road adaptations, and real-time insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSmartFleetManagementScreen(
    onBackPressed: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    viewModel: EnhancedSmartFleetViewModel = hiltViewModel(),
    sessionViewModel: TelematicsSessionViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val driverState by viewModel.driverState.observeAsState()
    val currentTrip by viewModel.currentTrip.observeAsState()
    val recentEvents by viewModel.recentEvents.observeAsState(emptyList())
    val liveAnalytics by viewModel.liveAnalytics.observeAsState()
    val phoneUsageAnalytics by viewModel.phoneUsageAnalytics.observeAsState()
    val speedingAnalytics by viewModel.speedingAnalytics.observeAsState()
    val powerState by viewModel.powerState.observeAsState()
    val batteryStats by viewModel.batteryStats.observeAsState()
    val currentSpeed by viewModel.currentSpeed.observeAsState(0f)
    val reportStatus by viewModel.reportGenerationStatus.observeAsState()

    // Session management states
    val sessionState by sessionViewModel.sessionState.observeAsState(SessionState.STOPPED)

    LaunchedEffect(Unit) {
        viewModel.initializeEnhancedFleetManagement()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TransparentAppBar(
            title = "Enhanced Fleet Management",
            onBackPressed = onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 120.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Telematics Session Panel
            item {
                EnhancedTelematicsSessionPanel(
                    currentTrip = currentTrip,
                    liveAnalytics = liveAnalytics
                )
            }

            // Enhanced Fleet Control Panel
            item {
                EnhancedFleetControlPanel(
                    isCollecting = isCollecting,
                    onToggleCollection = {
                        if (isCollecting) {
                            viewModel.stopFleetMonitoring()
                        } else {
                            viewModel.startFleetMonitoring()
                        }
                    },
                    batteryStats = batteryStats,
                    sessionState = sessionState,
                    currentSpeed = currentSpeed
                )
            }

            // Real-time Phone Usage Detection Panel
            item {
                PhoneUsageDetectionPanel(
                    phoneUsageAnalytics = phoneUsageAnalytics,
                    isActive = isCollecting
                )
            }

            // Enhanced Driver Status Dashboard
            item {
                EnhancedDriverStatusDashboard(
                    driverState = driverState,
                    isCollecting = isCollecting
                )
            }

            // Kenyan Road Adaptation Panel
            item {
                KenyanRoadAdaptationPanel(
                    speedingAnalytics = speedingAnalytics,
                    isActive = isCollecting
                )
            }

            // Live Event Stream
            item {
                LiveEventStreamPanel(
                    events = recentEvents,
                    isCollecting = isCollecting
                )
            }

            // Power and Performance Panel
            item {
                PowerPerformancePanel(
                    powerState = powerState,
                    batteryStats = batteryStats
                )
            }

            // Enhanced Report Generation Panel
            item {
                EnhancedReportGenerationPanel(
                    onGenerateEventReport = { viewModel.generateEnhancedEventReport() },
                    onGenerateInsuranceReport = { viewModel.generateEnhancedInsuranceReport() },
                    onGeneratePhoneUsageReport = { viewModel.generatePhoneUsageReport() },
                    onGenerateSpeedingReport = { viewModel.generateSpeedingReport() },
                    reportStatus = reportStatus,
                    onNavigateToReports = onNavigateToReports
                )
            }
        }
    }
}

@Composable
private fun EnhancedTelematicsSessionPanel(
    currentTrip: TripSummaryEntity?,
    liveAnalytics: LiveAnalytics?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Trip Session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Enhanced Trip Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Multi-sensor detection active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentTrip != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (currentTrip != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TripMetricItem(
                        label = "Distance",
                        value = "${String.format("%.1f", currentTrip.totalDistance)} km",
                        icon = Icons.Default.Route
                    )
                    TripMetricItem(
                        label = "Duration",
                        value = formatDuration(System.currentTimeMillis() - currentTrip.startTimestamp),
                        icon = Icons.Default.Schedule
                    )
                    TripMetricItem(
                        label = "Score",
                        value = "${currentTrip.overallScore.toInt()}/100",
                        icon = Icons.Default.Star
                    )
                }

                if (liveAnalytics != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = liveAnalytics.currentScore / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = getScoreColor(liveAnalytics.currentScore)
                    )

                    Text(
                        text = "Real-time Score: ${liveAnalytics.currentScore.toInt()}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneUsageDetectionPanel(
    phoneUsageAnalytics: PhoneUsageAnalytics?,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = "Phone Usage Detection",
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone Usage Detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Multi-sensor fusion technology",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Green.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = "Active",
                                tint = Color.Green,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MONITORING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detection Methods Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetectionMethodCard(
                    title = "Hand Movement",
                    weight = "25%",
                    isActive = isActive,
                    confidence = phoneUsageAnalytics?.averageHandMovementScore ?: 0f
                )
                DetectionMethodCard(
                    title = "Driving Disruption",
                    weight = "30%",
                    isActive = isActive,
                    confidence = phoneUsageAnalytics?.averageDrivingDisruptionScore ?: 0f
                )
                DetectionMethodCard(
                    title = "Device Orientation",
                    weight = "20%",
                    isActive = isActive,
                    confidence = 0.75f // Placeholder
                )
            }

            if (phoneUsageAnalytics != null && phoneUsageAnalytics.totalEvents > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PhoneUsageStatItem(
                        label = "Events",
                        value = "${phoneUsageAnalytics.totalEvents}",
                        subText = formatDuration(phoneUsageAnalytics.totalDuration)
                    )
                    PhoneUsageStatItem(
                        label = "Avg Confidence",
                        value = "${(phoneUsageAnalytics.averageConfidence * 100).toInt()}%",
                        subText = "Multi-sensor"
                    )
                    PhoneUsageStatItem(
                        label = "High Confidence",
                        value = "${phoneUsageAnalytics.highConfidenceEvents}",
                        subText = ">90% certain"
                    )
                }
            }
        }
    }
}

@Composable
private fun KenyanRoadAdaptationPanel(
    speedingAnalytics: SpeedingAnalytics?,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = "Kenyan Road Adaptation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kenyan Road Adaptation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Context-aware thresholds for local conditions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kenyan Speed Limits Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RoadTypeCard(
                    roadType = "🏙️ Urban",
                    speedLimit = "50 km/h",
                    threshold = "12+ km/h over",
                    violations = speedingAnalytics?.urbanViolations ?: 0
                )
                RoadTypeCard(
                    roadType = "��️ Rural",
                    speedLimit = "80 km/h",
                    threshold = "15+ km/h over",
                    violations = speedingAnalytics?.ruralViolations ?: 0
                )
                RoadTypeCard(
                    roadType = "🛤️ Highway",
                    speedLimit = "100 km/h",
                    threshold = "20+ km/h over",
                    violations = speedingAnalytics?.highwayViolations ?: 0
                )
            }

            if (speedingAnalytics != null && speedingAnalytics.totalViolations > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Total Violations: ${speedingAnalytics.totalViolations} • Max Speed: +${speedingAnalytics.maxSpeedOver.toInt()} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (speedingAnalytics.totalViolations > 0) Color.Red else Color.Green,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LiveEventStreamPanel(
    events: List<DrivingEventEntity>,
    isCollecting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = "Live Events",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Live Event Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isCollecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = "Live",
                            tint = Color.Red,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventNote,
                            contentDescription = "No Events",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isCollecting) "No events detected yet" else "Start monitoring to see live events",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                events.take(3).forEach { event ->
                    LiveEventItem(event = event)
                    if (event != events.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (events.size > 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${events.size - 3} more events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedReportGenerationPanel(
    onGenerateEventReport: () -> Unit,
    onGenerateInsuranceReport: () -> Unit,
    onGeneratePhoneUsageReport: () -> Unit,
    onGenerateSpeedingReport: () -> Unit,
    reportStatus: String?,
    onNavigateToReports: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Enhanced Reports",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Enhanced Analytics Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onNavigateToReports) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Report generation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = onGeneratePhoneUsageReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Phone Usage", fontSize = 12.sp)
                }

                ElevatedButton(
                    onClick = onGenerateSpeedingReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speeding", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = onGenerateEventReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Events", fontSize = 12.sp)
                }

                ElevatedButton(
                    onClick = onGenerateInsuranceReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Insurance", fontSize = 12.sp)
                }
            }

            // Report status
            reportStatus?.let { status ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedFleetControlPanel(
    isCollecting: Boolean,
    onToggleCollection: () -> Unit,
    batteryStats: BatteryStats?,
    sessionState: SessionState,
    currentSpeed: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        text = "Enhanced Fleet Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCollecting) "Multi-sensor detection active" else "Ready to start monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isCollecting,
                    onCheckedChange = { onToggleCollection() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(
                    label = "Status",
                    value = if (isCollecting) "ACTIVE" else "STOPPED",
                    color = if (isCollecting) Color.Green else Color.Gray
                )
                StatusItem(
                    label = "Speed",
                    value = "${currentSpeed.toInt()} km/h",
                    color = MaterialTheme.colorScheme.primary
                )
                StatusItem(
                    label = "Battery",
                    value = "${batteryStats?.level ?: 0}%",
                    color = when {
                        (batteryStats?.level ?: 0) > 50 -> Color.Green
                        (batteryStats?.level ?: 0) > 20 -> Color(0xFFFF9800)
                        else -> Color.Red
                    }
                )
            }
        }
    }
}

@Composable
private fun EnhancedDriverStatusDashboard(
    driverState: DriverState?,
    isCollecting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Driver Status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Enhanced Driver Detection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (driverState != null && isCollecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DriverMetricItem(
                        label = "Driver Status",
                        value = if (driverState.isDriver) "DETECTED" else "NOT DETECTED",
                        confidence = driverState.confidence
                    )
                    DriverMetricItem(
                        label = "Position",
                        value = driverState.phonePosition.name,
                        confidence = driverState.confidence
                    )
                    DriverMetricItem(
                        label = "Movement",
                        value = driverState.movementPattern.name,
                        confidence = driverState.confidence
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCollecting) "Analyzing driver patterns..." else "Start monitoring to detect driver",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerPerformancePanel(
    powerState: PowerState?,
    batteryStats: BatteryStats?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Battery6Bar,
                    contentDescription = "Power Performance",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Power & Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (batteryStats != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PowerMetricItem(
                        label = "Battery Level",
                        value = "${batteryStats.level}%",
                        icon = Icons.Default.Battery6Bar
                    )
                    PowerMetricItem(
                        label = "Temperature",
                        value = "${batteryStats.temperature.toInt()}°C",
                        icon = Icons.Default.Thermostat
                    )
                    PowerMetricItem(
                        label = "Charging",
                        value = if (batteryStats.isCharging) "YES" else "NO",
                        icon = if (batteryStats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Battery6Bar
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = batteryStats.level / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        batteryStats.level > 50 -> Color.Green
                        batteryStats.level > 20 -> Color(0xFFFF9800)
                        else -> Color.Red
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DriverMetricItem(
    label: String,
    value: String,
    confidence: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PowerMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Supporting composables and data classes

@Composable
private fun TripMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
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
private fun DetectionMethodCard(
    title: String,
    weight: String,
    isActive: Boolean,
    confidence: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isActive) "${(confidence * 100).toInt()}%" else "N/A",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = weight,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhoneUsageStatItem(
    label: String,
    value: String,
    subText: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoadTypeCard(
    roadType: String,
    speedLimit: String,
    threshold: String,
    violations: Int
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (violations > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = roadType,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = speedLimit,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = threshold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$violations violations",
                style = MaterialTheme.typography.bodySmall,
                color = if (violations > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveEventItem(
    event: DrivingEventEntity
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getEventIcon(event.eventType),
            contentDescription = event.eventType,
            tint = getSeverityColor(event.severity),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatEventType(event.eventType),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${event.severity} • ${(event.confidence * 100).toInt()}% confident",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Utility functions (reuse from previous file)
private fun getScoreColor(score: Float): Color {
    return when {
        score >= 80f -> Color(0xFF4CAF50)
        score >= 60f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getSeverityColor(severity: String): Color {
    return when (severity) {
        "CRITICAL" -> Color(0xFFF44336)
        "HIGH" -> Color(0xFFFF5722)
        "MEDIUM" -> Color(0xFFFF9800)
        "LOW" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
}

private fun getEventIcon(eventType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (eventType) {
        "PHONE_USAGE" -> Icons.Default.PhoneAndroid
        "SPEEDING" -> Icons.Default.Speed
        "HARD_BRAKING" -> Icons.Default.Warning
        "RAPID_ACCELERATION" -> Icons.AutoMirrored.Filled.TrendingUp
        "HARSH_CORNERING" -> Icons.Default.TurnRight
        "AGGRESSIVE_DRIVING" -> Icons.Default.Dangerous
        "SMOOTH_DRIVING" -> Icons.Default.Check
        "ECO_DRIVING" -> Icons.Default.Eco
        else -> Icons.AutoMirrored.Filled.EventNote
    }
}

private fun formatEventType(eventType: String): String {
    return when (eventType) {
        "PHONE_USAGE" -> "Phone Usage"
        "HARD_BRAKING" -> "Hard Braking"
        "RAPID_ACCELERATION" -> "Rapid Acceleration"
        "HARSH_CORNERING" -> "Harsh Cornering"
        "AGGRESSIVE_DRIVING" -> "Aggressive Driving"
        "SMOOTH_DRIVING" -> "Smooth Driving"
        "ECO_DRIVING" -> "Eco Driving"
        "SPEEDING" -> "Speeding"
        else -> eventType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
