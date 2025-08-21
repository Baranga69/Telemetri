package com.commerin.telemetri.data.location

import com.commerin.telemetri.domain.model.LocationData

object LocationMapper {
    fun map(raw: LocationRaw): LocationData {
        return LocationData(
            latitude = raw.latitude,
            longitude = raw.longitude,
            altitude = raw.altitude,
            speed = raw.speed,
            accuracy = raw.accuracy,
            bearing = raw.bearing,
            provider = raw.provider ?: "unknown",
            timestamp = raw.timestamp
        )
    }
}