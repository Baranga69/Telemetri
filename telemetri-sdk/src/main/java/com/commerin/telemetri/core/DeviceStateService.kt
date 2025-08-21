package com.commerin.telemetri.core

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.DeviceStateData

class DeviceStateService(private val context: Context) {
    companion object {
        private const val TAG = "DeviceStateService"
    }

    private val _deviceStateData = MutableLiveData<DeviceStateData>()
    val deviceStateData: LiveData<DeviceStateData> = _deviceStateData

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var isMonitoring = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateDeviceState()
            }
        }
    }

    fun start() {
        if (isMonitoring) {
            Log.w(TAG, "Device state monitoring already started")
            return
        }

        try {
            // Register battery receiver
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, batteryFilter)

            // Register network callback only if we have the required permission
            if (hasNetworkStatePermission()) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        updateDeviceState()
                    }

                    override fun onLost(network: Network) {
                        updateDeviceState()
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        updateDeviceState()
                    }
                }

                val networkRequest = NetworkRequest.Builder().build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
                Log.d(TAG, "Network monitoring enabled with permission")
            } else {
                Log.w(TAG, "ACCESS_NETWORK_STATE permission not granted, network monitoring disabled")
            }

            isMonitoring = true
            updateDeviceState() // Initial state
            Log.d(TAG, "Device state monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start device state monitoring", e)
        }
    }

    fun stop() {
        if (!isMonitoring) {
            Log.w(TAG, "Device state monitoring not started")
            return
        }

        try {
            context.unregisterReceiver(batteryReceiver)
            networkCallback?.let {
                if (hasNetworkStatePermission()) {
                    connectivityManager.unregisterNetworkCallback(it)
                }
            }
            isMonitoring = false
            Log.d(TAG, "Device state monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop device state monitoring", e)
        }
    }

    // Add missing method aliases for TelemetriManager compatibility
    fun startMonitoring() = start()
    fun stopMonitoring() = stop()

    // Add alias for deviceState property to match TelemetriManager expectations
    val deviceState: LiveData<DeviceStateData> = _deviceStateData

    private fun updateDeviceState() {
        try {
            val deviceState = DeviceStateData(
                batteryLevel = getBatteryLevel(),
                batteryTemperature = getBatteryTemperature(),
                isCharging = isCharging(),
                chargingType = getChargingType(),
                networkType = getNetworkType(),
                signalStrength = getSignalStrength(),
                isAirplaneModeOn = isAirplaneModeOn(),
                isWifiEnabled = isWifiEnabled(),
                isBluetoothEnabled = isBluetoothEnabled(),
                memoryUsage = getMemoryUsage(),
                cpuUsage = getCpuUsage(),
                storageUsage = getStorageUsage(),
                screenBrightness = getScreenBrightness(),
                isScreenOn = isScreenOn(),
                deviceOrientation = getDeviceOrientation(),
                timestamp = System.currentTimeMillis()
            )

            _deviceStateData.postValue(deviceState)
            Log.v(TAG, "Device state updated: Battery ${deviceState.batteryLevel}%, Network: ${deviceState.networkType}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device state", e)
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getBatteryTemperature(): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return temperature / 10.0f // Convert from tenths of degree Celsius
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getChargingType(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not charging"
        }
    }

    private fun getNetworkType(): String {
        if (!hasNetworkStatePermission()) {
            return "Permission Required"
        }

        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return "No connection"
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Unknown"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasPhoneStatePermission()) {
                        when (telephonyManager.dataNetworkType) {
                            TelephonyManager.NETWORK_TYPE_NR -> "5G"
                            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                            else -> "Cellular"
                        }
                    } else {
                        "Cellular"
                    }
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                else -> "Unknown"
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting network type: ${e.message}")
            "Permission Required"
        } catch (e: Exception) {
            Log.w(TAG, "Error getting network type", e)
            "Unknown"
        }
    }

    private fun getSignalStrength(): Int {
        // This would require READ_PHONE_STATE permission to get accurate signal strength
        // For now, return -1 to indicate unavailable
        if (!hasPhoneStatePermission()) {
            return -1
        }

        return try {
            // Signal strength would need additional implementation with PhoneStateListener
            // For now, return -1 to indicate unavailable
            -1
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting signal strength: ${e.message}")
            -1
        }
    }

    private fun isAirplaneModeOn(): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }

    private fun isWifiEnabled(): Boolean {
        if (!hasNetworkStatePermission()) {
            return false
        }

        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking WiFi state: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking WiFi state", e)
            false
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    private fun getMemoryUsage(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem - memoryInfo.availMem
    }

    private fun getCpuUsage(): Float {
        // CPU usage calculation is complex and requires reading /proc/stat
        // For now, return -1 to indicate unavailable
        return -1f
    }

    private fun getStorageUsage(): Long {
        val statFs = android.os.StatFs(context.filesDir.path)
        val totalBytes = statFs.totalBytes
        val availableBytes = statFs.availableBytes
        return totalBytes - availableBytes
    }

    private fun getScreenBrightness(): Int {
        return try {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) {
            -1
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isInteractive
    }

    private fun getDeviceOrientation(): String {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        return when (windowManager.defaultDisplay.rotation) {
            android.view.Surface.ROTATION_0 -> "Portrait"
            android.view.Surface.ROTATION_90 -> "Landscape"
            android.view.Surface.ROTATION_180 -> "Portrait (upside down)"
            android.view.Surface.ROTATION_270 -> "Landscape (upside down)"
            else -> "Unknown"
        }
    }

    // Permission check methods
    private fun hasNetworkStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
