package com.commerin.telemetri.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class UseCase(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val features: List<String>,
    val telemetryTypes: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToUseCase: (String) -> Unit
) {
    val useCases = listOf(
        UseCase(
            id = "network_demo",
            title = "Network Telemetry Demo",
            description = "Real-time network monitoring with signal strength and speed gauges",
            icon = Icons.Default.NetworkCheck,
            color = Color(0xFF673AB7),
            features = listOf(
                "Signal strength visualization",
                "Connection speed monitoring",
                "Network type detection",
                "Real-time gauge displays",
                "WiFi and cellular analysis"
            ),
            telemetryTypes = listOf("Network", "Signal", "Speed", "Connectivity")
        ),
        UseCase(
            id = "automotive",
            title = "Automotive Telemetry",
            description = "Comprehensive vehicle and driving analytics with high-precision data collection",
            icon = Icons.Default.DirectionsCar,
            color = Color(0xFF1976D2),
            features = listOf(
                "High-frequency sensor collection",
                "Precise GPS tracking (1-second intervals)",
                "Audio environment analysis",
                "Network connectivity monitoring",
                "Real-time performance tracking"
            ),
            telemetryTypes = listOf("Location", "Sensors", "Audio", "Network", "Performance", "Motion")
        ),
        UseCase(
            id = "fitness",
            title = "Fitness & Health Tracking",
            description = "Advanced movement analysis and health monitoring for fitness applications",
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFF4CAF50),
            features = listOf(
                "Activity recognition",
                "Step counting and frequency",
                "Heart rate monitoring",
                "Route tracking",
                "Performance analytics"
            ),
            telemetryTypes = listOf("Motion", "Sensors", "Location", "Biometric", "Performance")
        ),
        UseCase(
            id = "environmental",
            title = "Environmental Monitoring",
            description = "Environmental context awareness through multi-sensor data fusion",
            icon = Icons.Default.Eco,
            color = Color(0xFF4CAF50),
            features = listOf(
                "Ambient sound analysis",
                "Environmental sensor readings",
                "Air quality monitoring",
                "Light level detection",
                "Weather correlation"
            ),
            telemetryTypes = listOf("Audio", "Environmental", "Sensors", "Location", "Network")
        ),
        UseCase(
            id = "security",
            title = "Security & Surveillance",
            description = "Maximum data collection for security and monitoring applications",
            icon = Icons.Default.Security,
            color = Color(0xFFF44336),
            features = listOf(
                "Ultra-high frequency sampling",
                "Continuous monitoring",
                "Real-time analysis",
                "Comprehensive logging",
                "Anomaly detection"
            ),
            telemetryTypes = listOf("All Sensors", "Location", "Audio", "Network", "Performance", "Visual")
        ),
        UseCase(
            id = "battery_saver",
            title = "Battery Optimized",
            description = "Intelligent telemetry collection with maximum power efficiency",
            icon = Icons.Default.BatteryChargingFull,
            color = Color(0xFFFF9800),
            features = listOf(
                "Adaptive sampling rates",
                "Power-efficient collection",
                "Essential sensors only",
                "Smart scheduling",
                "Background optimization"
            ),
            telemetryTypes = listOf("Core Sensors", "Device State", "Minimal Location")
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Telemetri SDK Demo",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explore comprehensive telemetry data collection across different use cases",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose a Use Case",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(useCases) { useCase ->
                UseCaseCard(
                    useCase = useCase,
                    onClick = { onNavigateToUseCase(useCase.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UseCaseCard(
    useCase: UseCase,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = useCase.color.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = useCase.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(12.dp),
                        tint = useCase.color
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = useCase.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = useCase.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            Text(
                text = "Key Features:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            useCase.features.take(3).forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = useCase.color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telemetry types
            Text(
                text = "Telemetry Data:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                useCase.telemetryTypes.take(3).forEach { type ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = useCase.color.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = useCase.color
                        )
                    }
                }
                if (useCase.telemetryTypes.size > 3) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+${useCase.telemetryTypes.size - 3}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
