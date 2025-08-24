package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.core.NetworkTestType
import com.commerin.telemetri.ui.components.charts.SignalStrengthGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedTestWidget
import com.commerin.telemetri.ui.components.charts.NetworkSpeedData
import com.commerin.telemetri.ui.components.charts.AppNetworkTestType
import com.commerin.telemetri.ui.components.TransparentAppBar
import com.commerin.telemetri.ui.viewmodels.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTelemetryDemoScreen(
    onBackPressed: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val networkData by viewModel.networkData.observeAsState()
    val speedTestResult by viewModel.speedTestResult.observeAsState()

    // Convert SDK SpeedTestResult to UI NetworkSpeedData
    val speedTestData = speedTestResult?.let { result ->
        NetworkSpeedData(
            downloadSpeed = result.downloadSpeed,
            uploadSpeed = result.uploadSpeed,
            ping = result.ping,
            isTestRunning = result.isTestRunning,
            currentTestType = when (result.currentTestType) {
                NetworkTestType.DOWNLOAD -> AppNetworkTestType.DOWNLOAD
                NetworkTestType.UPLOAD -> AppNetworkTestType.UPLOAD
                NetworkTestType.PING -> AppNetworkTestType.PING
                null -> null
            },
            progress = result.progress
        )
    } ?: NetworkSpeedData()

    // Additional metrics from enhanced speed test
    val jitter = speedTestResult?.jitter ?: 0f
    val packetLoss = speedTestResult?.packetLoss ?: 0f
    val errorMessage = speedTestResult?.errorMessage

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent app bar
        TransparentAppBar(
            title = "Network Telemetry Demo",
            onBackPressed = onBackPressed
        )

        // Main content with padding for transparent app bar
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 120.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    text = "Network Diagnostics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isCollecting) "Active - Optimized network-only monitoring" else "Stopped",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCollecting)
                                        MaterialTheme.colorScheme.tertiary else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (isCollecting) {
                                        viewModel.stopCollection()
                                    } else {
                                        viewModel.startNetworkDiagnostics()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCollecting)
                                        MaterialTheme.colorScheme.errorContainer else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isCollecting)
                                        MaterialTheme.colorScheme.onErrorContainer else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isCollecting) "Stop" else "Start",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Network Speed Test Widget - now using real data
            item {
                NetworkSpeedTestWidget(
                    networkData = speedTestData,
                    onStartTest = {
                        if (!speedTestData.isTestRunning) {
                            viewModel.startNetworkSpeedTest()
                        }
                    },
                    onStopTest = {
                        viewModel.stopNetworkSpeedTest()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Network Signal Strength Gauges
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Signal Quality Monitoring",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Signal Strength Gauge
                            SignalStrengthGauge(
                                signalStrength = networkData?.signalStrength ?: -75,
                                modifier = Modifier.weight(1f),
                                title = "Signal Strength"
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Network Speed Gauge
                            NetworkSpeedGauge(
                                speedBps = networkData?.connectionSpeed ?: 10_000_000L, // 10 Mbps default
                                modifier = Modifier.weight(1f),
                                title = "Connection Speed"
                            )
                        }
                    }
                }
            }

            // Network Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Network Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        networkData?.let { data ->
                            NetworkDetailRow("Network Type", data.networkType.name)
                            NetworkDetailRow("Signal Quality", "${(data.signalQuality * 100).toInt()}%")
                            NetworkDetailRow("Latency", "${data.latency}ms")
                            NetworkDetailRow("Packet Loss", "${data.packetLoss}%")
                            NetworkDetailRow("Jitter", "${String.format("%.1f", jitter)} ms")

                            // Only show error message if it exists and is not null/empty
                            errorMessage?.let { error ->
                                if (error.isNotBlank() && error != "null") {
                                    NetworkDetailRow("Error", error)
                                }
                            }

                            data.wifiInfo?.let { wifi ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "WiFi Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                NetworkDetailRow("SSID", wifi.ssid)
                                NetworkDetailRow("Frequency", "${wifi.frequency} MHz")
                                NetworkDetailRow("Link Speed", "${wifi.linkSpeed} Mbps")
                            }

                            data.cellularInfo?.let { cellular ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Cellular Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                NetworkDetailRow("Carrier", cellular.operatorName)
                                NetworkDetailRow("Network Type", cellular.signalType)
                                NetworkDetailRow("Signal Strength", "${cellular.signalStrength} dBm")
                            }
                        } ?: Text(
                            "No network data available",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
