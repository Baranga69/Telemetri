package com.commerin.telemetri.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.ui.components.TransparentAppBar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Reports Screen with sophisticated driving event analytics
 * Showcases phone usage detection, speeding analysis, and Kenyan road adaptations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedReportsScreen(
    onBackPressed: () -> Unit,
    viewModel: EnhancedReportsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tripSummaries by viewModel.tripSummaries.observeAsState(emptyList())
    val drivingEvents by viewModel.drivingEvents.observeAsState(emptyList())
    val phoneUsageEvents by viewModel.phoneUsageEvents.observeAsState(emptyList())
    val speedingEvents by viewModel.speedingEvents.observeAsState(emptyList())
    val drivingStatistics by viewModel.drivingStatistics.observeAsState()
    val phoneUsageAnalytics by viewModel.phoneUsageAnalytics.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.loadEnhancedReports()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TransparentAppBar(
            title = "Enhanced Analytics & Reports",
            onBackPressed = onBackPressed,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        ) {
            // Enhanced Tab Row with new analytics tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Trip Analytics") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Phone Usage") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Speeding Analysis") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Event Details") }
                )
            }

            // Enhanced content based on selected tab
            when (selectedTab) {
                0 -> TripAnalyticsTab(
                    tripSummaries = tripSummaries,
                    drivingStatistics = drivingStatistics,
                    onViewTripDetails = { viewModel.viewTripDetails(it) }
                )
                1 -> PhoneUsageAnalyticsTab(
                    phoneUsageEvents = phoneUsageEvents,
                    phoneUsageAnalytics = phoneUsageAnalytics,
                    onViewEventDetails = { viewModel.viewEventDetails(it) }
                )
                2 -> SpeedingAnalysisTab(
                    speedingEvents = speedingEvents,
                    onViewEventDetails = { viewModel.viewEventDetails(it) },
                    onFilterByRoadType = { viewModel.filterSpeedingByRoadType(it) }
                )
                3 -> EnhancedEventDetailsTab(
                    drivingEvents = drivingEvents,
                    onFilterByType = { viewModel.filterEventsByType(it) },
                    onFilterBySeverity = { viewModel.filterEventsBySeverity(it) }
                )
            }
        }
    }
}

@Composable
private fun TripAnalyticsTab(
    tripSummaries: List<TripSummaryEntity>,
    drivingStatistics: com.commerin.telemetri.data.repository.DrivingStatistics?,
    onViewTripDetails: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Statistics Card
        drivingStatistics?.let { stats ->
            item {
                DrivingStatisticsCard(statistics = stats)
            }
        }

        // Trip Performance Overview
        item {
            Text(
                text = "Recent Trips",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (tripSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Trip Data",
                    description = "Start driving with telematics enabled to see trip analytics here.",
                    icon = Icons.Default.DirectionsCar
                )
            }
        } else {
            items(tripSummaries) { trip ->
                TripSummaryCard(
                    trip = trip,
                    onViewDetails = { onViewTripDetails(trip.tripId) }
                )
            }
        }
    }
}

@Composable
private fun PhoneUsageAnalyticsTab(
    phoneUsageEvents: List<DrivingEventEntity>,
    phoneUsageAnalytics: com.commerin.telemetri.data.repository.PhoneUsageAnalytics?,
    onViewEventDetails: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Phone Usage Analytics Overview
        phoneUsageAnalytics?.let { analytics ->
            item {
                PhoneUsageAnalyticsCard(analytics = analytics)
            }
        }

        // Detection Method Breakdown
        item {
            PhoneUsageDetectionMethodsCard(events = phoneUsageEvents)
        }

        // Individual Events
        item {
            Text(
                text = "Phone Usage Events",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (phoneUsageEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Phone Usage Detected",
                    description = "Our multi-sensor detection system will identify phone usage patterns during driving.",
                    icon = Icons.Default.PhoneAndroid
                )
            }
        } else {
            items(phoneUsageEvents) { event ->
                PhoneUsageEventCard(
                    event = event,
                    onViewDetails = { onViewEventDetails(event.eventId) }
                )
            }
        }
    }
}

@Composable
private fun SpeedingAnalysisTab(
    speedingEvents: List<DrivingEventEntity>,
    onViewEventDetails: (String) -> Unit,
    onFilterByRoadType: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kenyan Road Type Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onFilterByRoadType("ALL") },
                    label = { Text("All Roads") },
                    selected = false
                )
                FilterChip(
                    onClick = { onFilterByRoadType("URBAN") },
                    label = { Text("Urban (50km/h)") },
                    selected = false
                )
                FilterChip(
                    onClick = { onFilterByRoadType("RURAL") },
                    label = { Text("Rural (80km/h)") },
                    selected = false
                )
                FilterChip(
                    onClick = { onFilterByRoadType("HIGHWAY") },
                    label = { Text("Highway (100km/h)") },
                    selected = false
                )
            }
        }

        // Speeding Statistics by Road Type
        item {
            SpeedingByRoadTypeCard(events = speedingEvents)
        }

        // Individual Speeding Events
        item {
            Text(
                text = "Speeding Violations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (speedingEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Speeding Violations",
                    description = "Great driving! Our system monitors speed limits adapted for Kenyan roads.",
                    icon = Icons.Default.Speed
                )
            }
        } else {
            items(speedingEvents) { event ->
                SpeedingEventCard(
                    event = event,
                    onViewDetails = { onViewEventDetails(event.eventId) }
                )
            }
        }
    }
}

@Composable
private fun EnhancedEventDetailsTab(
    drivingEvents: List<DrivingEventEntity>,
    onFilterByType: (String) -> Unit,
    onFilterBySeverity: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Controls
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Filter by Event Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        onClick = { onFilterByType("ALL") },
                        label = { Text("All") },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onFilterByType("PHONE_USAGE") },
                        label = { Text("Phone Usage") },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onFilterByType("SPEEDING") },
                        label = { Text("Speeding") },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onFilterByType("HARD_BRAKING") },
                        label = { Text("Hard Braking") },
                        selected = false
                    )
                }

                Text(
                    text = "Filter by Severity",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { onFilterBySeverity("CRITICAL") },
                        label = { Text("Critical") },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onFilterBySeverity("HIGH") },
                        label = { Text("High") },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onFilterBySeverity("MEDIUM") },
                        label = { Text("Medium") },
                        selected = false
                    )
                }
            }
        }

        // Events List
        if (drivingEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Events Found",
                    description = "Adjust your filters or start a trip to see driving events.",
                    icon = Icons.Default.EventNote
                )
            }
        } else {
            items(drivingEvents) { event ->
                EnhancedDrivingEventCard(event = event)
            }
        }
    }
}

// Enhanced Card Components for displaying sophisticated data

@Composable
private fun DrivingStatisticsCard(
    statistics: com.commerin.telemetri.data.repository.DrivingStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Driving Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Trips",
                    value = "${statistics.totalTrips}",
                    icon = Icons.Default.DirectionsCar
                )
                StatisticItem(
                    label = "Distance",
                    value = "${String.format("%.1f", statistics.totalDistance)} km",
                    icon = Icons.Default.Route
                )
                StatisticItem(
                    label = "Avg Score",
                    value = "${statistics.averageScore.toInt()}/100",
                    icon = Icons.Default.Star
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Phone Usage",
                    value = "${statistics.phoneUsageEventCount}",
                    icon = Icons.Default.PhoneAndroid,
                    color = if (statistics.phoneUsageEventCount > 0) Color.Red else Color.Green
                )
                StatisticItem(
                    label = "Speeding",
                    value = "${statistics.speedingEventCount}",
                    icon = Icons.Default.Speed,
                    color = if (statistics.speedingEventCount > 0) Color.Red else Color.Green
                )
                StatisticItem(
                    label = "Critical",
                    value = "${statistics.criticalEventCount}",
                    icon = Icons.Default.Warning,
                    color = if (statistics.criticalEventCount > 0) Color.Red else Color.Green
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TripSummaryCard(
    trip: TripSummaryEntity,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(trip.startTimestamp)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.1f", trip.totalDistance)} km • ${formatDuration(trip.totalDuration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Overall Score Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = getScoreColor(trip.overallScore)
                ) {
                    Text(
                        text = "${trip.overallScore.toInt()}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enhanced Score Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem("Safety", trip.safetyScore)
                ScoreItem("Legal", trip.legalComplianceScore)
                ScoreItem("Smooth", trip.smoothnessScore)
                ScoreItem("Efficiency", trip.efficiencyScore)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Event Summary with enhanced categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EventCountChip("📱", trip.phoneUsageEventCount, "Phone Usage")
                EventCountChip("🏃", trip.speedingEventCount, "Speeding")
                EventCountChip("🛑", trip.hardBrakingEventCount, "Hard Braking")
                EventCountChip("⚠️", trip.criticalEventCount, "Critical")
            }
        }
    }
}

@Composable
private fun PhoneUsageAnalyticsCard(
    analytics: com.commerin.telemetri.data.repository.PhoneUsageAnalytics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = "Phone Usage",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phone Usage Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticsItem(
                    label = "Total Events",
                    value = "${analytics.totalEvents}",
                    subValue = formatDuration(analytics.totalDuration)
                )
                AnalyticsItem(
                    label = "Avg Confidence",
                    value = "${(analytics.averageConfidence * 100).toInt()}%",
                    subValue = "Multi-sensor"
                )
                AnalyticsItem(
                    label = "High Confidence",
                    value = "${analytics.highConfidenceEvents}",
                    subValue = ">90% sure"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detection method scores
            Text(
                text = "Detection Quality",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = analytics.averageHandMovementScore,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Hand Movement: ${(analytics.averageHandMovementScore * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = analytics.averageDrivingDisruptionScore,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Driving Disruption: ${(analytics.averageDrivingDisruptionScore * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PhoneUsageDetectionMethodsCard(
    events: List<DrivingEventEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detection Methods Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val avgHandMovement = events.mapNotNull { it.handMovementScore }.average().toFloat()
            val avgDrivingDisruption = events.mapNotNull { it.drivingDisruptionScore }.average().toFloat()
            val avgOrientation = events.mapNotNull { it.orientationChangeScore }.average().toFloat()
            val avgAudio = events.mapNotNull { it.audioPatternScore }.average().toFloat()
            val avgSpeed = events.mapNotNull { it.speedCorrelationScore }.average().toFloat()

            DetectionMethodItem("🤚 Hand Movement", avgHandMovement, "25% weight")
            DetectionMethodItem("🚗 Driving Disruption", avgDrivingDisruption, "30% weight")
            DetectionMethodItem("📱 Device Orientation", avgOrientation, "20% weight")
            DetectionMethodItem("🎵 Audio Patterns", avgAudio, "15% weight")
            DetectionMethodItem("⚡ Speed Correlation", avgSpeed, "10% weight")
        }
    }
}

@Composable
private fun SpeedingByRoadTypeCard(
    events: List<DrivingEventEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Speeding by Kenyan Road Types",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val urbanCount = events.count { it.speedingThresholdType == "URBAN" }
            val ruralCount = events.count { it.speedingThresholdType == "RURAL" }
            val highwayCount = events.count { it.speedingThresholdType == "HIGHWAY" }

            val maxSpeedUrban = events.filter { it.speedingThresholdType == "URBAN" }
                .maxOfOrNull { it.speedOverLimit ?: 0f } ?: 0f
            val maxSpeedRural = events.filter { it.speedingThresholdType == "RURAL" }
                .maxOfOrNull { it.speedOverLimit ?: 0f } ?: 0f
            val maxSpeedHighway = events.filter { it.speedingThresholdType == "HIGHWAY" }
                .maxOfOrNull { it.speedOverLimit ?: 0f } ?: 0f

            RoadTypeSpeedingItem("🏙️ Urban Roads", "50 km/h limit", urbanCount, maxSpeedUrban)
            RoadTypeSpeedingItem("🛣️ Rural Roads", "80 km/h limit", ruralCount, maxSpeedRural)
            RoadTypeSpeedingItem("🛤️ Highways", "100 km/h limit", highwayCount, maxSpeedHighway)
        }
    }
}

@Composable
private fun PhoneUsageEventCard(
    event: DrivingEventEntity,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = "Phone Usage",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone Usage Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Confidence Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getConfidenceColor(event.confidence)
                ) {
                    Text(
                        text = "${(event.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detection scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetectionScore("Hand", event.handMovementScore)
                DetectionScore("Driving", event.drivingDisruptionScore)
                DetectionScore("Orient", event.orientationChangeScore)
                DetectionScore("Audio", event.audioPatternScore)
            }

            if (event.duration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration: ${formatDuration(event.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedingEventCard(
    event: DrivingEventEntity,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Speeding",
                            tint = getSeverityColor(event.severity),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${event.speedOverLimit?.toInt() ?: 0} km/h over limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${getRoadTypeDisplay(event.speedingThresholdType)} • ${event.severity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Speed Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getSeverityColor(event.severity)
                ) {
                    Text(
                        text = "+${event.speedOverLimit?.toInt() ?: 0}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            event.speedingDuration?.let { duration ->
                if (duration > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Duration: ${formatDuration(duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedDrivingEventCard(
    event: DrivingEventEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getEventIcon(event.eventType),
                            contentDescription = event.eventType,
                            tint = getSeverityColor(event.severity),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatEventType(event.eventType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${event.severity} • Confidence: ${(event.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                            .format(Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Severity Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getSeverityColor(event.severity)
                ) {
                    Text(
                        text = event.severity,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Additional context based on event type
            when (event.eventType) {
                "PHONE_USAGE" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Multi-sensor detection: Hand movement, driving disruption, device orientation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                "SPEEDING" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Road: ${getRoadTypeDisplay(event.speedingThresholdType)} • Speed: +${event.speedOverLimit?.toInt() ?: 0} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    if (event.magnitude > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Magnitude: ${String.format("%.1f", event.magnitude)} m/s²",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper functions and smaller components

@Composable
private fun ScoreItem(label: String, score: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${score.toInt()}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = getScoreColor(score)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventCountChip(emoji: String, count: Int, label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (count > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AnalyticsItem(label: String, value: String, subValue: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetectionMethodItem(name: String, score: Float, weight: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = weight,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${(score * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoadTypeSpeedingItem(roadType: String, speedLimit: String, count: Int, maxSpeed: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roadType,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = speedLimit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$count violations",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) Color.Red else Color.Green
            )
            if (maxSpeed > 0) {
                Text(
                    text = "Max: +${maxSpeed.toInt()} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetectionScore(label: String, score: Float?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (score != null) "${(score * 100).toInt()}%" else "N/A",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (score != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Utility functions for colors and formatting

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 80f -> Color(0xFF4CAF50) // Green
        score >= 60f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getSeverityColor(severity: String): Color {
    return when (severity) {
        "CRITICAL" -> Color(0xFFF44336) // Red
        "HIGH" -> Color(0xFFFF5722) // Deep Orange
        "MEDIUM" -> Color(0xFFFF9800) // Orange
        "LOW" -> Color(0xFF4CAF50) // Green
        else -> Color.Gray
    }
}

private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.9f -> Color(0xFF4CAF50) // Green
        confidence >= 0.8f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getEventIcon(eventType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (eventType) {
        "PHONE_USAGE" -> Icons.Default.PhoneAndroid
        "SPEEDING" -> Icons.Default.Speed
        "HARD_BRAKING" -> Icons.Default.Warning
        "RAPID_ACCELERATION" -> Icons.Default.TrendingUp
        "HARSH_CORNERING" -> Icons.Default.TurnRight
        "AGGRESSIVE_DRIVING" -> Icons.Default.Dangerous
        "SMOOTH_DRIVING" -> Icons.Default.Check
        "ECO_DRIVING" -> Icons.Default.Eco
        else -> Icons.Default.EventNote
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

private fun getRoadTypeDisplay(speedingThresholdType: String?): String {
    return when (speedingThresholdType) {
        "URBAN" -> "Urban (50 km/h)"
        "RURAL" -> "Rural (80 km/h)"
        "HIGHWAY" -> "Highway (100 km/h)"
        else -> "Unknown Road"
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
