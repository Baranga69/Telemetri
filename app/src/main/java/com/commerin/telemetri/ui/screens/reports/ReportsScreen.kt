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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.data.database.EventReportEntity
import com.commerin.telemetri.data.database.EventSummaryEntity
import com.commerin.telemetri.data.database.InsuranceReportEntity
import com.commerin.telemetri.ui.components.TransparentAppBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBackPressed: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val eventReports by viewModel.eventReports.observeAsState(emptyList())
    val insuranceReports by viewModel.insuranceReports.observeAsState(emptyList())
    val eventSummary by viewModel.eventSummary.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TransparentAppBar(
            title = "Reports & Analytics",
            onBackPressed = onBackPressed
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Event Reports") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Insurance Reports") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Event Summary") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> EventReportsTab(
                    reports = eventReports,
                    onViewReport = { viewModel.viewReport(it) },
                    onDeleteReport = { viewModel.deleteEventReport(it) }
                )
                1 -> InsuranceReportsTab(
                    reports = insuranceReports,
                    onViewReport = { viewModel.viewInsuranceReport(it) },
                    onDeleteReport = { viewModel.deleteInsuranceReport(it) }
                )
                2 -> EventSummaryTab(
                    events = eventSummary,
                    onFilterByType = { viewModel.filterEventsByType(it) },
                    onClearFilter = { viewModel.clearEventFilter() }
                )
            }
        }
    }
}

@Composable
private fun EventReportsTab(
    reports: List<EventReportEntity>,
    onViewReport: (EventReportEntity) -> Unit,
    onDeleteReport: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reports.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Event Reports",
                    description = "Generate event reports from the Smart Fleet Management screen to see them here.",
                    icon = Icons.Default.Description
                )
            }
        } else {
            items(reports) { report ->
                EventReportCard(
                    report = report,
                    onView = { onViewReport(report) },
                    onDelete = { onDeleteReport(report.id) }
                )
            }
        }
    }
}

@Composable
private fun InsuranceReportsTab(
    reports: List<InsuranceReportEntity>,
    onViewReport: (InsuranceReportEntity) -> Unit,
    onDeleteReport: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reports.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Insurance Reports",
                    description = "Generate insurance reports from the Smart Fleet Management screen to see them here.",
                    icon = Icons.Default.Shield
                )
            }
        } else {
            items(reports) { report ->
                InsuranceReportCard(
                    report = report,
                    onView = { onViewReport(report) },
                    onDelete = { onDeleteReport(report.id) }
                )
            }
        }
    }
}

@Composable
private fun EventSummaryTab(
    events: List<EventSummaryEntity>,
    onFilterByType: (String) -> Unit,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = onClearFilter,
                label = { Text("All Events") }
            )
            AssistChip(
                onClick = { onFilterByType("HARD_BRAKING") },
                label = { Text("Hard Braking") }
            )
            AssistChip(
                onClick = { onFilterByType("SPEEDING") },
                label = { Text("Speeding") }
            )
        }

        // Events list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (events.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Events Recorded",
                        description = "Start fleet monitoring to record driving events and see them here.",
                        icon = Icons.Default.EventNote
                    )
                }
            } else {
                items(events) { event ->
                    EventSummaryCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventReportCard(
    report: EventReportEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(report.generatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onView) {
                        Icon(Icons.Default.Visibility, contentDescription = "View Report")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Report", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "Events", value = "${report.eventCount}")
                report.tripScore?.let { score ->
                    InfoChip(label = "Trip Score", value = "${score.toInt()}/100")
                }
                report.driverStatus?.let { status ->
                    InfoChip(label = "Driver", value = status)
                }
            }
        }
    }
}

@Composable
private fun InsuranceReportCard(
    report: InsuranceReportEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(report.generatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onView) {
                        Icon(Icons.Default.Visibility, contentDescription = "View Report")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Report", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                report.riskScore?.let { score ->
                    InfoChip(label = "Risk Score", value = "${score.toInt()}/100")
                }
                report.estimatedPremium?.let { premium ->
                    InfoChip(label = "Premium", value = "$${premium.toInt()}")
                }
                InfoChip(
                    label = "Discount",
                    value = if (report.discountEligible) "Yes" else "No"
                )
            }
        }
    }
}

@Composable
private fun EventSummaryCard(event: EventSummaryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (event.eventType) {
                    "HARD_BRAKING" -> Icons.Default.PauseCircle
                    "SPEEDING" -> Icons.Default.Warning
                    "RAPID_ACCELERATION" -> Icons.Default.Speed
                    "HARSH_CORNERING" -> Icons.Default.Rotate90DegreesCcw
                    "PHONE_USAGE" -> Icons.Default.PhoneAndroid
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (event.severity) {
                    "CRITICAL" -> Color.Red
                    "HIGH" -> Color.Red
                    "MEDIUM" -> Color.Yellow
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventType.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${event.severity} • ${String.format("%.1f", event.speed * 3.6f)} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    AssistChip(
        onClick = { },
        label = {
            Text(text = "$label: $value", style = MaterialTheme.typography.labelSmall)
        }
    )
}
