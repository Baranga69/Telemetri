package com.commerin.telemetri.data.location

import com.commerin.telemetri.domain.model.LocationData

object LocationMapper {
    fun map(raw: LocationRaw): LocationData {
        return LocationData(
            latitude = raw.latitude,
            longitude = raw.longitude,
            speed = raw.speed, // Convert non-null Float to nullable Float
            timestamp = raw.timestamp,
            accuracy = null, // Not available in LocationRaw
            bearing = null   // Not available in LocationRaw
        )
    }
}