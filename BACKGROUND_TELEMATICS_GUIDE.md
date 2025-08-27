## Background Telematics Implementation Guide

This guide shows how to integrate the new background telematics system that ensures continuous data collection even when the app is in the background, screen is locked, or app is killed.

## Key Components Created

### 1. TelematicsBackgroundService
- Foreground service that runs continuously
- Handles session lifecycle (start/stop/pause/resume)
- Persists through app state changes
- Uses wake locks for critical location tracking

### 2. TelematicsSessionManager
- Manages session persistence and recovery
- Saves session state to SharedPreferences
- Auto-recovers sessions after app restart
- Handles session history

### 3. EnhancedTelematicsManager
- Wrapper around existing TelemetriManager
- Provides simple API for background sessions
- Integrates with session management

### 4. TelematicsSessionViewModel
- ViewModel for UI integration
- Observes session state and data
- Handles UI interactions

### 5. TelematicsSessionPanel
- Ready-to-use Compose UI component
- Start/stop/pause/resume controls
- Battery optimization warnings
- Session duration display

## How to Integrate into Your App

### Step 1: Update your MainActivity or main screen to include the session panel

```kotlin
// In your SmartFleetManagementScreen or MainActivity
@Composable
fun SmartFleetManagementScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Add the telematics session panel at the top
        TelematicsSessionPanel()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Your existing UI components...
        // ...existing code...
    }
}
```

### Step 2: Start using the enhanced telematics manager

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var telematicsManager: EnhancedTelematicsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the enhanced telematics manager
        telematicsManager = EnhancedTelematicsManager.getInstance(this)
        
        // Check and request battery optimization exemption
        if (BatteryOptimizationHelper.shouldShowBatteryOptimizationRationale(this)) {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }
        
        setContent {
            // Your app content
        }
    }
}
```

### Step 3: Handle permissions in your app

```kotlin
// Request background location permission for reliable tracking
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        BACKGROUND_LOCATION_REQUEST_CODE
    )
}
```

## Key Benefits of This Implementation

### 1. **Persistent Sessions**
- Sessions continue even if app is killed by system
- Automatic recovery when app restarts
- Session state saved to disk

### 2. **Battery Optimization Handling**
- Detects battery optimization status
- Guides users to disable optimization
- Provides fallback behavior

### 3. **Foreground Service**
- Runs as foreground service with notification
- Uses wake locks for critical location tracking
- Proper lifecycle management

### 4. **Session Management**
- Start/stop/pause/resume functionality
- Session history tracking
- Duration tracking
- Error handling

### 5. **Background Location Tracking**
- Continues when screen is locked
- Works when app is in background
- Handles app lifecycle changes

## Usage Examples

### Start an Automotive Session
```kotlin
val telematicsManager = EnhancedTelematicsManager.getInstance(context)
val sessionId = telematicsManager.startAutomotiveSession()
```

### Start with Custom Configuration
```kotlin
val config = TelemetryConfig(
    enableSensorCollection = true,
    enableLocationTracking = true,
    enableAudioTelemetry = false,
    locationUpdateInterval = 1000L,
    batteryOptimizationEnabled = true
)
val sessionId = telematicsManager.startBackgroundSession(config)
```

### Monitor Session State
```kotlin
telematicsManager.sessionState.observe(this) { state ->
    when (state) {
        SessionState.RUNNING -> // Session is active
        SessionState.PAUSED -> // Session is paused
        SessionState.STOPPED -> // Session is stopped
        SessionState.ERROR -> // Handle error
    }
}
```

### Access Telemetry Data
```kotlin
// Location data
telematicsManager.locationData.observe(this) { locationData ->
    // Handle location updates
}

// Sensor data
telematicsManager.sensorData.observe(this) { sensorData ->
    // Handle sensor updates
}
```

## Configuration for Different Use Cases

### Automotive Use Case (Optimized)
```kotlin
telematicsManager.startAutomotiveSession()
// Uses: GPS, Motion sensors, Device state
// Excludes: Microphone, Network monitoring
// Optimized for: Battery life, Accuracy
```

### Custom Configuration
```kotlin
val config = TelemetryConfig(
    enableSensorCollection = true,
    enableLocationTracking = true,
    enableAudioTelemetry = false,
    enableNetworkTelemetry = false,
    enablePerformanceMonitoring = false,
    enableMotionAnalysis = true,
    enableDeviceStateMonitoring = true,
    sensorSamplingRate = SensorSamplingRate.HIGH,
    locationUpdateInterval = 1000L,
    batteryOptimizationEnabled = true
)
```

## Testing Background Operation

1. Start a session
2. Lock the screen
3. Check notification shows session is active
4. Open app after some time
5. Verify session continued and data was collected

## Troubleshooting

### Session Stops When App Goes to Background
- Check battery optimization is disabled
- Verify foreground service permission
- Check background location permission

### No Location Updates
- Ensure location permissions are granted
- Check GPS is enabled
- Verify background location permission

### Service Killed by System
- Disable battery optimization
- Check if device has aggressive power management
- Consider requesting users to pin app in recent apps

This implementation provides a robust solution for continuous telematics data collection that works reliably across different Android versions and device manufacturers.
