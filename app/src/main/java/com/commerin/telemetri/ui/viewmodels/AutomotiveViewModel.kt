package com.commerin.telemetri.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.TelemetriManager
import com.commerin.telemetri.core.TelemetryConfig
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomotiveViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _telemetryData = MutableLiveData<ComprehensiveTelemetryEvent>()
    val telemetryData: LiveData<ComprehensiveTelemetryEvent> = _telemetryData

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _sensorData = MutableLiveData<List<SensorData>>()
    val sensorData: LiveData<List<SensorData>> = _sensorData

    private val _audioData = MutableLiveData<AudioTelemetryData>()
    val audioData: LiveData<AudioTelemetryData> = _audioData

    private val _networkData = MutableLiveData<NetworkTelemetryData>()
    val networkData: LiveData<NetworkTelemetryData> = _networkData

    private val _performanceData = MutableLiveData<PerformanceTelemetryData>()
    val performanceData: LiveData<PerformanceTelemetryData> = _performanceData

    init {
        observeTelemetryData()
    }

    fun startAutomotiveCollection() {
        viewModelScope.launch {
            val automotiveConfig = TelemetriManager.ConfigPresets.automotiveUseCase()
            telemetriManager.startTelemetryCollection(automotiveConfig)
            _isCollecting.value = true
        }
    }

    fun stopCollection() {
        viewModelScope.launch {
            telemetriManager.stopTelemetryCollection()
            _isCollecting.value = false
        }
    }

    private fun observeTelemetryData() {
        telemetriManager.comprehensiveTelemetry.observeForever { telemetry ->
            _telemetryData.value = telemetry
        }

        telemetriManager.locationData.observeForever { location ->
            _locationData.value = location
        }

        telemetriManager.sensorData.observeForever { sensors ->
            _sensorData.value = sensors
        }

        telemetriManager.audioTelemetry.observeForever { audio ->
            _audioData.value = audio
        }

        telemetriManager.networkTelemetry.observeForever { network ->
            _networkData.value = network
        }

        telemetriManager.performanceTelemetry.observeForever { performance ->
            _performanceData.value = performance
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
