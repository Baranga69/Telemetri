package com.commerin.telemetri.ui.screens.permissions

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.google.accompanist.permissions.*

data class PermissionInfo(
    val permission: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val importance: PermissionImportance
)

enum class PermissionImportance {
    CRITICAL, HIGH, MEDIUM, LOW
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val permissionsToRequest = listOf(
        PermissionInfo(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            title = "Precise Location",
            description = "Required for accurate GPS tracking, automotive telemetry, and location-based analytics. Enables precise positioning for navigation and movement analysis.",
            icon = Icons.Default.LocationOn,
            color = Color(0xFF4CAF50),
            importance = PermissionImportance.CRITICAL
        ),
        PermissionInfo(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            title = "Approximate Location",
            description = "Provides general location information for environmental monitoring and network-based positioning. Used as fallback when precise location is unavailable.",
            icon = Icons.Default.MyLocation,
            color = Color(0xFF2196F3),
            importance = PermissionImportance.HIGH
        ),
        PermissionInfo(
            permission = Manifest.permission.RECORD_AUDIO,
            title = "Microphone Access",
            description = "Enables environmental sound analysis, noise level monitoring, and voice detection for context-aware telemetry. Used for acoustic environmental profiling.",
            icon = Icons.Default.Mic,
            color = Color(0xFF9C27B0),
            importance = PermissionImportance.HIGH
        ),
        PermissionInfo(
            permission = Manifest.permission.READ_PHONE_STATE,
            title = "Phone State",
            description = "Allows reading cellular network information including signal strength, network type (4G/5G), and carrier details for connectivity analytics.",
            icon = Icons.Default.SignalCellularAlt,
            color = Color(0xFFFF9800),
            importance = PermissionImportance.MEDIUM
        ),
        PermissionInfo(
            permission = Manifest.permission.CAMERA,
            title = "Camera Access",
            description = "Enables visual context analysis, light estimation, and motion detection through computer vision for enhanced environmental awareness.",
            icon = Icons.Default.CameraAlt,
            color = Color(0xFFE91E63),
            importance = PermissionImportance.MEDIUM
        ),
        PermissionInfo(
            permission = Manifest.permission.BODY_SENSORS,
            title = "Body Sensors",
            description = "Access to heart rate sensors and other biometric data for health and fitness tracking use cases. Enables comprehensive wellness monitoring.",
            icon = Icons.Default.Favorite,
            color = Color(0xFFF44336),
            importance = PermissionImportance.MEDIUM
        ),
        PermissionInfo(
            permission = "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
            title = "Activity Recognition",
            description = "Automatically detects user activities like walking, running, cycling, or being in a vehicle. Essential for fitness tracking and automotive use cases.",
            icon = Icons.Default.DirectionsRun,
            color = Color(0xFF00BCD4),
            importance = PermissionImportance.HIGH
        )
    )

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest.map { it.permission }
    )

    var currentStep by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { permissionsToRequest.size + 2 }) // +2 for intro and summary

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / pagerState.pageCount },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )

        Text(
            text = "Step ${pagerState.currentPage + 1} of ${pagerState.pageCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> IntroductionPage()
                pagerState.pageCount - 1 -> SummaryPage(
                    multiplePermissionsState = multiplePermissionsState,
                    onRequestPermissions = {
                        multiplePermissionsState.launchMultiplePermissionRequest()
                    }
                )
                else -> {
                    val permissionInfo = permissionsToRequest[page - 1]
                    PermissionDetailPage(permissionInfo = permissionInfo)
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        currentStep = pagerState.currentPage - 1
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Text("Previous")
            }

            if (pagerState.currentPage < pagerState.pageCount - 1) {
                Button(
                    onClick = {
                        currentStep = pagerState.currentPage + 1
                    }
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = {
                        multiplePermissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }

    LaunchedEffect(currentStep) {
        pagerState.animateScrollToPage(currentStep)
    }
}

@Composable
private fun IntroductionPage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to Telemetri SDK Demo",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This demonstration showcases comprehensive telemetry data collection capabilities. To provide the best experience, we need access to various device sensors and features.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Use Cases Covered:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Automotive Telemetry", style = MaterialTheme.typography.bodyMedium)
                    Text("• Fitness & Health Tracking", style = MaterialTheme.typography.bodyMedium)
                    Text("• Environmental Monitoring", style = MaterialTheme.typography.bodyMedium)
                    Text("• Security & Surveillance", style = MaterialTheme.typography.bodyMedium)
                    Text("• Battery-Optimized Collection", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionDetailPage(permissionInfo: PermissionInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = permissionInfo.color.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = permissionInfo.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(16.dp),
                    tint = permissionInfo.color
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = permissionInfo.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            ImportanceBadge(importance = permissionInfo.importance)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = permissionInfo.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Why is this needed?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getPermissionRationale(permissionInfo.permission),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportanceBadge(importance: PermissionImportance) {
    val (text, color) = when (importance) {
        PermissionImportance.CRITICAL -> "Critical" to Color(0xFFF44336)
        PermissionImportance.HIGH -> "High Priority" to Color(0xFFFF9800)
        PermissionImportance.MEDIUM -> "Recommended" to Color(0xFF2196F3)
        PermissionImportance.LOW -> "Optional" to Color(0xFF4CAF50)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SummaryPage(
    multiplePermissionsState: MultiplePermissionsState,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ready to Begin",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Grant the necessary permissions to unlock the full potential of comprehensive telemetry data collection.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (multiplePermissionsState.shouldShowRationale) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ Some permissions were denied",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To experience all telemetry features, please grant the requested permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant All Permissions")
            }
        }
    }
}

private fun getPermissionRationale(permission: String): String {
    return when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION ->
            "Precise location enables accurate tracking for automotive navigation, fitness route mapping, and location-based environmental analysis."
        Manifest.permission.ACCESS_COARSE_LOCATION ->
            "General location provides context for network-based positioning and regional environmental data correlation."
        Manifest.permission.RECORD_AUDIO ->
            "Audio analysis detects environmental sounds, measures noise levels, and provides acoustic context for comprehensive telemetry."
        Manifest.permission.READ_PHONE_STATE ->
            "Phone state access enables monitoring of cellular network quality, signal strength, and connectivity performance metrics."
        Manifest.permission.CAMERA ->
            "Camera access enables visual context analysis, automatic light level detection, and motion estimation through computer vision."
        Manifest.permission.BODY_SENSORS ->
            "Body sensor access enables heart rate monitoring and other biometric data collection for health and fitness applications."
        "com.google.android.gms.permission.ACTIVITY_RECOGNITION" ->
            "Activity recognition automatically detects user movement patterns, enabling intelligent context-aware telemetry collection."
        else -> "This permission enhances the telemetry collection capabilities and user experience."
    }
}
