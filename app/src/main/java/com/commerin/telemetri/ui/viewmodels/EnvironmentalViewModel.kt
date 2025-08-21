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

@HiltViewModel
class EnvironmentalViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _audioData = MutableLiveData<AudioTelemetryData>()
    val audioData: LiveData<AudioTelemetryData> = _audioData

    private val _environmentalData = MutableLiveData<EnvironmentalData>()
    val environmentalData: LiveData<EnvironmentalData> = _environmentalData

    private val _sensorData = MutableLiveData<List<SensorData>>()
    val sensorData: LiveData<List<SensorData>> = _sensorData

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _networkData = MutableLiveData<NetworkTelemetryData>()
    val networkData: LiveData<NetworkTelemetryData> = _networkData

    init {
        observeTelemetryData()
    }

    fun startEnvironmentalCollection() {
        viewModelScope.launch {
            val environmentalConfig = TelemetriManager.ConfigPresets.environmentalMonitoringUseCase()
            telemetriManager.startTelemetryCollection(environmentalConfig)
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
        telemetriManager.audioTelemetry.observeForever { audio ->
            _audioData.value = audio
        }

        telemetriManager.comprehensiveTelemetry.observeForever { comprehensive ->
            comprehensive.environmental?.let { environmental ->
                _environmentalData.value = environmental
            }
        }

        telemetriManager.sensorData.observeForever { sensors ->
            _sensorData.value = sensors
        }

        telemetriManager.locationData.observeForever { location ->
            _locationData.value = location
        }

        telemetriManager.networkTelemetry.observeForever { network ->
            _networkData.value = network
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
