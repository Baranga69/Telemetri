package com.commerin.telemetri

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.commerin.telemetri.core.TelematriSdk
import com.commerin.telemetri.data.repository.TelemetryEventRepository
import com.commerin.telemetri.domain.model.TelemetryEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var telemetryRepository: TelemetryEventRepository

    private lateinit var telemetriSdk: TelematriSdk

    // UI Components
    private lateinit var btnStartTracking: Button
    private lateinit var btnStopTracking: Button
    private lateinit var btnViewData: Button
    private lateinit var btnClearData: Button
    private lateinit var tvTrackingStatus: TextView
    private lateinit var tvLocationData: TextView
    private lateinit var tvSensorData: TextView
    private lateinit var tvEventCount: TextView
    private lateinit var tvStoredEvents: TextView
    private lateinit var tvEventLog: TextView

    private var isTracking = false
    private var eventCount = 0
    private val eventLog = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            startTelemetryTracking()
        } else {
            Toast.makeText(this, "Location permission required for telemetry tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeTelemetriSdk()
        setupClickListeners()
        updateStoredEventsCount()

        // Request permissions on app startup
        requestPermissionsOnStartup()
    }

    private fun initializeViews() {
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnStopTracking = findViewById(R.id.btnStopTracking)
        btnViewData = findViewById(R.id.btnViewData)
        btnClearData = findViewById(R.id.btnClearData)
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus)
        tvLocationData = findViewById(R.id.tvLocationData)
        tvSensorData = findViewById(R.id.tvSensorData)
        tvEventCount = findViewById(R.id.tvEventCount)
        tvStoredEvents = findViewById(R.id.tvStoredEvents)
        tvEventLog = findViewById(R.id.tvEventLog)
    }

    private fun initializeTelemetriSdk() {
        telemetriSdk = TelematriSdk(this)

        // Observe telemetry data
        telemetriSdk.observeTelemetry().observe(this) { telemetryEvent ->
            updateTelemetryDisplay(telemetryEvent)
            logEvent("New telemetry event: ${telemetryEvent.eventId}")
            eventCount++
            tvEventCount.text = "Events Collected: $eventCount"
        }
    }

    private fun setupClickListeners() {
        btnStartTracking.setOnClickListener {
            if (hasLocationPermissions()) {
                startTelemetryTracking()
            } else {
                requestLocationPermissions()
            }
        }

        btnStopTracking.setOnClickListener {
            stopTelemetryTracking()
        }

        btnViewData.setOnClickListener {
            viewStoredData()
        }

        btnClearData.setOnClickListener {
            clearAllData()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    @Suppress("MissingPermission")
    private fun startTelemetryTracking() {
        // Permission check is done before calling this method
        if (!hasLocationPermissions()) {
            logEvent("❌ Location permissions not granted")
            Toast.makeText(this, "Location permissions required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            telemetriSdk.startTracking()
            isTracking = true
            updateTrackingUI()
            logEvent("✅ Telemetry tracking started")
            Toast.makeText(this, "Telemetry tracking started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            logEvent("❌ Failed to start tracking: ${e.message}")
            Toast.makeText(this, "Failed to start tracking: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopTelemetryTracking() {
        telemetriSdk.stopTracking()
        isTracking = false
        updateTrackingUI()
        logEvent("⏹️ Telemetry tracking stopped")
        Toast.makeText(this, "Telemetry tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateTrackingUI() {
        if (isTracking) {
            btnStartTracking.isEnabled = false
            btnStopTracking.isEnabled = true
            tvTrackingStatus.text = "Status: Running 🟢"
        } else {
            btnStartTracking.isEnabled = true
            btnStopTracking.isEnabled = false
            tvTrackingStatus.text = "Status: Stopped 🔴"
            tvLocationData.text = "Location: No data"
            tvSensorData.text = "Sensors: No data"
        }
    }

    private fun updateTelemetryDisplay(event: TelemetryEvent) {
        // Update location display
        event.location?.let { location ->
            tvLocationData.text = "Location: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}\n" +
                    "Accuracy: ${location.accuracy?.let { String.format("%.1f", it) } ?: "N/A"}m, Speed: ${location.speed?.let { String.format("%.1f", it) } ?: "N/A"}m/s"
        }

        // Update sensor display
        if (event.rawSensors.isNotEmpty()) {
            val sensor = event.rawSensors.first()
            tvSensorData.text = "Sensor: ${getSensorTypeName(sensor.sensorType)}\n" +
                    "Values: X=${String.format("%.2f", sensor.x)}, Y=${String.format("%.2f", sensor.y)}, Z=${String.format("%.2f", sensor.z)}"
        }
    }

    private fun getSensorTypeName(sensorType: com.commerin.telemetri.domain.model.SensorType): String {
        return when (sensorType) {
            com.commerin.telemetri.domain.model.SensorType.ACCELEROMETER -> "Accelerometer"
            com.commerin.telemetri.domain.model.SensorType.GYROSCOPE -> "Gyroscope"
            com.commerin.telemetri.domain.model.SensorType.MAGNETOMETER -> "Magnetometer"
            else -> "Unknown"
        }
    }

    private fun viewStoredData() {
        lifecycleScope.launch {
            try {
                val events = telemetryRepository.getAllEvents()
                val eventDetails = events.takeLast(5).joinToString("\n\n") { event ->
                    "ID: ${event.eventId}\n" +
                    "Location: ${event.location?.let { "${it.latitude}, ${it.longitude}" } ?: "N/A"}\n" +
                    "Sensors: ${event.rawSensors.size} readings\n" +
                    "Time: ${Date(event.timestamp)}"
                }

                if (eventDetails.isNotEmpty()) {
                    logEvent("📊 Showing last ${events.takeLast(5).size} events:")
                    logEvent(eventDetails)
                } else {
                    logEvent("📊 No stored events found")
                }
            } catch (e: Exception) {
                logEvent("❌ Error retrieving data: ${e.message}")
            }
        }
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                telemetryRepository.deleteAllEvents()
                eventCount = 0
                tvEventCount.text = "Events Collected: 0"
                updateStoredEventsCount()
                logEvent("🗑️ All stored data cleared")
                Toast.makeText(this@MainActivity, "All data cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                logEvent("❌ Error clearing data: ${e.message}")
            }
        }
    }

    private fun updateStoredEventsCount() {
        lifecycleScope.launch {
            try {
                val events = telemetryRepository.getAllEvents()
                tvStoredEvents.text = "Stored Events: ${events.size}"
            } catch (e: Exception) {
                tvStoredEvents.text = "Stored Events: Error"
            }
        }
    }

    private fun logEvent(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        eventLog.add(0, logEntry)

        // Keep only last 50 entries
        if (eventLog.size > 50) {
            eventLog.removeAt(eventLog.size - 1)
        }

        tvEventLog.text = eventLog.joinToString("\n")
    }

    private fun requestPermissionsOnStartup() {
        if (!hasLocationPermissions()) {
            logEvent("📍 Requesting location permissions on startup...")
            requestLocationPermissions()
        } else {
            logEvent("✅ Location permissions already granted")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            telemetriSdk.stopTracking()
        }
    }
}
