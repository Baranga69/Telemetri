package com.commerin.telemetri.domain.model

// Enhanced audio telemetry data model
data class AudioTelemetryData(
    val amplitude: Float,              // Raw amplitude level
    val decibels: Float,              // Sound level in decibels
    val dominantFrequency: Float,     // Most prominent frequency (Hz)
    val spectralCentroid: Float,      // Center of mass of spectrum (Hz)
    val spectralRolloff: Float,       // Frequency below which 85% of energy lies
    val spectralFlux: Float,          // Rate of change in spectrum
    val zeroCrossingRate: Float,      // Rate of sign changes (indicates pitch)
    val soundClassification: SoundClassification, // Type of sound detected
    val frequencySpectrum: FloatArray, // Frequency spectrum data
    val isVoiceDetected: Boolean,     // Human voice detection
    val noiseLevelCategory: String,   // Categorical noise level
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTelemetryData
        return amplitude == other.amplitude &&
               decibels == other.decibels &&
               dominantFrequency == other.dominantFrequency &&
               timestamp == other.timestamp &&
               frequencySpectrum.contentEquals(other.frequencySpectrum)
    }

    override fun hashCode(): Int {
        var result = amplitude.hashCode()
        result = 31 * result + decibels.hashCode()
        result = 31 * result + dominantFrequency.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + frequencySpectrum.contentHashCode()
        return result
    }
}

// Sound classification categories for environmental context
enum class SoundClassification {
    SILENCE,           // No significant sound
    HUMAN_VOICE,       // Human speech or conversation
    TRAFFIC,           // Vehicle traffic noise
    MECHANICAL,        // Machinery or mechanical sounds
    NATURE,            // Natural sounds (birds, wind, water)
    MUSIC,             // Musical content
    AMBIENT,           // General environmental noise
    CONSTRUCTION,      // Construction or industrial sounds
    UNKNOWN            // Unclassified sound
}

// Network telemetry data for connectivity analysis
data class NetworkTelemetryData(
    val networkType: NetworkType,
    val signalStrength: Int,          // Signal strength (dBm)
    val signalQuality: Float,         // Signal quality (0-1)
    val connectionSpeed: Long,        // Current connection speed (bps)
    val latency: Long,               // Network latency (ms)
    val packetLoss: Float,           // Packet loss percentage
    val wifiInfo: WifiTelemetryInfo?, // WiFi specific data
    val cellularInfo: CellularTelemetryInfo?, // Cellular specific data
    val bluetoothDevices: List<BluetoothDeviceInfo>, // Nearby Bluetooth devices
    val timestamp: Long
)

data class WifiTelemetryInfo(
    val ssid: String,
    val bssid: String,
    val frequency: Int,              // WiFi frequency (MHz)
    val rssi: Int,                   // Received signal strength (dBm)
    val linkSpeed: Int,              // Link speed (Mbps)
    val ipAddress: String,
    val macAddress: String,
    val channelWidth: Int,           // Channel bandwidth
    val wifiStandard: String,        // 802.11 standard (n, ac, ax, etc.)
    val nearbyNetworks: List<NearbyWifiNetwork>
)

data class CellularTelemetryInfo(
    val operatorName: String,
    val mcc: String,                 // Mobile Country Code
    val mnc: String,                 // Mobile Network Code
    val cellId: Long,               // Cell tower ID
    val lac: Int,                   // Location Area Code
    val signalStrength: Int,        // Signal strength (dBm)
    val signalType: String,         // 2G, 3G, 4G, 5G
    val dataActivity: String,       // Data activity state
    val roaming: Boolean
)

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val deviceClass: Int,
    val rssi: Int,                  // Signal strength
    val bondState: String,          // Paired, bonding, not bonded
    val deviceType: String          // Classic, LE, Dual
)

data class NearbyWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String
)

enum class NetworkType {
    WIFI,
    CELLULAR_2G,
    CELLULAR_3G,
    CELLULAR_4G,
    CELLULAR_5G,
    ETHERNET,
    BLUETOOTH,
    VPN,
    UNKNOWN,
    DISCONNECTED
}

// Performance telemetry data for device health monitoring
data class PerformanceTelemetryData(
    val cpuUsage: Float,             // CPU usage percentage (0-100)
    val memoryUsage: MemoryUsageInfo, // Memory consumption details
    val batteryInfo: BatteryTelemetryInfo, // Enhanced battery data
    val thermalState: ThermalStateInfo, // Device temperature data
    val storageInfo: StorageUsageInfo, // Storage usage details
    val networkUsage: NetworkUsageInfo, // Data usage statistics
    val timestamp: Long
)

data class MemoryUsageInfo(
    val totalRam: Long,              // Total device RAM (bytes)
    val availableRam: Long,          // Available RAM (bytes)
    val usedRam: Long,              // Used RAM (bytes)
    val appMemoryUsage: Long,       // Current app memory usage (bytes)
    val memoryPressure: String,     // Low, Medium, High
    val swapUsage: Long            // Swap usage if available (bytes)
)

data class BatteryTelemetryInfo(
    val level: Int,                  // Battery percentage (0-100)
    val temperature: Float,          // Battery temperature (°C)
    val voltage: Float,             // Battery voltage (V)
    val current: Float,             // Battery current (mA)
    val capacity: Float,            // Battery capacity (mAh)
    val health: String,             // Battery health status
    val chargingState: ChargingState, // Charging status
    val powerSource: String,        // AC, USB, Wireless, Battery
    val estimatedTimeRemaining: Long // Estimated battery time (minutes)
)

enum class ChargingState {
    NOT_CHARGING,
    CHARGING,
    DISCHARGING,
    FULL,
    UNKNOWN
}

data class ThermalStateInfo(
    val cpuTemperature: Float,       // CPU temperature (°C)
    val batteryTemperature: Float,   // Battery temperature (°C)
    val ambientTemperature: Float,   // Ambient temperature (°C)
    val thermalThrottling: Boolean,  // Is thermal throttling active
    val thermalState: String,        // Normal, Light, Moderate, Severe, Critical
    val fanSpeed: Int               // Fan speed if available (RPM)
)

data class StorageUsageInfo(
    val totalInternalStorage: Long,  // Total internal storage (bytes)
    val availableInternalStorage: Long, // Available internal storage (bytes)
    val totalExternalStorage: Long,  // Total external storage (bytes)
    val availableExternalStorage: Long, // Available external storage (bytes)
    val appDataUsage: Long,         // Current app data usage (bytes)
    val cacheUsage: Long           // Cache usage (bytes)
)

data class NetworkUsageInfo(
    val totalDataSent: Long,         // Total data sent (bytes)
    val totalDataReceived: Long,     // Total data received (bytes)
    val wifiDataSent: Long,         // WiFi data sent (bytes)
    val wifiDataReceived: Long,     // WiFi data received (bytes)
    val mobileDataSent: Long,       // Mobile data sent (bytes)
    val mobileDataReceived: Long,   // Mobile data received (bytes)
    val backgroundDataUsage: Long,  // Background data usage (bytes)
    val foregroundDataUsage: Long   // Foreground data usage (bytes)
)

// Device state data for comprehensive telemetry collection
data class DeviceStateData(
    val batteryLevel: Int,           // Battery percentage (0-100)
    val batteryTemperature: Float,   // Battery temperature in Celsius
    val isCharging: Boolean,         // Whether device is charging
    val chargingType: String,        // AC, USB, Wireless, Not charging
    val networkType: String,         // WiFi, 4G LTE, 5G, Cellular, etc.
    val signalStrength: Int,         // Signal strength (-1 if unavailable)
    val isAirplaneModeOn: Boolean,   // Airplane mode status
    val isWifiEnabled: Boolean,      // WiFi connectivity status
    val isBluetoothEnabled: Boolean, // Bluetooth status
    val memoryUsage: Long,           // Used memory in bytes
    val cpuUsage: Float,             // CPU usage percentage (-1 if unavailable)
    val storageUsage: Long,          // Used storage in bytes
    val screenBrightness: Int,       // Screen brightness level
    val isScreenOn: Boolean,         // Screen power state
    val deviceOrientation: String,   // Portrait, Landscape, etc.
    val timestamp: Long              // Timestamp when data was collected
)

// Environmental data for context awareness
data class EnvironmentalData(
    val ambientTemperature: Float,       // Temperature in Celsius
    val lightLevel: Float,               // Ambient light in lux
    val pressure: Float,                 // Atmospheric pressure in hPa
    val humidity: Float,                 // Relative humidity percentage
    val proximityDistance: Float,        // Proximity sensor distance
    val noiseLevel: Float,               // Ambient noise level (if available)
    val uvIndex: Float,                  // UV index (if available)
    val airQuality: Float,               // Air quality index (if available)
    val timestamp: Long
)

// Comprehensive telemetry event combining all data types
data class ComprehensiveTelemetryEvent(
    val eventId: String,
    val location: LocationData?,
    val motion: MotionData?,
    val sensors: List<SensorData>,
    val deviceState: DeviceStateData?,
    val environmental: EnvironmentalData?,
    val userContext: UserContextData?,
    val timestamp: Long,
    val sessionId: String,                // Session identifier
    val eventType: TelemetryEventType,    // Type of telemetry event
    val metadata: Map<String, Any>        // Additional metadata
)

// User context data for personalized telemetry
data class UserContextData(
    val activityType: ActivityType,
    val confidence: Float,
    val locationContext: LocationContext,
    val deviceUsagePattern: String,
    val timeOfDay: String,
    val dayOfWeek: String,
    val timestamp: Long
)

enum class LocationContext {
    HOME, WORK, COMMUTE, TRAVEL, OUTDOOR, INDOOR, UNKNOWN
}

enum class TelemetryEventType {
    LOCATION_UPDATE,        // Standard location update
    MOTION_EVENT,           // Motion/sensor event
    ACTIVITY_CHANGE,        // Activity recognition change
    DEVICE_STATE_CHANGE,    // Device state change
    ENVIRONMENTAL_CHANGE,   // Environmental condition change
    GEOFENCE_EVENT,         // Geofence entry/exit
    SIGNIFICANT_MOTION,     // Significant motion detected
    CUSTOM_EVENT,           // Custom application event
    VEHICLE_TELEMETRY,      // Vehicle-specific telemetry
    FITNESS_TRACKING,       // Fitness tracking events
    ENVIRONMENTAL_MONITORING, // Environmental monitoring
    CONNECTIVITY_CHANGE,    // Network connectivity changes
    GENERAL_TELEMETRY      // General telemetry events
}
