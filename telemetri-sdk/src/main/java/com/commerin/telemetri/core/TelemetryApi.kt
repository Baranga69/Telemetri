package com.commerin.telemetri.core

import com.commerin.telemetri.domain.model.TelemetryEvent

/**
 * Interface for backend API communication.
 *
 * Implement this to provide network upload and connectivity checks for telemetry events.
 */
interface TelemetryApi {
    suspend fun uploadEvents(events: List<TelemetryEvent>): List<String> // returns list of successfully uploaded event IDs
    fun isNetworkAvailable(): Boolean
}
