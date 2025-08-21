package com.commerin.telemetri.core

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.LocationData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService(private val context: Context) {
    companion object {
        private const val TAG = "LocationService"
        private const val HIGH_ACCURACY_INTERVAL = 2000L      // 2 seconds for high precision
        private const val BALANCED_INTERVAL = 5000L           // 5 seconds for balanced
        private const val LOW_POWER_INTERVAL = 30000L         // 30 seconds for low power
        private const val FASTEST_INTERVAL = 1000L            // Fastest possible updates
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private var currentLocationRequest: LocationRequest? = null
    private var isTracking = false

    // Enhanced location request with multiple priority modes
    private fun createLocationRequest(priority: Int = Priority.PRIORITY_HIGH_ACCURACY): LocationRequest {
        val interval = when (priority) {
            Priority.PRIORITY_HIGH_ACCURACY -> HIGH_ACCURACY_INTERVAL
            Priority.PRIORITY_BALANCED_POWER_ACCURACY -> BALANCED_INTERVAL
            Priority.PRIORITY_LOW_POWER -> LOW_POWER_INTERVAL
            else -> BALANCED_INTERVAL
        }

        return LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMaxUpdateDelayMillis(interval * 2)
            .setWaitForAccurateLocation(false)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                try {
                    val enhancedLocationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        provider = location.provider ?: "fused",
                        timestamp = location.time
                    )

                    _locationData.postValue(enhancedLocationData)
                    Log.v(TAG, "Location updated: ${location.latitude}, ${location.longitude}, altitude: ${location.altitude}m (accuracy: ${location.accuracy}m)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing location data", e)
                }
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start(priority: Int = Priority.PRIORITY_HIGH_ACCURACY) {
        if (isTracking) {
            Log.w(TAG, "Location tracking already started")
            return
        }

        if (!PermissionUtils.hasLocationPermission(context)) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        try {
            currentLocationRequest = createLocationRequest(priority)
            fusedClient.requestLocationUpdates(
                currentLocationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            Log.d(TAG, "Location tracking started with priority: $priority")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location tracking", e)
        }
    }

    fun stop() {
        if (!isTracking) {
            Log.w(TAG, "Location tracking not started")
            return
        }

        try {
            fusedClient.removeLocationUpdates(locationCallback)
            isTracking = false
            Log.d(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop location tracking", e)
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastKnownLocation(): LocationData? {
        return try {
            var lastLocation: Location? = null
            fusedClient.lastLocation.addOnSuccessListener { location ->
                lastLocation = location
            }

            lastLocation?.let { location ->
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    speed = if (location.hasSpeed()) location.speed else null,
                    accuracy = if (location.hasAccuracy()) location.accuracy else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    provider = location.provider ?: "fused",
                    timestamp = location.time
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last known location", e)
            null
        }
    }

    fun changePriority(newPriority: Int) {
        if (isTracking) {
            stop()
            start(newPriority)
        }
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getEnabledProviders(): List<String> {
        return locationManager.getProviders(true)
    }
}