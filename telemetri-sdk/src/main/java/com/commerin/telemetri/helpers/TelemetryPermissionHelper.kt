package com.commerin.telemetri.helpers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.commerin.telemetri.domain.model.PermissionState
import com.commerin.telemetri.domain.model.PermissionStateCallback
import com.commerin.telemetri.domain.model.TelemetryPermissions
import com.commerin.telemetri.utils.PermissionUtils

/**
 * Helper class to simplify permission requests for telemetry services
 * This class should be used by apps integrating the telemetry SDK
 *
 * Note: This version uses direct permission requests instead of ActivityResultLauncher
 * to avoid lifecycle timing issues with Compose
 */
class TelemetryPermissionHelper(
    private val activity: FragmentActivity,
    private val callback: PermissionStateCallback? = null
) {

    /**
     * Request all runtime permissions for full telemetry functionality
     */
    fun requestAllPermissions() {
        val missingPermissions = PermissionUtils.getMissingRuntimePermissions(activity)

        if (missingPermissions.isEmpty()) {
            // All permissions already granted
            notifyAllPermissionsGranted()
            return
        }

        showPermissionRationale(missingPermissions, "All Telemetry Features")
    }

    /**
     * Request audio recording permission with rationale dialog
     */
    fun requestAudioPermission() {
        val missingPermissions = PermissionUtils.getMissingAudioPermissions(activity)

        if (missingPermissions.isEmpty()) {
            callback?.onAudioPermissionStateChanged(
                PermissionState.Granted,
                TelemetryPermissions.AUDIO_RECORDING
            )
            return
        }

        showPermissionRationale(missingPermissions, "Audio Telemetry")
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions() {
        val missingPermissions = PermissionUtils.getMissingLocationPermissions(activity)

        if (missingPermissions.isEmpty()) {
            callback?.onLocationPermissionStateChanged(
                PermissionState.Granted,
                TelemetryPermissions.LOCATION_FINE
            )
            return
        }

        showPermissionRationale(missingPermissions, "Location Telemetry")
    }

    /**
     * Request connectivity permissions
     */
    fun requestConnectivityPermissions() {
        val missingPermissions = PermissionUtils.getMissingConnectivityPermissions(activity)

        if (missingPermissions.isEmpty()) {
            callback?.onConnectivityPermissionStateChanged(
                PermissionState.Granted,
                TelemetryPermissions.BLUETOOTH_CONNECT
            )
            return
        }

        // Filter only runtime permissions for connectivity
        val runtimeMissingPermissions = missingPermissions.filter {
            TelemetryPermissions.isRuntimePermission(it)
        }

        if (runtimeMissingPermissions.isNotEmpty()) {
            showPermissionRationale(runtimeMissingPermissions, "Connectivity Telemetry")
        } else {
            // Only normal permissions missing, automatically granted
            callback?.onConnectivityPermissionStateChanged(
                PermissionState.Granted,
                TelemetryPermissions.NETWORK_STATE
            )
        }
    }

    /**
     * Request device state permissions
     */
    fun requestDeviceStatePermissions() {
        val missingPermissions = PermissionUtils.getMissingDeviceStatePermissions(activity)

        if (missingPermissions.isEmpty()) {
            callback?.onDeviceStatePermissionChanged(
                PermissionState.Granted,
                TelemetryPermissions.READ_PHONE_STATE
            )
            return
        }

        showPermissionRationale(missingPermissions, "Device State Telemetry")
    }

    /**
     * Show rationale dialog explaining why the permissions are needed
     */
    private fun showPermissionRationale(permissions: List<String>, featureName: String) {
        // Check if we should show rationale for any permission
        val shouldShowRationale = permissions.any { permission ->
            TelemetryPermissions.isRuntimePermission(permission) &&
            PermissionUtils.shouldShowRationale(activity, permission)
        }

        if (shouldShowRationale) {
            val permissionDescriptions = permissions.map { permission ->
                "• ${PermissionUtils.getPermissionCategory(permission)}: ${PermissionUtils.getPermissionDescription(permission)}"
            }.joinToString("\n\n")

            AlertDialog.Builder(activity)
                .setTitle("$featureName Permissions Required")
                .setMessage(
                    "The Telemetry SDK needs the following permissions for $featureName functionality:\n\n" +
                    permissionDescriptions + "\n\n" +
                    "Would you like to grant these permissions now?"
                )
                .setPositiveButton("Grant") { _, _ ->
                    requestPermissions(permissions)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    handlePermissionDenied(permissions)
                }
                .setCancelable(false)
                .show()
        } else {
            requestPermissions(permissions)
        }
    }

    /**
     * Request the specified permissions using direct ActivityCompat.requestPermissions
     */
    private fun requestPermissions(permissions: List<String>) {
        // Only request runtime permissions
        val runtimePermissions = permissions.filter { TelemetryPermissions.isRuntimePermission(it) }

        if (runtimePermissions.isNotEmpty()) {
            // Use direct permission request instead of ActivityResultLauncher
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                runtimePermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // No runtime permissions to request, all are granted
            handlePermissionResults(permissions.associateWith { true })
        }
    }

    /**
     * Handle permission request results - this should be called from the activity's onRequestPermissionsResult
     */
    fun handlePermissionRequestResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val results = permissions.mapIndexed { index, permission ->
                permission to (grantResults.getOrNull(index) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            }.toMap()

            handlePermissionResults(results)
        }
    }

    /**
     * Handle permission request results
     */
    private fun handlePermissionResults(results: Map<String, Boolean>) {
        // Handle audio permissions
        val audioGranted = results[TelemetryPermissions.AUDIO_RECORDING] ?:
                          PermissionUtils.isAudioPermissionGranted(activity)
        if (results.containsKey(TelemetryPermissions.AUDIO_RECORDING)) {
            val audioState = getPermissionStateFromResult(TelemetryPermissions.AUDIO_RECORDING, audioGranted)
            callback?.onAudioPermissionStateChanged(audioState, TelemetryPermissions.AUDIO_RECORDING)
        }

        // Handle location permissions
        val locationPermissions = TelemetryPermissions.getLocationPermissions()
        if (locationPermissions.any { results.containsKey(it) }) {
            val locationGranted = locationPermissions.all { permission ->
                results[permission] ?: PermissionUtils.getPermissionState(activity, permission) == PermissionState.Granted
            }
            val locationState = if (locationGranted) PermissionState.Granted
                               else getPermissionStateFromResult(TelemetryPermissions.LOCATION_FINE, false)
            callback?.onLocationPermissionStateChanged(locationState, TelemetryPermissions.LOCATION_FINE)
        }

        // Handle connectivity permissions
        val connectivityPermissions = TelemetryPermissions.getConnectivityPermissions()
        if (connectivityPermissions.any { results.containsKey(it) }) {
            val bluetoothGranted = results[TelemetryPermissions.BLUETOOTH_CONNECT] ?:
                                  PermissionUtils.getPermissionState(activity, TelemetryPermissions.BLUETOOTH_CONNECT) == PermissionState.Granted
            val connectivityState = getPermissionStateFromResult(TelemetryPermissions.BLUETOOTH_CONNECT, bluetoothGranted)
            callback?.onConnectivityPermissionStateChanged(connectivityState, TelemetryPermissions.BLUETOOTH_CONNECT)
        }

        // Handle device state permissions
        val phoneStateGranted = results[TelemetryPermissions.READ_PHONE_STATE] ?:
                               PermissionUtils.getPermissionState(activity, TelemetryPermissions.READ_PHONE_STATE) == PermissionState.Granted
        if (results.containsKey(TelemetryPermissions.READ_PHONE_STATE)) {
            val phoneState = getPermissionStateFromResult(TelemetryPermissions.READ_PHONE_STATE, phoneStateGranted)
            callback?.onDeviceStatePermissionChanged(phoneState, TelemetryPermissions.READ_PHONE_STATE)
        }

        // Check for permanently denied permissions
        val permanentlyDenied = results.filterValues { !it }.keys.filter { permission ->
            !PermissionUtils.shouldShowRationale(activity, permission)
        }

        if (permanentlyDenied.isNotEmpty()) {
            showPermanentlyDeniedDialog(permanentlyDenied.toList())
        }
    }

    /**
     * Get permission state from request result
     */
    private fun getPermissionStateFromResult(permission: String, granted: Boolean): PermissionState {
        return when {
            granted -> PermissionState.Granted
            PermissionUtils.shouldShowRationale(activity, permission) -> PermissionState.Denied
            else -> PermissionState.PermanentlyDenied(permission)
        }
    }

    /**
     * Handle permission denied scenario
     */
    private fun handlePermissionDenied(permissions: List<String>) {
        permissions.forEach { permission ->
            when (PermissionUtils.getPermissionCategory(permission)) {
                "Audio Telemetry" -> callback?.onAudioPermissionStateChanged(
                    PermissionState.Denied, permission
                )
                "Location Telemetry" -> callback?.onLocationPermissionStateChanged(
                    PermissionState.Denied, permission
                )
                "Connectivity Telemetry" -> callback?.onConnectivityPermissionStateChanged(
                    PermissionState.Denied, permission
                )
                "Device State Telemetry" -> callback?.onDeviceStatePermissionChanged(
                    PermissionState.Denied, permission
                )
            }
        }
    }

    /**
     * Notify that all permissions are granted
     */
    private fun notifyAllPermissionsGranted() {
        callback?.onAudioPermissionStateChanged(PermissionState.Granted, TelemetryPermissions.AUDIO_RECORDING)
        callback?.onLocationPermissionStateChanged(PermissionState.Granted, TelemetryPermissions.LOCATION_FINE)
        callback?.onConnectivityPermissionStateChanged(PermissionState.Granted, TelemetryPermissions.BLUETOOTH_CONNECT)
        callback?.onDeviceStatePermissionChanged(PermissionState.Granted, TelemetryPermissions.READ_PHONE_STATE)
    }

    /**
     * Show dialog when permissions are permanently denied
     */
    private fun showPermanentlyDeniedDialog(permissions: List<String>) {
        val permissionNames = permissions.map { PermissionUtils.getPermissionCategory(it) }.distinct()

        AlertDialog.Builder(activity)
            .setTitle("Permissions Permanently Denied")
            .setMessage(
                "The following permissions have been permanently denied:\n" +
                permissionNames.joinToString(", ") + "\n\n" +
                "To use these telemetry features, please enable the permissions in your device settings:\n\n" +
                "Settings > Apps > ${activity.packageManager.getApplicationLabel(activity.applicationInfo)} > Permissions"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", activity.packageName, null)
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean {
        return PermissionUtils.areAllRuntimePermissionsGranted(activity)
    }

    /**
     * Check if specific category permissions are granted
     */
    fun areAudioPermissionsGranted(): Boolean = PermissionUtils.areAudioPermissionsGranted(activity)
    fun areLocationPermissionsGranted(): Boolean = PermissionUtils.areLocationPermissionsGranted(activity)
    fun areConnectivityPermissionsGranted(): Boolean = PermissionUtils.areConnectivityPermissionsGranted(activity)
    fun areDeviceStatePermissionsGranted(): Boolean = PermissionUtils.areDeviceStatePermissionsGranted(activity)

    /**
     * Get current permission state for all categories
     */
    fun getAllPermissionStates(): Map<String, Map<String, PermissionState>> {
        return PermissionUtils.getPermissionsByCategory(activity)
    }

    /**
     * Get summary of missing permissions by category
     */
    fun getMissingPermissionsSummary(): Map<String, List<String>> {
        return mapOf(
            "Audio Telemetry" to PermissionUtils.getMissingAudioPermissions(activity),
            "Location Telemetry" to PermissionUtils.getMissingLocationPermissions(activity),
            "Connectivity Telemetry" to PermissionUtils.getMissingConnectivityPermissions(activity),
            "Device State Telemetry" to PermissionUtils.getMissingDeviceStatePermissions(activity)
        ).filterValues { it.isNotEmpty() }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}

/**
 * Extension function to make permission requests easier
 */
fun FragmentActivity.createTelemetryPermissionHelper(
    callback: PermissionStateCallback? = null
): TelemetryPermissionHelper {
    return TelemetryPermissionHelper(this, callback)
}
