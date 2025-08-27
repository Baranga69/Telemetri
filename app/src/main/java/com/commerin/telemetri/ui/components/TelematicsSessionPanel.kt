package com.commerin.telemetri.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commerin.telemetri.core.BatteryOptimizationHelper
import com.commerin.telemetri.domain.model.SessionState
import com.commerin.telemetri.ui.viewmodels.TelematicsSessionViewModel
import java.util.concurrent.TimeUnit

/**
 * Session control panel for starting/stopping/pausing telematics sessions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelematicsSessionPanel(
    modifier: Modifier = Modifier,
    viewModel: TelematicsSessionViewModel
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.observeAsState(SessionState.STOPPED)
    val activeSession by viewModel.activeSession.observeAsState()
    val sessionDuration by viewModel.sessionDuration.observeAsState(0L)
    val isStartingSession by viewModel.isStartingSession.observeAsState(false)
    val batteryOptimizationStatus by viewModel.batteryOptimizationStatus.observeAsState("")

    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (sessionState) {
                SessionState.RUNNING -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                SessionState.PAUSED -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Session Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Background Telematics Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getSessionStatusText(sessionState, activeSession?.sessionId),
                        style = MaterialTheme.typography.bodySmall,
                        color = getSessionStatusColor(sessionState)
                    )
                }

                // Session Duration
                if (sessionState != SessionState.STOPPED && sessionDuration > 0) {
                    Text(
                        text = formatDuration(sessionDuration),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Battery Optimization Warning
            if (viewModel.needsBatteryOptimization()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ),
                    onClick = { showBatteryOptimizationDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Battery optimization may affect background tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (sessionState) {
                    SessionState.STOPPED -> {
                        Button(
                            onClick = {
                                if (viewModel.needsBatteryOptimization()) {
                                    showBatteryOptimizationDialog = true
                                } else {
                                    viewModel.startAutomotiveSession()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isStartingSession
                        ) {
                            if (isStartingSession) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Start Session")
                        }
                    }

                    SessionState.RUNNING -> {
                        Button(
                            onClick = { viewModel.pauseSession() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause")
                        }

                        Button(
                            onClick = { viewModel.stopSession() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }

                    SessionState.PAUSED -> {
                        Button(
                            onClick = { viewModel.resumeSession() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume")
                        }

                        Button(
                            onClick = { viewModel.stopSession() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }

                    SessionState.ERROR -> {
                        Button(
                            onClick = { viewModel.startAutomotiveSession() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restart")
                        }
                    }
                }
            }
        }
    }

    // Battery Optimization Dialog
    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            title = { Text("Battery Optimization") },
            text = {
                Column {
                    Text("For reliable background tracking, please disable battery optimization for this app.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This ensures telematics data collection continues even when the screen is locked or the app is in the background.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                            context as androidx.activity.ComponentActivity
                        )
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        viewModel.startAutomotiveSession() // Start anyway
                    }
                ) {
                    Text("Start Anyway")
                }
            }
        )
    }
}

@Composable
private fun getSessionStatusText(state: SessionState, sessionId: String?): String {
    return when (state) {
        SessionState.RUNNING -> "Recording driving data • ID: ${sessionId?.take(8)}"
        SessionState.PAUSED -> "Session paused • ID: ${sessionId?.take(8)}"
        SessionState.STOPPED -> "No active session"
        SessionState.ERROR -> "Session error - please restart"
    }
}

@Composable
private fun getSessionStatusColor(state: SessionState): Color {
    return when (state) {
        SessionState.RUNNING -> Color(0xFF4CAF50)
        SessionState.PAUSED -> Color(0xFFFF9800)
        SessionState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        SessionState.ERROR -> Color(0xFFF44336)
    }
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
