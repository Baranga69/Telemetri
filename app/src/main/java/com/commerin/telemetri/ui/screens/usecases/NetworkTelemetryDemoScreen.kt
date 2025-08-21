package com.commerin.telemetri.ui.screens.usecases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commerin.telemetri.ui.components.charts.SignalStrengthGauge
import com.commerin.telemetri.ui.components.charts.NetworkSpeedGauge
import com.commerin.telemetri.ui.viewmodels.AutomotiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTelemetryDemoScreen(
    onBackPressed: () -> Unit,
    viewModel: AutomotiveViewModel = hiltViewModel()
) {
    val isCollecting by viewModel.isCollecting.observeAsState(false)
    val networkData by viewModel.networkData.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Network Telemetry Demo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Network Monitoring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCollecting) "Active - Real-time monitoring" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCollecting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (isCollecting) {
                                viewModel.stopCollection()
                            } else {
                                viewModel.startAutomotiveCollection()
                            }
                        }
                    ) {
                        Text(if (isCollecting) "Stop" else "Start")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Signal Strength Gauges
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Signal Quality Monitoring",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Network Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        networkData?.let { data ->
                            NetworkDetailRow("Network Type", data.networkType.name)
                            NetworkDetailRow("Signal Quality", "${(data.signalQuality * 100).toInt()}%")
                            NetworkDetailRow("Latency", "${data.latency}ms")
                            NetworkDetailRow("Packet Loss", "${data.packetLoss}%")

                            data.wifiInfo?.let { wifi ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "WiFi Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                NetworkDetailRow("SSID", wifi.ssid)
                                NetworkDetailRow("Frequency", "${wifi.frequency} MHz")
                                NetworkDetailRow("Link Speed", "${wifi.linkSpeed} Mbps")
                            }

                            data.cellularInfo?.let { cellular ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Cellular Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                NetworkDetailRow("Operator", cellular.operatorName)
                                NetworkDetailRow("Signal Type", cellular.signalType)
                                NetworkDetailRow("Data Activity", cellular.dataActivity)
                                NetworkDetailRow("Roaming", if (cellular.roaming) "Yes" else "No")
                            }
                        } ?: run {
                            Text(
                                text = "No network data available. Start monitoring to see real-time information.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Demo Data Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Demo Signal Variations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Experience different signal conditions:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Excellent Signal Demo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SignalStrengthGauge(
                                    signalStrength = -45,
                                    modifier = Modifier.size(120.dp),
                                    title = ""
                                )
                                Text("Excellent", style = MaterialTheme.typography.labelMedium)
                                Text("-45 dBm", style = MaterialTheme.typography.labelSmall)
                            }

                            // Good Signal Demo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SignalStrengthGauge(
                                    signalStrength = -65,
                                    modifier = Modifier.size(120.dp),
                                    title = ""
                                )
                                Text("Good", style = MaterialTheme.typography.labelMedium)
                                Text("-65 dBm", style = MaterialTheme.typography.labelSmall)
                            }

                            // Poor Signal Demo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SignalStrengthGauge(
                                    signalStrength = -85,
                                    modifier = Modifier.size(120.dp),
                                    title = ""
                                )
                                Text("Poor", style = MaterialTheme.typography.labelMedium)
                                Text("-85 dBm", style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
            fontWeight = FontWeight.Medium
        )
    }
}
