package com.commerin.telemetri.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Utility class for managing battery optimization settings to ensure reliable background operation
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    /**
     * Check if the app is whitelisted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // No battery optimization on older versions
        }
    }

    /**
     * Request to ignore battery optimizations for reliable background operation
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(activity)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, requestCode)
                    Log.d(TAG, "Requested battery optimization whitelist")
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting battery optimization whitelist", e)
                    // Fallback to general battery optimization settings
                    openBatteryOptimizationSettings(activity)
                }
            }
        }
    }

    /**
     * Open battery optimization settings page
     */
    fun openBatteryOptimizationSettings(activity: Activity) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            activity.startActivity(intent)
            Log.d(TAG, "Opened battery optimization settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening battery optimization settings", e)
        }
    }

    /**
     * Show explanation dialog about why battery optimization should be disabled
     */
    fun shouldShowBatteryOptimizationRationale(context: Context): Boolean {
        return !isIgnoringBatteryOptimizations(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Get battery optimization status message for UI
     */
    fun getBatteryOptimizationStatus(context: Context): String {
        return if (isIgnoringBatteryOptimizations(context)) {
            "Battery optimization disabled - background operation enabled"
        } else {
            "Battery optimization enabled - may affect background data collection"
        }
    }
}
