package com.commerin.telemetri.domain.repository

import com.commerin.telemetri.data.location.LocationDataSource
import com.commerin.telemetri.data.location.LocationMapper
import com.commerin.telemetri.data.sensors.SensorDataSource
import com.commerin.telemetri.data.sensors.SensorMapper
import com.commerin.telemetri.data.sensors.SensorRaw
import com.commerin.telemetri.domain.model.LocationData
import com.commerin.telemetri.domain.model.SensorData
import com.commerin.telemetri.domain.model.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TelemetryRepositoryImpl(
    private val locationDataSource: LocationDataSource,
    private val sensorDataSource: SensorDataSource
) : TelemetryRepository {

    override fun getLocationUpdates(): Flow<LocationData> {
        return locationDataSource.getLocationUpdates().map { LocationMapper.map(it) }
    }

    override fun getSensorUpdates(): Flow<SensorData> {
        return sensorDataSource.getAccelerometerData().map { sensorRaw: SensorRaw -> SensorMapper.map(sensorRaw) }
    }

    override suspend fun saveTelemetryData(event: TelemetryEvent) {
        // Implementation for saving telemetry data
        TODO("Implement saving telemetry data")
    }

    override fun getAccelerometerUpdates(): Flow<SensorData> {
        return sensorDataSource.getAccelerometerData().map { sensorRaw: SensorRaw -> SensorMapper.map(sensorRaw) }
    }
}