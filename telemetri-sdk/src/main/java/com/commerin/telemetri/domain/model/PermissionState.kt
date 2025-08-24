package com.commerin.telemetri.domain.model

/**
 * Represents the state of required permissions for telemetry services
 */
sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object NotRequested : PermissionState()
    data class PermanentlyDenied(val permission: String) : PermissionState()
}

/**
 * Callback interface for permission state changes
 */
interface PermissionStateCallback {
    /**
     * Called when audio recording permission state changes
     * @param state Current permission state
     * @param permission The permission string that changed
     */
    fun onAudioPermissionStateChanged(state: PermissionState, permission: String)

    /**
     * Called when location permission state changes
     * @param state Current permission state
     * @param permission The permission string that changed
     */
    fun onLocationPermissionStateChanged(state: PermissionState, permission: String)

    /**
     * Called when connectivity permission state changes
     * @param state Current permission state
     * @param permission The permission string that changed
     */
    fun onConnectivityPermissionStateChanged(state: PermissionState, permission: String)

    /**
     * Called when device state permission changes
     * @param state Current permission state
     * @param permission The permission string that changed
     */
    fun onDeviceStatePermissionChanged(state: PermissionState, permission: String)
}

/**
 * Exception thrown when required permissions are not granted
 */
class TelemetryPermissionException(
    val permission: String,
    val permissionState: PermissionState,
    message: String
) : Exception(message)

/**
 * Required permissions for telemetry services organized by functionality
 */
object TelemetryPermissions {
    // Audio telemetry permissions
    const val AUDIO_RECORDING = android.Manifest.permission.RECORD_AUDIO

    // Location telemetry permissions
    const val LOCATION_FINE = android.Manifest.permission.ACCESS_FINE_LOCATION
    const val LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION

    // Connectivity telemetry permissions
    const val WIFI_STATE = android.Manifest.permission.ACCESS_WIFI_STATE
    const val BLUETOOTH_CONNECT = android.Manifest.permission.BLUETOOTH_CONNECT
    const val NETWORK_STATE = android.Manifest.permission.ACCESS_NETWORK_STATE

    // Device state permissions
    const val READ_PHONE_STATE = android.Manifest.permission.READ_PHONE_STATE

    /**
     * Get all required permissions for full telemetry functionality
     */
    fun getAllRequiredPermissions(): Array<String> = arrayOf(
        AUDIO_RECORDING,
        LOCATION_FINE,
        LOCATION_COARSE,
        WIFI_STATE,
        BLUETOOTH_CONNECT,
        NETWORK_STATE,
        READ_PHONE_STATE
    )

    /**
     * Get permissions required specifically for audio telemetry
     */
    fun getAudioPermissions(): Array<String> = arrayOf(AUDIO_RECORDING)

    /**
     * Get permissions required for location telemetry
     */
    fun getLocationPermissions(): Array<String> = arrayOf(LOCATION_FINE, LOCATION_COARSE)

    /**
     * Get permissions required for connectivity telemetry
     */
    fun getConnectivityPermissions(): Array<String> = arrayOf(WIFI_STATE, BLUETOOTH_CONNECT, NETWORK_STATE)

    /**
     * Get permissions required for device state telemetry
     */
    fun getDeviceStatePermissions(): Array<String> = arrayOf(READ_PHONE_STATE)

    /**
     * Get runtime permissions (permissions that require user approval)
     */
    fun getRuntimePermissions(): Array<String> = arrayOf(
        AUDIO_RECORDING,
        LOCATION_FINE,
        LOCATION_COARSE,
        BLUETOOTH_CONNECT,
        READ_PHONE_STATE
    )

    /**
     * Get normal permissions (automatically granted)
     */
    fun getNormalPermissions(): Array<String> = arrayOf(
        WIFI_STATE,
        NETWORK_STATE
    )

    /**
     * Check if a permission is a runtime permission
     */
    fun isRuntimePermission(permission: String): Boolean {
        return getRuntimePermissions().contains(permission)
    }
}
