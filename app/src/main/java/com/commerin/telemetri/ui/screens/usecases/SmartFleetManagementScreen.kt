package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.core.*
import com.commerin.telemetri.domain.model.*
import com.commerin.telemetri.ui.components.*
import com.commerin.telemetri.ui.viewmodels.SmartFleetViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFleetManagementScreen(
    onBackPressed: () -> Unit,
    viewModel: SmartFleetViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val driverState by viewModel.driverState.observeAsState()
    val currentTrip by viewModel.currentTrip.observeAsState()
    val drivingEvents by viewModel.drivingEvents.observeAsState(emptyList())
    val powerState by viewModel.powerState.observeAsState()
    val batteryStats by viewModel.batteryStats.observeAsState()
    val riskScore by viewModel.riskScore.observeAsState()
    val insurancePremium by viewModel.insurancePremium.observeAsState()
    val currentSpeed by viewModel.currentSpeed.observeAsState(0f)
    val reportStatus by viewModel.reportGenerationStatus.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeFleetManagement()
    }

    // Show status messages when reports are generated
    reportStatus?.let { status ->
        LaunchedEffect(status) {
            // You could show a snackbar or toast here
            // For now, we'll just log it (the status is already shown in the UI)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TransparentAppBar(
            title = "Smart Fleet Management",
            onBackPressed = onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 120.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fleet Control Panel
            item {
                FleetControlPanel(
                    isCollecting = isCollecting,
                    onToggleCollection = {
                        if (isCollecting) {
                            viewModel.stopFleetMonitoring()
                        } else {
                            viewModel.startFleetMonitoring()
                        }
                    },
                    batteryStats = batteryStats
                )
            }

            // Driver Status Dashboard
            item {
                DriverStatusDashboard(
                    driverState = driverState,
                    currentTrip = currentTrip,
                    riskScore = riskScore
                )
            }

            // Real-time Events Monitor
            item {
                RealTimeEventsMonitor(
                    events = drivingEvents.take(5), // Show last 5 events
                    currentSpeed = currentSpeed,
                    onViewAllEvents = { viewModel.exportEventReport() }
                )
            }

//            // Power Management Status
//            item {
//                PowerManagementDashboard(
//                    powerState = powerState,
//                    batteryStats = batteryStats,
//                    onOptimizePower = { viewModel.optimizePowerSettings() }
//                )
//            }

            // Insurance Analytics
            item {
                InsuranceAnalyticsDashboard(
                    premium = insurancePremium,
                    riskScore = riskScore,
                    onGenerateReport = { viewModel.generateInsuranceReport() }
                )
            }

            // Fleet Analytics Charts
            item {
                FleetAnalyticsCharts(
                    events = drivingEvents,
                    currentTrip = currentTrip
                )
            }

            // Report Generation Status
            reportStatus?.let { status ->
                item {
                    ReportStatusCard(status = status)
                }
            }

            // Navigation to Reports Screen
            item {
                NavigationCard(
                    onNavigateToReports = {
                        // Navigate to reports screen - you'll need to pass navController
                        // For now, we'll show a placeholder
                    }
                )
            }
        }
    }
}

@Composable
private fun FleetControlPanel(
    isCollecting: Boolean,
    onToggleCollection: () -> Unit,
    batteryStats: TelemetriManager.BatteryOptimizationStats?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
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
                        text = "Fleet Command Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCollecting) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isCollecting) Color.Green else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCollecting) "Advanced Monitoring Active" else "System Standby",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = onToggleCollection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCollecting)
                            MaterialTheme.colorScheme.errorContainer else
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCollecting) "Stop Fleet" else "Start Fleet")
                }
            }

            if (batteryStats != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.Battery6Bar,
                        label = "Battery",
                        value = "${batteryStats.currentBatteryLevel.toInt()}%"
                    )
                    InfoChip(
                        icon = Icons.Default.Speed,
                        label = "Power Mode",
                        value = batteryStats.powerMode
                    )
                    InfoChip(
                        icon = Icons.Default.Sensors,
                        label = "Active Sensors",
                        value = "${batteryStats.activeSensors}"
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverStatusDashboard(
    driverState: DriverState?,
    currentTrip: TripScore?,
    riskScore: Float?
) {
    TelemetryDataCard(
        title = "Driver Status",
        icon = Icons.Default.Person,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        if (driverState != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DataRow("Driver Status", if (driverState.isDriver) "Confirmed Driver" else "Passenger/Unknown")
                    DataRow("Confidence", "${(driverState.confidence * 100).toInt()}%")
                    DataRow("Phone Position", driverState.phonePosition.name.replace("_", " "))
                    DataRow("Movement Pattern", driverState.movementPattern.name.replace("_", " "))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Driver confidence indicator
                    CircularProgressIndicator(
                        progress = driverState.confidence,
                        modifier = Modifier.size(60.dp),
                        color = when {
                            driverState.confidence > 0.8f -> Color.Green
                            driverState.confidence > 0.6f -> Color.Yellow
                            else -> Color.Red
                        },
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Driver\nConfidence",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Driver detection not available - start fleet monitoring",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (currentTrip != null) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Current Trip Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreCard("Overall", currentTrip.overallScore, Color.Blue)
                ScoreCard("Safety", currentTrip.safetyScore, Color.Green)
                ScoreCard("Legal", currentTrip.legalComplianceScore, Color.Red)
            }
        }
    }
}

@Composable
private fun RealTimeEventsMonitor(
    events: List<DrivingEvent>,
    currentSpeed: Float,
    onViewAllEvents: () -> Unit
) {
    TelemetryDataCard(
        title = "Real-Time Event Monitor",
        icon = Icons.Default.Warning,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        // Add speedometer at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Speed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DigitalSpeedometer(
                speed = currentSpeed,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (events.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            events.forEach { event ->
                EventCard(event = event)
                if (event != events.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onViewAllEvents,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View All Events & Generate Report")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Description, contentDescription = null)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No driving events detected yet. Start monitoring to see real-time events.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PowerManagementDashboard(
    powerState: AdaptivePowerManager.PowerState?,
    batteryStats: TelemetriManager.BatteryOptimizationStats?,
    onOptimizePower: () -> Unit
) {
    TelemetryDataCard(
        title = "Adaptive Power Management",
        icon = Icons.Default.PowerSettingsNew,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        if (powerState != null && batteryStats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DataRow("Power Mode", powerState.powerMode.name.replace("_", " "))
                    DataRow("Battery Level", "${powerState.batteryLevel.toInt()}%")
                    DataRow("Estimated Life", "${powerState.estimatedBatteryLife} min")
                    DataRow("Charging", if (powerState.isCharging) "Yes" else "No")
                    DataRow("Thermal State", powerState.thermalState.name)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Battery level indicator
                    CircularProgressIndicator(
                        progress = powerState.batteryLevel / 100f,
                        modifier = Modifier.size(60.dp),
                        color = when {
                            powerState.batteryLevel > 50f -> Color.Green
                            powerState.batteryLevel > 20f -> Color.Yellow
                            else -> Color.Red
                        },
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Battery\nLevel",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOptimizePower,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Optimize Power Settings")
            }
        } else {
            Text(
                text = "Power management data not available",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsuranceAnalyticsDashboard(
    premium: RiskAssessmentEngine.InsurancePremiumEstimate?,
    riskScore: Float?,
    onGenerateReport: () -> Unit
) {
    TelemetryDataCard(
        title = "Insurance Analytics",
        icon = Icons.Default.Shield,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        if (premium != null) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DataRow("Base Premium", "$${premium.basePremium.toInt()}/year")
                        DataRow("Risk Multiplier", String.format("%.2fx", premium.riskMultiplier))
                        DataRow("Estimated Premium", "$${premium.estimatedPremium.toInt()}/year")
                        DataRow("Discount Eligible", if (premium.discountEligible) "Yes" else "No")
                        if (premium.discountEligible) {
                            DataRow("Discount", "${premium.discountPercentage.toInt()}%")
                        }
                    }

                    if (riskScore != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                            progress = { (100 - riskScore) / 100f },
                            modifier = Modifier.size(60.dp),
                            color = when {
                                                                riskScore < 20f -> Color.Green
                                                                riskScore < 50f -> Color.Yellow
                                                                else -> Color.Red
                                                            },
                            strokeWidth = 6.dp,
                            trackColor = ProgressIndicatorDefaults.circularTrackColor,
                            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Safety\nScore",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (premium.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    premium.recommendations.take(2).forEach { recommendation ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Icon(
                                Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGenerateReport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Insurance Report")
                }
            }
        } else {
            Text(
                text = "Insurance analytics will appear after sufficient driving data is collected",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FleetAnalyticsCharts(
    events: List<DrivingEvent>,
    currentTrip: TripScore?
) {
    TelemetryDataCard(
        title = "Fleet Analytics",
        icon = Icons.Default.Analytics,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        if (events.isNotEmpty()) {
            // Event distribution chart
            val eventCounts = events.groupBy { it.eventType }.mapValues { it.value.size }

            Text(
                text = "Event Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            eventCounts.forEach { (eventType, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = eventType.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(text = "$count")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (eventType) {
                                DrivingEventType.HARD_BRAKING, DrivingEventType.SPEEDING -> Color.Red.copy(
                                    alpha = 0.2f
                                )
                                DrivingEventType.SMOOTH_DRIVING, DrivingEventType.ECO_DRIVING -> Color.Green.copy(
                                    alpha = 0.2f
                                )
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(
                text = "Analytics charts will appear as driving data is collected",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(event: DrivingEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.severity) {
                EventSeverity.CRITICAL -> Color.Red.copy(alpha = 0.1f)
                EventSeverity.HIGH -> Color.Red.copy(alpha = 0.1f)
                EventSeverity.MEDIUM -> Color.Yellow.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (event.eventType) {
                    DrivingEventType.HARD_BRAKING -> Icons.Default.PauseCircle
                    DrivingEventType.RAPID_ACCELERATION -> Icons.Default.Speed
                    DrivingEventType.HARSH_CORNERING -> Icons.Default.Rotate90DegreesCcw
                    DrivingEventType.SPEEDING -> Icons.Default.Warning
                    DrivingEventType.PHONE_USAGE -> Icons.Default.PhoneAndroid
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (event.severity) {
                    EventSeverity.CRITICAL -> Color.Red
                    EventSeverity.HIGH -> Color.Red
                    EventSeverity.MEDIUM -> Color.Yellow
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${event.severity.name} • ${String.format(Locale.getDefault(), "%.1f", event.speed * 3.6f)} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatTime(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DigitalSpeedometer(speed: Float, modifier: Modifier = Modifier) {
    val speedText = String.format(Locale.getDefault(), "%03d", speed.toInt().coerceIn(0, 999))

    Card(
        modifier = modifier
            .height(80.dp)
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 7-segment style speed display
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                speedText.forEach { digit ->
                    SevenSegmentDigit(
                        digit = digit,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Unit label
            Text(
                text = "KM/H",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Red,
                    fontSize = 10.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SevenSegmentDigit(digit: Char, modifier: Modifier = Modifier) {
    val segments = getSevenSegmentPattern(digit)

    Box(
        modifier = modifier
            .width(20.dp)
            .height(32.dp)
    ) {
        // Top horizontal segment
        HorizontalSegment(
            isActive = segments[0],
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 1.dp)
        )

        // Top-left vertical segment
        VerticalSegment(
            isActive = segments[1],
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 1.dp, y = 3.dp)
        )

        // Top-right vertical segment
        VerticalSegment(
            isActive = segments[2],
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-1).dp, y = 3.dp)
        )

        // Middle horizontal segment
        HorizontalSegment(
            isActive = segments[3],
            modifier = Modifier
                .align(Alignment.Center)
        )

        // Bottom-left vertical segment
        VerticalSegment(
            isActive = segments[4],
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 1.dp, y = (-3).dp)
        )

        // Bottom-right vertical segment
        VerticalSegment(
            isActive = segments[5],
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-1).dp, y = (-3).dp)
        )

        // Bottom horizontal segment
        HorizontalSegment(
            isActive = segments[6],
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-1).dp)
        )
    }
}

@Composable
private fun HorizontalSegment(isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(14.dp)
            .height(2.dp)
            .background(
                color = if (isActive) Color.Red else Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(1.dp)
            )
    )
}

@Composable
private fun VerticalSegment(isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(2.dp)
            .height(12.dp)
            .background(
                color = if (isActive) Color.Red else Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(1.dp)
            )
    )
}

private fun getSevenSegmentPattern(digit: Char): BooleanArray {
    return when (digit) {
        '0' -> booleanArrayOf(true, true, true, false, true, true, true)
        '1' -> booleanArrayOf(false, false, true, false, false, true, false)
        '2' -> booleanArrayOf(true, false, true, true, true, false, true)
        '3' -> booleanArrayOf(true, false, true, true, false, true, true)
        '4' -> booleanArrayOf(false, true, true, true, false, true, false)
        '5' -> booleanArrayOf(true, true, false, true, false, true, true)
        '6' -> booleanArrayOf(true, true, false, true, true, true, true)
        '7' -> booleanArrayOf(true, false, true, false, false, true, false)
        '8' -> booleanArrayOf(true, true, true, true, true, true, true)
        '9' -> booleanArrayOf(true, true, true, true, false, true, true)
        else -> booleanArrayOf(false, false, false, false, false, false, false) // blank
    }
}

@Composable
private fun ScoreCard(label: String, score: Float, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${score.toInt()}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND)
    )
}
