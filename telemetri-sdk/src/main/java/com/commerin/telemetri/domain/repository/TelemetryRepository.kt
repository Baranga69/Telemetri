package com.commerin.telemetri.domain.repository

import com.commerin.telemetri.domain.model.LocationData
import com.commerin.telemetri.domain.model.SensorData
import com.commerin.telemetri.domain.model.TelemetryEvent
import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    fun getLocationUpdates(): Flow<LocationData>
    fun getSensorUpdates(): Flow<SensorData>
    fun getAccelerometerUpdates(): Flow<SensorData>
    suspend fun saveTelemetryData(event: TelemetryEvent)
}
