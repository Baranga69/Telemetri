package com.commerin.telemetri.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.TelemetriManager
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PowerSavingStats(
    val powerSavedPercent: Int,
    val activeSensorsCount: Int,
    val averageSampleRate: Int,
    val efficiencyScore: String
)

@HiltViewModel
class BatterySaverViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _batteryInfo = MutableLiveData<BatteryTelemetryInfo>()
    val batteryInfo: LiveData<BatteryTelemetryInfo> = _batteryInfo

    private val _powerSavingStats = MutableLiveData<PowerSavingStats>()
    val powerSavingStats: LiveData<PowerSavingStats> = _powerSavingStats

    private val _sensorData = MutableLiveData<List<SensorData>>()
    val sensorData: LiveData<List<SensorData>> = _sensorData

    private val _deviceState = MutableLiveData<DeviceStateData>()
    val deviceState: LiveData<DeviceStateData> = _deviceState

    private var initialBatteryLevel = 0

    init {
        observeTelemetryData()
    }

    fun startBatterySaverCollection() {
        viewModelScope.launch {
            val batterySaverConfig = TelemetriManager.ConfigPresets.batterySaverUseCase()
            telemetriManager.startTelemetryCollection(batterySaverConfig)
            _isCollecting.value = true

            // Record initial battery level for power saving calculations
            _batteryInfo.value?.let {
                initialBatteryLevel = it.level
            }

            updatePowerSavingStats()
        }
    }

    fun stopCollection() {
        viewModelScope.launch {
            telemetriManager.stopTelemetryCollection()
            _isCollecting.value = false
        }
    }

    private fun observeTelemetryData() {
        telemetriManager.performanceTelemetry.observeForever { performance ->
            _batteryInfo.value = performance.batteryInfo
            updatePowerSavingStats()
        }

        telemetriManager.sensorData.observeForever { sensors ->
            // Filter to only essential sensors for battery saver mode
            val essentialSensors = sensors.filter { sensor ->
                sensor.sensorType in listOf(
                    SensorType.ACCELEROMETER,
                    SensorType.GYROSCOPE,
                    SensorType.MAGNETOMETER
                )
            }
            _sensorData.value = essentialSensors
            updatePowerSavingStats()
        }

        telemetriManager.comprehensiveTelemetry.observeForever { comprehensive ->
            comprehensive.deviceState?.let { deviceState ->
                _deviceState.value = deviceState
            }
        }
    }

    private fun updatePowerSavingStats() {
        if (!_isCollecting.value!!) return

        val currentBattery = _batteryInfo.value
        val activeSensors = _sensorData.value?.size ?: 0

        // Calculate power savings compared to full telemetry mode
        val powerSavedPercent = calculatePowerSavings()

        // Calculate average sample rate (lower in battery saver mode)
        val averageSampleRate = 10 // Hz, much lower than normal mode

        // Calculate efficiency score
        val efficiencyScore = calculateEfficiencyScore(currentBattery, activeSensors)

        _powerSavingStats.value = PowerSavingStats(
            powerSavedPercent = powerSavedPercent,
            activeSensorsCount = activeSensors,
            averageSampleRate = averageSampleRate,
            efficiencyScore = efficiencyScore
        )
    }

    private fun calculatePowerSavings(): Int {
        // Estimate power savings compared to full telemetry mode
        // Battery saver mode typically saves 60-80% power
        return 75 // 75% power savings
    }

    private fun calculateEfficiencyScore(batteryInfo: BatteryTelemetryInfo?, activeSensors: Int): String {
        if (batteryInfo == null) return "Unknown"

        return when {
            batteryInfo.level > 80 && activeSensors <= 3 -> "Excellent"
            batteryInfo.level > 60 && activeSensors <= 4 -> "Good"
            batteryInfo.level > 40 && activeSensors <= 5 -> "Fair"
            batteryInfo.level > 20 -> "Poor"
            else -> "Critical"
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
