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
import com.commerin.telemetri.ui.components.TransparentAppBar
import com.commerin.telemetri.ui.theme.*

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
            color = NetworkDataColor,
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
            color = SensorDataColor,
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
            color = MotionDataColor,
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
            color = LocationDataColor,
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
            color = Color(0xFFE57373), // Light red
            features = listOf(
                "Complete sensor suite",
                "High-frequency monitoring",
                "Audio surveillance",
                "Network intrusion detection",
                "Performance anomaly detection"
            ),
            telemetryTypes = listOf("All Sensors", "Audio", "Network", "Location", "Performance", "Motion")
        ),
        UseCase(
            id = "smart_fleet",
            title = "Smart Fleet Management",
            description = "Advanced telematics with AI-powered driver detection, event analysis, and insurance scoring",
            icon = Icons.Default.LocalShipping,
            color = Color(0xFF7E57C2), // Purple
            features = listOf(
                "AI Driver vs Passenger Detection",
                "Real-time Driving Event Analysis",
                "Insurance Risk Scoring",
                "Adaptive Power Management",
                "Trip Performance Analytics",
                "Battery Optimization"
            ),
            telemetryTypes = listOf("AI Detection", "Event Analysis", "Insurance Analytics", "Power Management", "Risk Assessment")
        ),
        UseCase(
            id = "battery_saver",
            title = "Battery Saver Mode",
            description = "Minimal power consumption while maintaining essential monitoring capabilities",
            icon = Icons.Default.BatteryAlert,
            color = Color(0xFF81C784), // Light green
            features = listOf(
                "Ultra-low power consumption",
                "Essential sensors only",
                "Minimal background processing",
                "Extended battery life",
                "Smart adaptive sampling"
            ),
            telemetryTypes = listOf("Minimal Sensors", "Basic Device State")
        )
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent app bar
        TransparentAppBar(
            title = "Telemetri SDK Demo"
        )

        // Main content with padding for transparent app bar
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 120.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            // Use cases grid
            items(useCases) { useCase ->
                UseCaseCard(
                    useCase = useCase,
                    onClick = { onNavigateToUseCase(useCase.id) }
                )
            }
        }
    }
}

@Composable
private fun UseCaseCard(
    useCase: UseCase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = useCase.color.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(
                        imageVector = useCase.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp),
                        tint = useCase.color
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = useCase.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = useCase.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            Text(
                text = "Key Features:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            useCase.features.take(3).forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = useCase.color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telemetry types
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                useCase.telemetryTypes.take(3).forEach { type ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = useCase.color.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelSmall,
                            color = useCase.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (useCase.telemetryTypes.size > 3) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "+${useCase.telemetryTypes.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
