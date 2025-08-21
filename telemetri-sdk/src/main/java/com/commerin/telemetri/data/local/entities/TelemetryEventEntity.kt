package com.commerin.telemetri.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.commerin.telemetri.domain.model.LocationData
import com.commerin.telemetri.domain.model.MotionData
import com.commerin.telemetri.domain.model.SensorData

@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey val eventId: String,
    val location: LocationData?,
    val motion: MotionData?,
    val rawSensors: List<SensorData>,
    val timestamp: Long,
    val synced: Boolean = false
)
