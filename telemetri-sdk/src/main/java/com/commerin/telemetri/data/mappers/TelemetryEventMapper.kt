package com.commerin.telemetri.data.mappers

import com.commerin.telemetri.data.local.entities.TelemetryEventEntity
import com.commerin.telemetri.domain.model.TelemetryEvent

object TelemetryEventMapper {
    fun toEntity(model: TelemetryEvent, synced: Boolean = false): TelemetryEventEntity {
        return TelemetryEventEntity(
            eventId = model.eventId,
            location = model.location,
            motion = model.motion,
            rawSensors = model.rawSensors,
            timestamp = model.timestamp,
            synced = synced
        )
    }

    fun toModel(entity: TelemetryEventEntity): TelemetryEvent {
        return TelemetryEvent(
            eventId = entity.eventId,
            location = entity.location,
            motion = entity.motion,
            rawSensors = entity.rawSensors,
            timestamp = entity.timestamp
        )
    }
}

