package com.commerin.telemetri.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.StatFs
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileReader
import java.io.BufferedReader

class PerformanceTelemetryService(private val context: Context) {
    companion object {
        private const val TAG = "PerfTelemetryService" // Shortened to fit 23 char limit
        private const val MONITORING_INTERVAL_MS = 10000L // 10 seconds
        private const val CPU_STAT_FILE = "/proc/stat"
        private const val MEMINFO_FILE = "/proc/meminfo"
        private const val THERMAL_ZONE_PATH = "/sys/class/thermal/thermal_zone"
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _performanceData = MutableLiveData<PerformanceTelemetryData>()
    val performanceData: LiveData<PerformanceTelemetryData> = _performanceData

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    private var isMonitoring = false

    // For CPU usage calculation
    private var lastTotalCpuTime = 0L
    private var lastIdleCpuTime = 0L

    fun startPerformanceMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Performance monitoring already running")
            return
        }

        isMonitoring = true

        monitoringJob = scope.launch {
            while (isMonitoring) {
                try {
                    collectPerformanceTelemetry()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in performance monitoring loop", e)
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }

        Log.d(TAG, "Performance telemetry monitoring started")
    }

    fun stopPerformanceMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        Log.d(TAG, "Performance telemetry monitoring stopped")
    }

    private suspend fun collectPerformanceTelemetry() {
        try {
            val cpuUsage = getCpuUsage()
            val memoryUsage = getMemoryUsageInfo()
            val batteryInfo = getBatteryTelemetryInfo()
            val thermalState = getThermalStateInfo()
            val storageInfo = getStorageUsageInfo()
            val networkUsage = getNetworkUsageInfo()

            val performanceTelemetry = PerformanceTelemetryData(
                cpuUsage = cpuUsage,
                memoryUsage = memoryUsage,
                batteryInfo = batteryInfo,
                thermalState = thermalState,
                storageInfo = storageInfo,
                networkUsage = networkUsage,
                timestamp = System.currentTimeMillis()
            )

            withContext(Dispatchers.Main) {
                _performanceData.postValue(performanceTelemetry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting performance telemetry", e)
        }
    }

    private suspend fun getCpuUsage(): Float {
        return withContext(Dispatchers.IO) {
            try {
                val cpuInfo = readCpuInfo()
                if (cpuInfo != null) {
                    val totalDelta = cpuInfo.first - lastTotalCpuTime
                    val idleDelta = cpuInfo.second - lastIdleCpuTime

                    lastTotalCpuTime = cpuInfo.first
                    lastIdleCpuTime = cpuInfo.second

                    if (totalDelta > 0) {
                        ((totalDelta - idleDelta).toFloat() / totalDelta) * 100f
                    } else {
                        0f
                    }
                } else {
                    // Fallback: estimate from Debug class (less accurate)
                    val memInfo = Debug.MemoryInfo()
                    Debug.getMemoryInfo(memInfo)
                    // This is a very rough estimation
                    (memInfo.getTotalPrivateDirty() * 100f) / getTotalMemory()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error calculating CPU usage", e)
                0f
            }
        }
    }

    private fun readCpuInfo(): Pair<Long, Long>? {
        return try {
            BufferedReader(FileReader(CPU_STAT_FILE)).use { reader ->
                val line = reader.readLine()
                if (line != null && line.startsWith("cpu ")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 5) {
                        val user = parts[1].toLong()
                        val nice = parts[2].toLong()
                        val system = parts[3].toLong()
                        val idle = parts[4].toLong()
                        val iowait = if (parts.size > 5) parts[5].toLong() else 0L
                        val irq = if (parts.size > 6) parts[6].toLong() else 0L
                        val softirq = if (parts.size > 7) parts[7].toLong() else 0L

                        val total = user + nice + system + idle + iowait + irq + softirq
                        Pair(total, idle)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading CPU info", e)
            null
        }
    }

    private fun getMemoryUsageInfo(): MemoryUsageInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availableRam = memInfo.availMem
        val usedRam = totalRam - availableRam

        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)
        val appMemoryUsage = (debugMemInfo.getTotalPss() * 1024).toLong()

        val memoryPressure = when {
            memInfo.lowMemory -> "High"
            (availableRam.toFloat() / totalRam) < 0.2f -> "Medium"
            else -> "Low"
        }

        return MemoryUsageInfo(
            totalRam = totalRam,
            availableRam = availableRam,
            usedRam = usedRam,
            appMemoryUsage = appMemoryUsage,
            memoryPressure = memoryPressure,
            swapUsage = getSwapUsage()
        )
    }

    private fun getSwapUsage(): Long {
        return try {
            BufferedReader(FileReader(MEMINFO_FILE)).use { reader ->
                var swapTotal = 0L
                var swapFree = 0L

                // Replace reader.lines().forEach with manual line reading for API 21+ compatibility
                var line = reader.readLine()
                while (line != null) {
                    when {
                        line.startsWith("SwapTotal:") -> {
                            swapTotal = line.split("\\s+".toRegex())[1].toLong() * 1024
                        }
                        line.startsWith("SwapFree:") -> {
                            swapFree = line.split("\\s+".toRegex())[1].toLong() * 1024
                        }
                    }
                    line = reader.readLine()
                }

                swapTotal - swapFree
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading swap usage", e)
            0L
        }
    }

    private fun getBatteryTelemetryInfo(): BatteryTelemetryInfo {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0f

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargingState = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
            else -> ChargingState.UNKNOWN
        }

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthString = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        return BatteryTelemetryInfo(
            level = batteryPct,
            temperature = temperature,
            voltage = voltage,
            current = getBatteryCurrent(),
            capacity = getBatteryCapacity(),
            health = healthString,
            chargingState = chargingState,
            powerSource = powerSource,
            estimatedTimeRemaining = estimateBatteryTimeRemaining(batteryPct, chargingState)
        )
    }

    private fun getBatteryCurrent(): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery current", e)
            0f
        }
    }

    private fun getBatteryCapacity(): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100.0f
            } else {
                100f // Default assumption
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery capacity", e)
            100f
        }
    }

    private fun estimateBatteryTimeRemaining(batteryLevel: Int, chargingState: ChargingState): Long {
        return when (chargingState) {
            ChargingState.CHARGING -> {
                // Rough estimation: assume 2 hours for full charge from current level
                ((100 - batteryLevel) * 2 * 60 / 100).toLong()
            }
            ChargingState.DISCHARGING -> {
                // Rough estimation: assume 8 hours total battery life
                (batteryLevel * 8 * 60 / 100).toLong()
            }
            else -> -1L
        }
    }

    private fun getThermalStateInfo(): ThermalStateInfo {
        val batteryTemp = getBatteryTelemetryInfo().temperature
        val cpuTemp = getCpuTemperature()
        val ambientTemp = getAmbientTemperature()

        val thermalState = when {
            cpuTemp > 85 || batteryTemp > 45 -> "Critical"
            cpuTemp > 75 || batteryTemp > 40 -> "Severe"
            cpuTemp > 65 || batteryTemp > 35 -> "Moderate"
            cpuTemp > 55 || batteryTemp > 30 -> "Light"
            else -> "Normal"
        }

        val thermalThrottling = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Use the correct thermal state API for Android Q+
                powerManager.currentThermalStatus != PowerManager.THERMAL_STATUS_NONE
            } catch (e: Exception) {
                Log.w(TAG, "Error reading thermal status", e)
                cpuTemp > 70 // Fallback estimation
            }
        } else {
            cpuTemp > 70 // Estimate based on CPU temperature for older versions
        }

        return ThermalStateInfo(
            cpuTemperature = cpuTemp,
            batteryTemperature = batteryTemp,
            ambientTemperature = ambientTemp,
            thermalThrottling = thermalThrottling,
            thermalState = thermalState,
            fanSpeed = 0 // Most mobile devices don't have fans
        )
    }

    private fun getCpuTemperature(): Float {
        return try {
            // Try to read from thermal zones
            for (i in 0..10) {
                val thermalFile = File("$THERMAL_ZONE_PATH$i/temp")
                if (thermalFile.exists()) {
                    val temp = thermalFile.readText().trim().toFloatOrNull()
                    if (temp != null) {
                        return temp / 1000f // Convert from milli-Celsius
                    }
                }
            }
            25f // Default room temperature
        } catch (e: Exception) {
            Log.w(TAG, "Error reading CPU temperature", e)
            25f
        }
    }

    private fun getAmbientTemperature(): Float {
        // This would typically come from environmental sensors
        // For now, return a default value
        return 25f
    }

    private fun getStorageUsageInfo(): StorageUsageInfo {
        val internalDir = context.filesDir
        val externalDir = context.getExternalFilesDir(null)

        val internalStat = StatFs(internalDir.path)
        val totalInternal = internalStat.blockCountLong * internalStat.blockSizeLong
        val availableInternal = internalStat.availableBlocksLong * internalStat.blockSizeLong

        var totalExternal = 0L
        var availableExternal = 0L

        externalDir?.let { dir ->
            val externalStat = StatFs(dir.path)
            totalExternal = externalStat.blockCountLong * externalStat.blockSizeLong
            availableExternal = externalStat.availableBlocksLong * externalStat.blockSizeLong
        }

        val appDataUsage = getAppDataUsage()
        val cacheUsage = getCacheUsage()

        return StorageUsageInfo(
            totalInternalStorage = totalInternal,
            availableInternalStorage = availableInternal,
            totalExternalStorage = totalExternal,
            availableExternalStorage = availableExternal,
            appDataUsage = appDataUsage,
            cacheUsage = cacheUsage
        )
    }

    private fun getAppDataUsage(): Long {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            if (applicationInfo?.dataDir != null) {
                val appDir = File(applicationInfo.dataDir)
                calculateDirectorySize(appDir)
            } else {
                Log.w(TAG, "ApplicationInfo or dataDir is null")
                0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating app data usage", e)
            0L
        }
    }

    private fun getCacheUsage(): Long {
        return try {
            calculateDirectorySize(context.cacheDir)
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating cache usage", e)
            0L
        }
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) {
                        calculateDirectorySize(file)
                    } else {
                        file.length()
                    }
                }
            } else {
                size = directory.length()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating directory size for ${directory.path}", e)
        }
        return size
    }

    private fun getNetworkUsageInfo(): NetworkUsageInfo {
        // This is a simplified implementation
        // In a production environment, you'd want to track actual network usage over time
        return NetworkUsageInfo(
            totalDataSent = 0L,
            totalDataReceived = 0L,
            wifiDataSent = 0L,
            wifiDataReceived = 0L,
            mobileDataSent = 0L,
            mobileDataReceived = 0L,
            backgroundDataUsage = 0L,
            foregroundDataUsage = 0L
        )
    }

    private fun getTotalMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    // =====================================
    // Power Management Methods
    // =====================================

    private var wasPausedForPowerSaving = false
    private var wasMonitoringBeforePause = false

    /**
     * Pause performance telemetry for power saving
     */
    fun pauseForPowerSaving() {
        if (!isMonitoring) {
            Log.d(TAG, "Performance telemetry not running - nothing to pause")
            return
        }

        Log.d(TAG, "Pausing performance telemetry for power saving")
        wasMonitoringBeforePause = isMonitoring
        wasPausedForPowerSaving = true
        stopPerformanceMonitoring()
    }

    /**
     * Resume performance telemetry from power saving mode
     */
    fun resumeFromPowerSaving() {
        if (!wasPausedForPowerSaving) {
            Log.d(TAG, "Performance telemetry was not paused for power saving")
            return
        }

        Log.d(TAG, "Resuming performance telemetry from power saving")
        wasPausedForPowerSaving = false

        if (wasMonitoringBeforePause) {
            startPerformanceMonitoring()
        }

        wasMonitoringBeforePause = false
    }

    fun cleanup() {
        stopPerformanceMonitoring()
        scope.cancel()
    }
}
