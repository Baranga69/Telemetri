package com.commerin.telemetri.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.SpeedTestResult
import com.commerin.telemetri.core.TelemetriManager
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _networkData = MutableLiveData<NetworkTelemetryData>()
    val networkData: LiveData<NetworkTelemetryData> = _networkData

    private val _speedTestResult = MutableLiveData<SpeedTestResult>()
    val speedTestResult: LiveData<SpeedTestResult> = _speedTestResult

    private val _deviceStateData = MutableLiveData<DeviceStateData>()
    val deviceStateData: LiveData<DeviceStateData> = _deviceStateData

    init {
        observeTelemetryData()
    }

    /**
     * Start network diagnostics collection using optimized network-only configuration
     */
    fun startNetworkDiagnostics() {
        viewModelScope.launch {
            val networkConfig = TelemetriManager.ConfigPresets.networkDiagnosticsUseCase()
            telemetriManager.startTelemetryCollection(networkConfig)
            _isCollecting.value = true
        }
    }

    /**
     * Stop network diagnostics collection
     */
    fun stopCollection() {
        viewModelScope.launch {
            telemetriManager.stopTelemetryCollection()
            _isCollecting.value = false
        }
    }

    private fun observeTelemetryData() {
        // Only observe data streams that are enabled in network diagnostics config

        telemetriManager.locationData.observeForever { location ->
            _locationData.value = location
        }

        telemetriManager.networkTelemetry.observeForever { network ->
            _networkData.value = network
        }

        telemetriManager.speedTestResult.observeForever { speedTest ->
            _speedTestResult.value = speedTest
        }

        // Basic device state for context
        telemetriManager.comprehensiveTelemetry.observeForever { telemetry ->
            telemetry.deviceState?.let { deviceState ->
                _deviceStateData.value = deviceState
            }
        }
    }

    // Network speed test methods
    fun startNetworkSpeedTest() {
        viewModelScope.launch {
            telemetriManager.startNetworkSpeedTest()
        }
    }

    fun stopNetworkSpeedTest() {
        viewModelScope.launch {
            telemetriManager.stopNetworkSpeedTest()
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
