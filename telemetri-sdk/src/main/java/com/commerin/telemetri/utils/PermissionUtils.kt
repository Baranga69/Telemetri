package com.commerin.telemetri.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.commerin.telemetri.domain.model.PermissionState
import com.commerin.telemetri.domain.model.TelemetryPermissions

/**
 * Utility class for managing telemetry-related permissions
 */
class PermissionUtils {

    companion object {

        /**
         * Check if audio recording permission is granted
         */
        fun isAudioPermissionGranted(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                TelemetryPermissions.AUDIO_RECORDING
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Check if location permissions are granted
         */
        fun areLocationPermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getLocationPermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if connectivity permissions are granted
         */
        fun areConnectivityPermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getConnectivityPermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if device state permissions are granted
         */
        fun areDeviceStatePermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getDeviceStatePermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if all telemetry permissions are granted
         */
        fun areAllTelemetryPermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getAllRequiredPermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if all runtime permissions are granted
         */
        fun areAllRuntimePermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getRuntimePermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get the current state of audio recording permission
         */
        fun getAudioPermissionState(context: Context): PermissionState {
            return getPermissionState(context, TelemetryPermissions.AUDIO_RECORDING)
        }

        /**
         * Get the current state of a specific permission
         */
        fun getPermissionState(context: Context, permission: String): PermissionState {
            return when {
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ->
                    PermissionState.Granted
                shouldShowRationale(context, permission) ->
                    PermissionState.Denied
                else ->
                    PermissionState.NotRequested
            }
        }

        /**
         * Check if permission rationale should be shown
         */
        fun shouldShowRationale(context: Context, permission: String): Boolean {
            return try {
                // This requires an Activity context, so we'll catch the exception
                // and return false if we can't determine the rationale state
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context as androidx.fragment.app.FragmentActivity,
                    permission
                )
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Get human-readable description of what the permission is used for
         */
        fun getPermissionDescription(permission: String): String {
            return when (permission) {
                TelemetryPermissions.AUDIO_RECORDING ->
                    "Audio recording permission is required to analyze environmental sounds, " +
                    "detect voice patterns, and measure noise levels for telemetry data."

                TelemetryPermissions.LOCATION_FINE ->
                    "Precise location permission is required for accurate GPS tracking, " +
                    "location-based telemetry, and driving behavior analysis."

                TelemetryPermissions.LOCATION_COARSE ->
                    "Approximate location permission is required for general location tracking " +
                    "and region-based telemetry analysis."

                TelemetryPermissions.WIFI_STATE ->
                    "WiFi state access is required to monitor network connectivity, " +
                    "analyze connection quality, and optimize data transmission."

                TelemetryPermissions.BLUETOOTH_CONNECT ->
                    "Bluetooth permission is required to connect with vehicle systems, " +
                    "OBD-II devices, and other automotive sensors for enhanced telemetry."

                TelemetryPermissions.NETWORK_STATE ->
                    "Network state access is required to monitor cellular and WiFi connectivity, " +
                    "optimize data usage, and ensure reliable telemetry transmission."

                TelemetryPermissions.READ_PHONE_STATE ->
                    "Phone state permission is required to detect calls, monitor signal strength, " +
                    "and analyze device connectivity for comprehensive telemetry data."

                else -> "This permission is required for telemetry functionality."
            }
        }

        /**
         * Get permission category name
         */
        fun getPermissionCategory(permission: String): String {
            return when (permission) {
                TelemetryPermissions.AUDIO_RECORDING -> "Audio Telemetry"
                TelemetryPermissions.LOCATION_FINE, TelemetryPermissions.LOCATION_COARSE -> "Location Telemetry"
                TelemetryPermissions.WIFI_STATE, TelemetryPermissions.BLUETOOTH_CONNECT, TelemetryPermissions.NETWORK_STATE -> "Connectivity Telemetry"
                TelemetryPermissions.READ_PHONE_STATE -> "Device State Telemetry"
                else -> "General Telemetry"
            }
        }

        /**
         * Check if all required permissions for audio telemetry are granted
         */
        fun areAudioPermissionsGranted(context: Context): Boolean {
            return TelemetryPermissions.getAudioPermissions().all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get missing permissions for audio telemetry
         */
        fun getMissingAudioPermissions(context: Context): List<String> {
            return TelemetryPermissions.getAudioPermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get missing location permissions
         */
        fun getMissingLocationPermissions(context: Context): List<String> {
            return TelemetryPermissions.getLocationPermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get missing connectivity permissions
         */
        fun getMissingConnectivityPermissions(context: Context): List<String> {
            return TelemetryPermissions.getConnectivityPermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get missing device state permissions
         */
        fun getMissingDeviceStatePermissions(context: Context): List<String> {
            return TelemetryPermissions.getDeviceStatePermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get all missing runtime permissions
         */
        fun getMissingRuntimePermissions(context: Context): List<String> {
            return TelemetryPermissions.getRuntimePermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get all missing permissions for telemetry
         */
        fun getAllMissingPermissions(context: Context): List<String> {
            return TelemetryPermissions.getAllRequiredPermissions().filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get permissions grouped by category with their states
         */
        fun getPermissionsByCategory(context: Context): Map<String, Map<String, PermissionState>> {
            return mapOf(
                "Audio Telemetry" to TelemetryPermissions.getAudioPermissions().associateWith {
                    getPermissionState(context, it)
                },
                "Location Telemetry" to TelemetryPermissions.getLocationPermissions().associateWith {
                    getPermissionState(context, it)
                },
                "Connectivity Telemetry" to TelemetryPermissions.getConnectivityPermissions().associateWith {
                    getPermissionState(context, it)
                },
                "Device State Telemetry" to TelemetryPermissions.getDeviceStatePermissions().associateWith {
                    getPermissionState(context, it)
                }
            )
        }
    }
}
