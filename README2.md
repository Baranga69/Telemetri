# Telemetri SDK

> **Enterprise-grade Android telemetry and sensor data collection SDK with real-time analytics**

[![Android API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Telemetri is a comprehensive Android SDK designed for collecting, analyzing, and managing telemetry data from mobile devices. Built with modern Android architecture patterns, it provides real-time sensor fusion, location tracking, network monitoring, and advanced motion analysis capabilities.

## 🚀 Key Features

### 📱 **Multi-Modal Data Collection**
- **Sensor Fusion**: Accelerometer, gyroscope, magnetometer, gravity, and linear acceleration
- **High-Precision Location**: GPS tracking with 1-second updates and accuracy filtering
- **Network Analytics**: Real-time network quality monitoring and speed testing
- **Audio Telemetry**: Sound level monitoring and acoustic environment analysis
- **Motion Analysis**: Advanced activity recognition (walking, driving, cycling)
- **Device State**: Battery, performance, and system health monitoring

### 🎯 **Specialized Use Cases**
- **Automotive Telemetry**: Vehicle speed detection with sensor fusion
- **Fitness Tracking**: Step counting, activity recognition, and motion patterns
- **Network Diagnostics**: Comprehensive speed testing with parallel connections
- **Environmental Monitoring**: Sound levels, light, pressure, temperature, humidity
- **Security Analytics**: Device state monitoring and anomaly detection

### 🏗️ **Enterprise Architecture**
- **MVVM Pattern**: Clean separation of concerns with reactive data flow
- **Dependency Injection**: Hilt-powered DI for scalable component management
- **Coroutines**: Efficient async processing with structured concurrency
- **LiveData Streams**: Reactive data observation with lifecycle awareness
- **Room Database**: Local storage with automatic sync capabilities
- **WorkManager**: Reliable background data synchronization

## 📋 Requirements

- **Android SDK**: API level 21+ (Android 5.0+)
- **Kotlin**: 2.0.0+
- **Java**: 11+
- **Compile SDK**: 36

## 🛠️ Installation

### Gradle Setup

Add the Telemetri SDK to your project:

```kotlin
// Project-level build.gradle.kts
allprojects {
    repositories {
        google()
        mavenCentral()
        // Add your repository if SDK is published
    }
}

// App-level build.gradle.kts
dependencies {
    implementation project(':telemetri-sdk')
    
    // Required dependencies
    implementation "androidx.hilt:hilt-android:2.48"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    implementation "androidx.work:work-runtime-ktx:2.8.1"
    kapt "androidx.hilt:hilt-compiler:2.48"
}
```

### Permissions

Add required permissions to your `AndroidManifest.xml`:

```xml
<!-- Core permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Location (for automotive/fitness use cases) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Audio (for environmental monitoring) -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Device state -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## ⚡ Quick Start

### 1. Initialize Your Application

```kotlin
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize telemetry with default configuration
        val telemetriManager = TelemetriManager.getInstance(this)
        
        // Schedule background sync
        BackgroundSyncService.scheduleSync(this)
    }
}
```

### 2. Basic Usage

```kotlin
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var telemetriManager: TelemetriManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start basic telemetry collection
        telemetriManager.startTelemetryCollection()
        
        // Observe comprehensive telemetry data
        telemetriManager.comprehensiveTelemetry.observe(this) { telemetryEvent ->
            // Handle telemetry data
            Log.d("Telemetry", "Sensors: ${telemetryEvent.sensorData?.size}")
            Log.d("Telemetry", "Location: ${telemetryEvent.locationData?.speed}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        telemetriManager.stopTelemetryCollection()
    }
}
```

## 🎯 Use Case Configurations

The SDK provides pre-configured setups for common use cases:

### Automotive Telemetry
```kotlin
val automotiveConfig = TelemetriManager.ConfigPresets.automotiveUseCase()
telemetriManager.startTelemetryCollection(automotiveConfig)

// Observe vehicle speed
telemetriManager.motionAnalysis.observe(this) { motionData ->
    val vehicleSpeed = motionData.vehicleSpeed // m/s
    val activityType = motionData.activityType // IN_VEHICLE, WALKING, etc.
}
```

### Fitness Tracking
```kotlin
val fitnessConfig = TelemetriManager.ConfigPresets.fitnessUseCase()
telemetriManager.startTelemetryCollection(fitnessConfig)

// Monitor step count and activity
telemetriManager.motionAnalysis.observe(this) { motionData ->
    val stepCount = motionData.stepCount
    val stepFrequency = motionData.stepFrequency
}
```

### Network Diagnostics
```kotlin
val networkConfig = TelemetriManager.ConfigPresets.networkDiagnosticsUseCase()
telemetriManager.startTelemetryCollection(networkConfig)

// Run speed test
telemetriManager.startNetworkSpeedTest()
telemetriManager.speedTestResult.observe(this) { result ->
    val downloadSpeed = result.downloadSpeed // Mbps
    val uploadSpeed = result.uploadSpeed     // Mbps
    val ping = result.ping                   // ms
}
```

### Environmental Monitoring
```kotlin
val environmentalConfig = TelemetriManager.ConfigPresets.environmentalUseCase()
telemetriManager.startTelemetryCollection(environmentalConfig)

// Monitor environmental conditions
telemetriManager.audioTelemetry.observe(this) { audioData ->
    val soundLevel = audioData.soundLevel    // dB
    val frequency = audioData.frequency      // Hz
}
```

## 🔧 Advanced Configuration

### Custom Telemetry Configuration

```kotlin
val customConfig = TelemetryConfig(
    enableSensorCollection = true,
    enableLocationTracking = true,
    enableAudioTelemetry = false,          // Disable for privacy
    enableNetworkTelemetry = true,
    enablePerformanceMonitoring = true,
    enableMotionAnalysis = true,
    enableDeviceStateMonitoring = true,
    
    sensorSamplingRate = SensorSamplingRate.HIGH,
    locationUpdateInterval = 2000L,         // 2 seconds
    batteryOptimizationEnabled = true,
    
    audioAnalysisEnabled = false,
    networkQualityMonitoring = true
)

telemetriManager.startTelemetryCollection(customConfig)
```

### Sensor Management

```kotlin
val sensorService = SensorService(context)

// Configure specific sensors
sensorService.enableSensorType(SensorType.ACCELEROMETER)
sensorService.enableSensorType(SensorType.GYROSCOPE)
sensorService.disableSensorType(SensorType.MAGNETOMETER)

// Check hardware support
val supportedSensors = sensorService.getAllHardwareSupportedSensors()
val isGyroSupported = sensorService.isHardwareSupported(SensorType.GYROSCOPE)

// Get detailed sensor information
val sensorInfo = sensorService.getSensorInfo(SensorType.ACCELEROMETER)
println("Sensor: ${sensorInfo.sensorName}, Power: ${sensorInfo.power}mA")
```

### Network Speed Testing

```kotlin
// High-performance parallel speed testing
telemetriManager.startNetworkSpeedTest()

telemetriManager.speedTestResult.observe(this) { result ->
    when {
        result.isTestRunning -> {
            val progress = result.progress * 100
            val currentTest = result.currentTestType?.name
            // Update UI with progress
        }
        result.errorMessage != null -> {
            // Handle test error
        }
        else -> {
            // Test completed successfully
            val downloadMbps = result.downloadSpeed
            val uploadMbps = result.uploadSpeed
            val pingMs = result.ping
            val jitterMs = result.jitter
            val packetLoss = result.packetLoss
        }
    }
}
```

## 📊 Data Models

### Comprehensive Telemetry Event

```kotlin
data class ComprehensiveTelemetryEvent(
    val sessionId: String,
    val timestamp: Long,
    val sensorData: List<SensorData>?,
    val locationData: LocationData?,
    val audioTelemetryData: AudioTelemetryData?,
    val networkTelemetryData: NetworkTelemetryData?,
    val performanceTelemetryData: PerformanceTelemetryData?,
    val motionData: MotionData?,
    val deviceState: DeviceStateData?
)
```

### Motion Analysis

```kotlin
data class MotionData(
    val accelerationMagnitude: Float,
    val gyroscopeMagnitude: Float,
    val activityType: ActivityType,        // STILL, WALKING, RUNNING, IN_VEHICLE
    val confidence: Float,                 // 0.0 - 1.0
    val stepCount: Int,
    val stepFrequency: Float,              // steps per minute
    val vehicleSpeed: Float,               // m/s (sensor fusion)
    val timestamp: Long
)
```

### Network Analytics

```kotlin
data class SpeedTestResult(
    val downloadSpeed: Float,              // Mbps
    val uploadSpeed: Float,                // Mbps
    val ping: Float,                       // ms
    val jitter: Float,                     // ms
    val packetLoss: Float,                 // percentage
    val isTestRunning: Boolean,
    val currentTestType: NetworkTestType?, // DOWNLOAD, UPLOAD, PING
    val progress: Float                    // 0.0 - 1.0
)
```

## 🎨 UI Components

The SDK includes pre-built UI components for rapid development:

### Vehicle Speedometer
```kotlin
VehicleSpeedometerWidget(
    currentSpeed = vehicleSpeed,
    isRecording = isRecording,
    speedUnit = SpeedUnit.KPH,
    onToggleRecording = { /* handle recording */ },
    onToggleUnit = { /* switch KPH/MPH */ }
)
```

### Network Speed Test
```kotlin
NetworkSpeedTestWidget(
    networkData = speedTestData,
    onStartTest = { telemetriManager.startNetworkSpeedTest() },
    onStopTest = { telemetriManager.stopNetworkSpeedTest() }
)
```

### Signal Strength Gauge
```kotlin
SignalStrengthGauge(
    signalStrength = networkData.signalStrength,
    networkType = networkData.networkType
)
```

## 🔒 Privacy & Security

### Data Privacy
- **Opt-in Collection**: All data collection requires explicit configuration
- **Local Storage**: Data stored locally by default with optional sync
- **Permission Management**: Granular control over required permissions
- **Audio Privacy**: Audio telemetry can be completely disabled

### Security Features
- **Encrypted Storage**: Local data encrypted using Android Keystore
- **Secure Sync**: HTTPS-only communication with backend services
- **Data Minimization**: Collect only configured data types
- **Session Management**: Automatic session isolation and cleanup

## ⚡ Performance Optimization

### Battery Optimization
```kotlin
val batteryOptimizedConfig = TelemetryConfig(
    sensorSamplingRate = SensorSamplingRate.NORMAL,    // Reduced frequency
    locationUpdateInterval = 5000L,                     // 5-second updates
    batteryOptimizationEnabled = true,                  // Smart sensor management
    enablePerformanceMonitoring = false                // Disable if not needed
)
```

### Memory Management
- **Efficient Buffering**: Smart data buffering to minimize memory usage
- **Lifecycle Awareness**: Automatic pause/resume based on app lifecycle
- **Resource Cleanup**: Proper cleanup of sensors and services

### Background Processing
- **WorkManager Integration**: Reliable background sync with system optimization
- **Adaptive Scheduling**: Intelligent sync scheduling based on device state
- **Network Awareness**: Sync only on appropriate network conditions

## 🧪 Testing

### Unit Testing
```kotlin
@Test
fun testMotionAnalysis() {
    val motionEngine = MotionAnalysisEngine(context)
    motionEngine.startAnalysis()
    
    // Verify sensor registration
    assertTrue(motionEngine.isAnalyzing)
}
```

### Integration Testing
```kotlin
@Test
fun testTelemetryDataFlow() {
    val telemetriManager = TelemetriManager.getInstance(context)
    val config = TelemetryConfig(enableSensorCollection = true)
    
    telemetriManager.startTelemetryCollection(config)
    
    // Verify data collection
    telemetriManager.comprehensiveTelemetry.observeForever { data ->
        assertNotNull(data.sensorData)
    }
}
```

## 📖 API Reference

### TelemetriManager
- `getInstance(context)` - Get singleton instance
- `startTelemetryCollection(config)` - Begin data collection
- `stopTelemetryCollection()` - Stop data collection
- `startNetworkSpeedTest()` - Run network speed test
- `configureTelemetry(config)` - Update configuration

### Configuration Presets
- `ConfigPresets.automotiveUseCase()` - Vehicle telemetry
- `ConfigPresets.fitnessUseCase()` - Fitness tracking
- `ConfigPresets.networkDiagnosticsUseCase()` - Network monitoring
- `ConfigPresets.environmentalUseCase()` - Environmental sensing
- `ConfigPresets.securityUseCase()` - Security monitoring

### Sensor Service
- `enableSensorType(type)` - Enable specific sensor
- `disableSensorType(type)` - Disable specific sensor
- `getAvailableSensors()` - Get supported sensors
- `getSensorInfo(type)` - Get detailed sensor information

## 🚀 Demo Application

The project includes a comprehensive demo application showcasing all SDK capabilities:

- **Real-time Dashboard**: Live telemetry data visualization
- **Use Case Screens**: Dedicated screens for each use case
- **Interactive Controls**: Start/stop collection and configuration
- **Data Export**: Export collected data for analysis

To run the demo:
```bash
./gradlew :app:installDebug
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run the demo app

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for code formatting
- Write comprehensive tests for new features

## 📄 License

```
MIT License

Copyright (c) 2025 Commerin Telemetri

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📞 Support

- **Documentation**: [API Documentation](docs/api.md)
- **Issues**: [GitHub Issues](https://github.com/commerin/telemetri/issues)
- **Email**: support@commerin.com
- **Discord**: [Community Discord](https://discord.gg/telemetri)

---

**Built with ❤️ by the Commerin team**

*Empowering mobile applications with enterprise-grade telemetry and analytics*
