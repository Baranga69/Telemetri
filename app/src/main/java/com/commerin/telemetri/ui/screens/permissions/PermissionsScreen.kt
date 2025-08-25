package com.commerin.telemetri.ui.screens.permissions

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.commerin.telemetri.core.AudioTelemetryService
import com.commerin.telemetri.core.TelemetriManager
import com.commerin.telemetri.domain.model.PermissionState
import com.commerin.telemetri.domain.model.PermissionStateCallback
import com.commerin.telemetri.domain.model.TelemetryPermissionException
import com.commerin.telemetri.domain.model.TelemetryPermissions
import com.commerin.telemetri.helpers.createTelemetryPermissionHelper
import com.commerin.telemetri.utils.PermissionUtils

data class TelemetryPermissionInfo(
    val permission: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val category: String,
    val importance: PermissionImportance,
    val useCases: List<String>
)

data class UseCaseInfo(
    val name: String,
    val description: String,
    val requiredPermissions: List<String>,
    val icon: ImageVector,
    val color: Color
)

enum class PermissionImportance {
    CRITICAL, HIGH, MEDIUM, LOW
}

@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return

    // State management
    var permissionStates by remember { mutableStateOf<Map<String, PermissionState>>(emptyMap()) }
    var telemetryServices by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var showError by remember { mutableStateOf<String?>(null) }
    var selectedUseCase by remember { mutableStateOf<String?>(null) }

    // Telemetry services
    val audioService = remember { AudioTelemetryService(context) }
    val telemetriManager = remember { TelemetriManager.getInstance(context) }

    // Permission helper with integrated callbacks
    val permissionHelper = remember {
        activity.createTelemetryPermissionHelper(object : PermissionStateCallback {
            override fun onAudioPermissionStateChanged(state: PermissionState, permission: String) {
                permissionStates = permissionStates + (permission to state)
                handleAudioPermissionChange(state, audioService) { error ->
                    showError = error
                }
            }

            override fun onLocationPermissionStateChanged(state: PermissionState, permission: String) {
                permissionStates = permissionStates + (permission to state)
                handleLocationPermissionChange(state)
            }

            override fun onConnectivityPermissionStateChanged(state: PermissionState, permission: String) {
                permissionStates = permissionStates + (permission to state)
                handleConnectivityPermissionChange(state)
            }

            override fun onDeviceStatePermissionChanged(state: PermissionState, permission: String) {
                permissionStates = permissionStates + (permission to state)
                handleDeviceStatePermissionChange(state)
            }
        })
    }

    // Initialize permission states
    LaunchedEffect(Unit) {
        permissionStates = TelemetryPermissions.getAllRequiredPermissions().associateWith { permission ->
            PermissionUtils.getPermissionState(context, permission)
        }
        telemetryServices = mapOf(
            "Audio Telemetry" to permissionHelper.areAudioPermissionsGranted(),
            "Location Telemetry" to permissionHelper.areLocationPermissionsGranted(),
            "Connectivity Telemetry" to permissionHelper.areConnectivityPermissionsGranted(),
            "Device State Telemetry" to permissionHelper.areDeviceStatePermissionsGranted()
        )

        // Check if all permissions are granted
        if (permissionHelper.areAllPermissionsGranted()) {
            onPermissionsGranted()
        }
    }

    // Update telemetry services when permission states change
    LaunchedEffect(permissionStates) {
        telemetryServices = mapOf(
            "Audio Telemetry" to permissionHelper.areAudioPermissionsGranted(),
            "Location Telemetry" to permissionHelper.areLocationPermissionsGranted(),
            "Connectivity Telemetry" to permissionHelper.areConnectivityPermissionsGranted(),
            "Device State Telemetry" to permissionHelper.areDeviceStatePermissionsGranted()
        )
    }

    // Use cases from TelemetriManager
    val telemetryUseCases = listOf(
        UseCaseInfo(
            name = "Automotive Telemetry",
            description = "Vehicle speed tracking, navigation, and motion analysis using GPS and motion sensors",
            requiredPermissions = listOf(TelemetryPermissions.LOCATION_FINE, TelemetryPermissions.LOCATION_COARSE),
            icon = Icons.Default.DirectionsCar,
            color = Color(0xFF4CAF50)
        ),
        UseCaseInfo(
            name = "Fitness Tracking",
            description = "Activity detection, step counting, and health metrics using motion sensors and GPS",
            requiredPermissions = listOf(TelemetryPermissions.LOCATION_FINE, TelemetryPermissions.LOCATION_COARSE),
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFF2196F3)
        ),
        UseCaseInfo(
            name = "Environmental Monitoring",
            description = "Ambient data collection including noise levels, environmental sensors, and location context",
            requiredPermissions = listOf(
                TelemetryPermissions.AUDIO_RECORDING,
                TelemetryPermissions.LOCATION_FINE,
                TelemetryPermissions.LOCATION_COARSE,
                TelemetryPermissions.NETWORK_STATE,
                TelemetryPermissions.WIFI_STATE
            ),
            icon = Icons.Default.Nature,
            color = Color(0xFF9C27B0)
        ),
        UseCaseInfo(
            name = "Security Monitoring",
            description = "Comprehensive security monitoring with all sensors for theft detection and unauthorized access",
            requiredPermissions = TelemetryPermissions.getAllRequiredPermissions().toList(),
            icon = Icons.Default.Security,
            color = Color(0xFFF44336)
        ),
        UseCaseInfo(
            name = "Battery Saver",
            description = "Minimal monitoring for maximum battery life with basic device state only",
            requiredPermissions = emptyList(), // Only uses normal permissions
            icon = Icons.Default.BatteryChargingFull,
            color = Color(0xFF4CAF50)
        ),
        UseCaseInfo(
            name = "Network Diagnostics",
            description = "Network performance testing and connectivity analysis with minimal location context",
            requiredPermissions = listOf(
                TelemetryPermissions.LOCATION_COARSE,
                TelemetryPermissions.NETWORK_STATE,
                TelemetryPermissions.WIFI_STATE
            ),
            icon = Icons.Default.NetworkCheck,
            color = Color(0xFF00BCD4)
        )
    )

    // SDK permissions mapped to UI info with use case information
    val telemetryPermissions = listOf(
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.AUDIO_RECORDING,
            title = "Audio Recording",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.AUDIO_RECORDING),
            icon = Icons.Default.Mic,
            color = Color(0xFF9C27B0),
            category = "Audio Telemetry",
            importance = PermissionImportance.HIGH,
            useCases = listOf("Environmental Monitoring", "Security Monitoring")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.LOCATION_FINE,
            title = "Precise Location",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.LOCATION_FINE),
            icon = Icons.Default.LocationOn,
            color = Color(0xFF4CAF50),
            category = "Location Telemetry",
            importance = PermissionImportance.CRITICAL,
            useCases = listOf("Automotive Telemetry", "Fitness Tracking", "Environmental Monitoring", "Security Monitoring")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.LOCATION_COARSE,
            title = "Approximate Location",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.LOCATION_COARSE),
            icon = Icons.Default.MyLocation,
            color = Color(0xFF2196F3),
            category = "Location Telemetry",
            importance = PermissionImportance.HIGH,
            useCases = listOf("Automotive Telemetry", "Fitness Tracking", "Environmental Monitoring", "Security Monitoring", "Network Diagnostics")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.BLUETOOTH_CONNECT,
            title = "Bluetooth Access",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.BLUETOOTH_CONNECT),
            icon = Icons.Default.Bluetooth,
            color = Color(0xFF00BCD4),
            category = "Connectivity Telemetry",
            importance = PermissionImportance.MEDIUM,
            useCases = listOf("Security Monitoring")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.READ_PHONE_STATE,
            title = "Phone State",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.READ_PHONE_STATE),
            icon = Icons.Default.SignalCellularAlt,
            color = Color(0xFFFF9800),
            category = "Device State Telemetry",
            importance = PermissionImportance.MEDIUM,
            useCases = listOf("Security Monitoring")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.WIFI_STATE,
            title = "WiFi State",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.WIFI_STATE),
            icon = Icons.Default.Wifi,
            color = Color(0xFF607D8B),
            category = "Connectivity Telemetry",
            importance = PermissionImportance.LOW,
            useCases = listOf("Environmental Monitoring", "Security Monitoring", "Network Diagnostics")
        ),
        TelemetryPermissionInfo(
            permission = TelemetryPermissions.NETWORK_STATE,
            title = "Network State",
            description = PermissionUtils.getPermissionDescription(TelemetryPermissions.NETWORK_STATE),
            icon = Icons.Default.NetworkCheck,
            color = Color(0xFF795548),
            category = "Connectivity Telemetry",
            importance = PermissionImportance.LOW,
            useCases = listOf("Environmental Monitoring", "Security Monitoring", "Network Diagnostics")
        )
    )

    // Replace the nested scrollable layout with a single LazyColumn that contains all content in a unified scrollable surface
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            TelemetryPermissionsHeader(
                grantedCount = permissionStates.count { it.value is PermissionState.Granted },
                totalCount = telemetryPermissions.size,
                activeServices = telemetryServices.count { it.value }
            )
        }

        // Use case selection (now non-scrollable)
        item {
            UseCaseSelectionOptimized(
                useCases = telemetryUseCases,
                selectedUseCase = selectedUseCase,
                onUseCaseSelected = { selectedUseCase = it },
                permissionStates = permissionStates,
                onStartUseCase = { useCase ->
                    when (useCase.name) {
                        "Automotive Telemetry" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.automotiveUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                        "Fitness Tracking" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.fitnessTrackingUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                        "Environmental Monitoring" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.environmentalMonitoringUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                        "Security Monitoring" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.securityMonitoringUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                        "Battery Saver" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.batterySaverUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                        "Network Diagnostics" -> {
                            telemetriManager.configureTelemetry(TelemetriManager.ConfigPresets.networkDiagnosticsUseCase())
                            telemetriManager.startTelemetryCollection()
                        }
                    }
                    onPermissionsGranted()
                }
            )
        }

        // Error display
        showError?.let { error ->
            item {
                ErrorCard(
                    error = error,
                    onDismiss = { showError = null }
                )
            }
        }

        // Permissions list (now integrated into the main scroll)
        items(telemetryPermissions) { permissionInfo ->
            PermissionCard(
                permissionInfo = permissionInfo,
                state = permissionStates[permissionInfo.permission] ?: PermissionState.NotRequested,
                onRequestPermission = {
                    requestPermissionByCategory(permissionInfo.category, permissionHelper)
                },
                isHighlighted = selectedUseCase?.let { useCase ->
                    telemetryUseCases.find { it.name == useCase }?.requiredPermissions?.contains(permissionInfo.permission) == true
                } ?: false
            )
        }

        // Action buttons at the bottom
        item {
            PermissionActionButtons(
                allGranted = permissionHelper.areAllPermissionsGranted(),
                onRequestAll = {
                    permissionHelper.requestAllPermissions()
                },
                onContinue = onPermissionsGranted
            )
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            audioService.cleanup()
        }
    }
}

@Composable
private fun UseCaseSelectionOptimized(
    useCases: List<UseCaseInfo>,
    selectedUseCase: String?,
    onUseCaseSelected: (String?) -> Unit,
    permissionStates: Map<String, PermissionState>,
    onStartUseCase: (UseCaseInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Choose Your Use Case",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select the primary purpose to see required permissions and start optimized telemetry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Directly show the selected use case with its details
            selectedUseCase?.let { useCaseName ->
                val useCase = useCases.find { it.name == useCaseName }
                if (useCase != null) {
                    val hasRequiredPermissions = useCase.requiredPermissions.all { permission ->
                        permissionStates[permission] is PermissionState.Granted
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = useCase.color.copy(alpha = 0.1f)
                        ),
                        onClick = {
                            onUseCaseSelected(null) // Deselect use case
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = useCase.icon,
                                contentDescription = null,
                                tint = useCase.color,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = useCase.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${useCase.requiredPermissions.size} permissions required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (hasRequiredPermissions && useCase.requiredPermissions.isNotEmpty()) {
                                Button(
                                    onClick = { onStartUseCase(useCase) },
                                    colors = ButtonDefaults.buttonColors(containerColor = useCase.color),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Start",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else if (useCase.requiredPermissions.isEmpty()) {
                                Button(
                                    onClick = { onStartUseCase(useCase) },
                                    colors = ButtonDefaults.buttonColors(containerColor = useCase.color),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Start",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Permissions needed",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } ?: run {
                // If no use case is selected, show all available use cases
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    useCases.forEach { useCase ->
                        val hasRequiredPermissions = useCase.requiredPermissions.all { permission ->
                            permissionStates[permission] is PermissionState.Granted
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedUseCase == useCase.name) {
                                    useCase.color.copy(alpha = 0.1f)
                            } else MaterialTheme.colorScheme.surface
                        ),
                            onClick = {
                                onUseCaseSelected(if (selectedUseCase == useCase.name) null else useCase.name)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = useCase.icon,
                                    contentDescription = null,
                                    tint = useCase.color,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = useCase.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${useCase.requiredPermissions.size} permissions required",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (hasRequiredPermissions && useCase.requiredPermissions.isNotEmpty()) {
                                    Button(
                                        onClick = { onStartUseCase(useCase) },
                                        colors = ButtonDefaults.buttonColors(containerColor = useCase.color),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Start",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else if (useCase.requiredPermissions.isEmpty()) {
                                    Button(
                                        onClick = { onStartUseCase(useCase) },
                                        colors = ButtonDefaults.buttonColors(containerColor = useCase.color),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Start",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Permissions needed",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryPermissionsHeader(
    grantedCount: Int,
    totalCount: Int,
    activeServices: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(top =15.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Telemetry SDK Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Grant permissions to enable comprehensive telemetry data collection",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip(
                    label = "Permissions",
                    value = "$grantedCount/$totalCount",
                    color = if (grantedCount == totalCount) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                StatusChip(
                    label = "Services",
                    value = "$activeServices/4",
                    color = if (activeServices == 4) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    permissionInfo: TelemetryPermissionInfo,
    state: PermissionState,
    onRequestPermission: () -> Unit,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier.padding(2.dp)
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                permissionInfo.color.copy(alpha = 0.05f)
            } else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Permission icon
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = permissionInfo.color.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(
                        imageVector = permissionInfo.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp),
                        tint = permissionInfo.color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Permission info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = permissionInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = permissionInfo.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (permissionInfo.useCases.isNotEmpty()) {
                        Text(
                            text = "Used in: ${permissionInfo.useCases.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = permissionInfo.color,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Permission state and action
                PermissionStateIndicator(
                    state = state,
                    importance = permissionInfo.importance,
                    onRequest = onRequestPermission
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = permissionInfo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionStateIndicator(
    state: PermissionState,
    importance: PermissionImportance,
    onRequest: () -> Unit
) {
    when (state) {
        is PermissionState.Granted -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
        is PermissionState.Denied, is PermissionState.NotRequested -> {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (importance) {
                        PermissionImportance.CRITICAL -> Color(0xFFF44336)
                        PermissionImportance.HIGH -> Color(0xFFFF9800)
                        PermissionImportance.MEDIUM -> Color(0xFF2196F3)
                        PermissionImportance.LOW -> Color(0xFF4CAF50)
                    }
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is PermissionState.PermanentlyDenied -> {
            OutlinedButton(
                onClick = onRequest,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionActionButtons(
    allGranted: Boolean,
    onRequestAll: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!allGranted) {
            Button(
                onClick = onRequestAll,
                modifier = Modifier.weight(1f)
            ) {
                Text("Grant All Permissions")
            }
        } else {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to App")
            }
        }
    }
}

// Permission handling functions
private fun requestPermissionByCategory(
    category: String,
    permissionHelper: com.commerin.telemetri.helpers.TelemetryPermissionHelper
) {
    when (category) {
        "Audio Telemetry" -> permissionHelper.requestAudioPermission()
        "Location Telemetry" -> permissionHelper.requestLocationPermissions()
        "Connectivity Telemetry" -> permissionHelper.requestConnectivityPermissions()
        "Device State Telemetry" -> permissionHelper.requestDeviceStatePermissions()
    }
}

private fun handleAudioPermissionChange(
    state: PermissionState,
    audioService: AudioTelemetryService,
    onError: (String) -> Unit
) {
    when (state) {
        is PermissionState.Granted -> {
            try {
                audioService.startAudioTelemetry()
            } catch (e: TelemetryPermissionException) {
                onError("Audio telemetry error: ${e.message}")
            } catch (e: SecurityException) {
                onError("Audio permission denied by system: ${e.message}")
            }
        }
        is PermissionState.Denied -> {
            onError("Audio permission needed for environmental sound analysis")
        }
        is PermissionState.PermanentlyDenied -> {
            onError("Audio permission permanently denied. Please enable in settings.")
        }
        is PermissionState.NotRequested -> {
            // Permission not yet requested, no action needed
        }
    }
}

private fun handleLocationPermissionChange(state: PermissionState) {
    when (state) {
        is PermissionState.Granted -> {
            // Start location telemetry service
        }
        is PermissionState.Denied,
        is PermissionState.PermanentlyDenied,
        is PermissionState.NotRequested -> {
            // Handle denied/not requested states
        }
    }
}

private fun handleConnectivityPermissionChange(state: PermissionState) {
    when (state) {
        is PermissionState.Granted -> {
            // Start connectivity telemetry service
        }
        is PermissionState.Denied,
        is PermissionState.PermanentlyDenied,
        is PermissionState.NotRequested -> {
            // Handle denied/not requested states
        }
    }
}

private fun handleDeviceStatePermissionChange(state: PermissionState) {
    when (state) {
        is PermissionState.Granted -> {
            // Start device state telemetry service
        }
        is PermissionState.Denied,
        is PermissionState.PermanentlyDenied,
        is PermissionState.NotRequested -> {
            // Handle denied/not requested states
        }
    }
}
