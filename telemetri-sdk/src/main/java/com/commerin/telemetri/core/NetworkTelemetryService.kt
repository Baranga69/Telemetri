package com.commerin.telemetri.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiManager
import android.telephony.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

class NetworkTelemetryService(private val context: Context) {
    companion object {
        private const val TAG = "NetworkTelemetryService"
        private const val PING_HOST = "8.8.8.8"
        private const val PING_TIMEOUT = 3000
        private const val SCAN_INTERVAL_MS = 30000L // 30 seconds
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val _networkData = MutableLiveData<NetworkTelemetryData>()
    val networkData: LiveData<NetworkTelemetryData> = _networkData

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    private var isMonitoring = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            if (hasNetworkStatePermission()) {
                scope.launch {
                    collectNetworkTelemetry()
                }
            }
        }

        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            if (hasNetworkStatePermission()) {
                scope.launch {
                    collectNetworkTelemetry()
                }
            }
        }

        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.d(TAG, "Network capabilities changed")
            if (hasNetworkStatePermission()) {
                scope.launch {
                    collectNetworkTelemetry()
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startNetworkMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Network monitoring already running")
            return
        }

        // Check for required permissions
        if (!hasNetworkStatePermission()) {
            Log.e(TAG, "ACCESS_NETWORK_STATE permission not granted")
            return
        }

        isMonitoring = true

        // Register network callback with proper API level check and permission handling
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                Log.d(TAG, "Network callback registered successfully")
            } else {
                Log.w(TAG, "Network callback not supported on API level ${Build.VERSION.SDK_INT}")
                // For older versions, we'll rely only on periodic monitoring
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for network callback registration", e)
            // Continue with periodic monitoring only
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            // Continue with periodic monitoring only
        }

        // Start periodic monitoring (works as fallback for older APIs or permission issues)
        monitoringJob = scope.launch {
            while (isMonitoring) {
                try {
                    collectNetworkTelemetry()
                    delay(SCAN_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network monitoring loop", e)
                    delay(SCAN_INTERVAL_MS)
                }
            }
        }

        Log.d(TAG, "Network telemetry monitoring started")
    }

    fun stopNetworkMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering network callback", e)
        }

        Log.d(TAG, "Network telemetry monitoring stopped")
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private suspend fun collectNetworkTelemetry() {
        try {
            // Only collect data if we have the minimum required permission
            if (!hasNetworkStatePermission()) {
                Log.w(TAG, "ACCESS_NETWORK_STATE permission not available")
                return
            }

            val networkType = getCurrentNetworkType()
            val signalStrength = getSignalStrength()
            val signalQuality = calculateSignalQuality(signalStrength)
            val connectionSpeed = measureConnectionSpeed()
            val latency = measureLatency()
            val packetLoss = measurePacketLoss()

            // Only collect WiFi info if we have the required permissions
            val wifiInfo = if (hasWifiPermissions()) {
                try {
                    @Suppress("MissingPermission")
                    getWifiTelemetryInfo()
                } catch (e: SecurityException) {
                    Log.w(TAG, "WiFi permissions revoked during collection", e)
                    null
                }
            } else {
                null
            }

            val cellularInfo = getCellularTelemetryInfo()

            // Only collect Bluetooth info if we have the required permissions
            val bluetoothDevices = if (hasBluetoothPermission()) {
                try {
                    @Suppress("MissingPermission")
                    getBluetoothDevices()
                } catch (e: SecurityException) {
                    Log.w(TAG, "Bluetooth permissions revoked during collection", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

            val networkTelemetry = NetworkTelemetryData(
                networkType = networkType,
                signalStrength = signalStrength,
                signalQuality = signalQuality,
                connectionSpeed = connectionSpeed,
                latency = latency,
                packetLoss = packetLoss,
                wifiInfo = wifiInfo,
                cellularInfo = cellularInfo,
                bluetoothDevices = bluetoothDevices,
                timestamp = System.currentTimeMillis()
            )

            withContext(Dispatchers.Main) {
                _networkData.postValue(networkTelemetry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting network telemetry", e)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getCurrentNetworkType(): NetworkType {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                when {
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                        @Suppress("MissingPermission")
                        getCellularNetworkType()
                    }
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true -> NetworkType.BLUETOOTH
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> NetworkType.VPN
                    else -> if (activeNetwork != null) NetworkType.UNKNOWN else NetworkType.DISCONNECTED
                }
            } else {
                // Fallback for API < 23
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                    ConnectivityManager.TYPE_MOBILE -> {
                        @Suppress("MissingPermission")
                        getCellularNetworkType()
                    }
                    ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                    ConnectivityManager.TYPE_BLUETOOTH -> NetworkType.BLUETOOTH
                    ConnectivityManager.TYPE_VPN -> NetworkType.VPN
                    else -> if (networkInfo?.isConnected == true) NetworkType.UNKNOWN else NetworkType.DISCONNECTED
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting network type", e)
            NetworkType.UNKNOWN
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE])
    private fun getCellularNetworkType(): NetworkType {
        return try {
            if (!hasPhoneStatePermission()) {
                return NetworkType.CELLULAR_4G // Default assumption when permission not available
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    when (telephonyManager.dataNetworkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> NetworkType.CELLULAR_5G
                        TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.CELLULAR_4G
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSPAP,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_UMTS -> NetworkType.CELLULAR_3G
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_GPRS -> NetworkType.CELLULAR_2G
                        else -> NetworkType.CELLULAR_4G // Default assumption
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission required for data network type", e)
                    NetworkType.CELLULAR_4G
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    when (telephonyManager.networkType) {
                        TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.CELLULAR_4G
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSPAP,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_UMTS -> NetworkType.CELLULAR_3G
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_GPRS -> NetworkType.CELLULAR_2G
                        else -> NetworkType.CELLULAR_4G
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission required for network type", e)
                    NetworkType.CELLULAR_4G
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting cellular network type", e)
            NetworkType.CELLULAR_4G
        }
    }

    private fun getSignalStrength(): Int {
        return try {
            val currentNetworkType = if (hasNetworkStatePermission()) {
                @Suppress("MissingPermission")
                getCurrentNetworkType()
            } else {
                NetworkType.UNKNOWN
            }

            if (currentNetworkType == NetworkType.WIFI) {
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo?.rssi ?: -100
            } else {
                // For cellular, this is a simplified approach
                -75 // Default cellular signal strength
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting signal strength", e)
            -100
        }
    }

    private fun calculateSignalQuality(signalStrength: Int): Float {
        return when {
            signalStrength >= -50 -> 1.0f
            signalStrength >= -60 -> 0.8f
            signalStrength >= -70 -> 0.6f
            signalStrength >= -80 -> 0.4f
            signalStrength >= -90 -> 0.2f
            else -> 0.1f
        }
    }

    private suspend fun measureConnectionSpeed(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val testData = ByteArray(1024) // 1KB test
                // This is a simplified speed test - in production you'd want a proper implementation
                val endTime = System.currentTimeMillis()
                val timeTaken = endTime - startTime
                if (timeTaken > 0) (testData.size * 8L * 1000L) / timeTaken else 0L
            } catch (e: Exception) {
                Log.w(TAG, "Error measuring connection speed", e)
                0L
            }
        }
    }

    private suspend fun measureLatency(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val address = InetAddress.getByName(PING_HOST)
                val reachable = address.isReachable(PING_TIMEOUT)
                val endTime = System.currentTimeMillis()
                if (reachable) endTime - startTime else -1L
            } catch (e: UnknownHostException) {
                Log.w(TAG, "Unknown host for latency test", e)
                -1L
            } catch (e: IOException) {
                Log.w(TAG, "IO error during latency test", e)
                -1L
            }
        }
    }

    private suspend fun measurePacketLoss(): Float {
        return withContext(Dispatchers.IO) {
            try {
                // Simplified packet loss measurement
                var successCount = 0
                val totalPings = 5

                repeat(totalPings) {
                    try {
                        val address = InetAddress.getByName(PING_HOST)
                        if (address.isReachable(1000)) {
                            successCount++
                        }
                    } catch (_: Exception) {
                        // Packet lost
                    }
                }

                ((totalPings - successCount).toFloat() / totalPings) * 100f
            } catch (e: Exception) {
                Log.w(TAG, "Error measuring packet loss", e)
                0f
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun getWifiTelemetryInfo(): WifiTelemetryInfo? {
        return try {
            val currentNetworkType = if (hasNetworkStatePermission()) {
                @Suppress("MissingPermission")
                getCurrentNetworkType()
            } else {
                return null
            }

            if (currentNetworkType != NetworkType.WIFI) return null
            if (!hasWifiPermissions()) return null

            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo ?: return null
            val nearbyNetworks = getNearbyWifiNetworks()

            WifiTelemetryInfo(
                ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown",
                bssid = wifiInfo.bssid ?: "Unknown",
                frequency = wifiInfo.frequency,
                rssi = wifiInfo.rssi,
                linkSpeed = wifiInfo.linkSpeed,
                ipAddress = "Hidden", // Privacy: Don't expose IP address
                macAddress = "Hidden", // Privacy: Don't expose MAC address
                channelWidth = 0, // Would need API 23+ for this
                wifiStandard = "Unknown", // Would need API 30+ for this
                nearbyNetworks = nearbyNetworks
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error getting WiFi telemetry info", e)
            null
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun getNearbyWifiNetworks(): List<NearbyWifiNetwork> {
        return try {
            if (!hasWifiPermissions()) {
                Log.w(TAG, "WiFi permissions required for scanning")
                return emptyList()
            }

            wifiManager.scanResults?.map { scanResult ->
                NearbyWifiNetwork(
                    ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        scanResult.wifiSsid?.toString() ?: "Unknown"
                    } else {
                        @Suppress("DEPRECATION")
                        scanResult.SSID ?: "Unknown"
                    },
                    bssid = scanResult.BSSID ?: "Unknown",
                    rssi = scanResult.level,
                    frequency = scanResult.frequency,
                    capabilities = scanResult.capabilities
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning WiFi networks", e)
            emptyList()
        }
    }

    private fun getCellularTelemetryInfo(): CellularTelemetryInfo? {
        return try {
            val currentNetworkType = if (hasNetworkStatePermission()) {
                @Suppress("MissingPermission")
                getCurrentNetworkType()
            } else {
                return null
            }

            if (currentNetworkType.name.startsWith("CELLULAR").not()) return null
            if (!hasPhoneStatePermission()) {
                Log.w(TAG, "Phone state permission required for cellular info")
                return null
            }

            CellularTelemetryInfo(
                operatorName = telephonyManager.networkOperatorName ?: "Unknown",
                mcc = telephonyManager.networkOperator?.take(3) ?: "000",
                mnc = telephonyManager.networkOperator?.drop(3) ?: "00",
                cellId = 0L, // Would require additional permissions and APIs
                lac = 0, // Would require additional permissions and APIs
                signalStrength = getSignalStrength(),
                signalType = if (hasPhoneStatePermission()) {
                    @Suppress("MissingPermission")
                    getCellularNetworkType().name
                } else {
                    "Unknown"
                },
                dataActivity = getDataActivity(),
                roaming = telephonyManager.isNetworkRoaming
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error getting cellular telemetry info", e)
            null
        }
    }

    private fun getDataActivity(): String {
        return try {
            when (telephonyManager.dataActivity) {
                TelephonyManager.DATA_ACTIVITY_IN -> "Inbound"
                TelephonyManager.DATA_ACTIVITY_OUT -> "Outbound"
                TelephonyManager.DATA_ACTIVITY_INOUT -> "Bidirectional"
                TelephonyManager.DATA_ACTIVITY_DORMANT -> "Dormant"
                else -> "None"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting data activity", e)
            "None"
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getBluetoothDevices(): List<BluetoothDeviceInfo> {
        return try {
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter?.isEnabled != true) return emptyList()
            if (!hasBluetoothPermission()) {
                Log.w(TAG, "Bluetooth permission required for device scanning")
                return emptyList()
            }

            bluetoothAdapter.bondedDevices?.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown",
                    address = device.address ?: "Unknown",
                    deviceClass = device.bluetoothClass?.deviceClass ?: 0,
                    rssi = -60, // Default value, actual RSSI requires scanning
                    bondState = getBondStateString(device.bondState),
                    deviceType = getDeviceTypeString(device.type)
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error getting Bluetooth devices", e)
            emptyList()
        }
    }

    private fun getBondStateString(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_BONDED -> "Bonded"
            BluetoothDevice.BOND_BONDING -> "Bonding"
            BluetoothDevice.BOND_NONE -> "Not Bonded"
            else -> "Unknown"
        }
    }

    private fun getDeviceTypeString(deviceType: Int): String {
        return when (deviceType) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
            else -> "Unknown"
        }
    }

    private fun hasNetworkStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWifiPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun cleanup() {
        stopNetworkMonitoring()
        scope.cancel()
    }
}
