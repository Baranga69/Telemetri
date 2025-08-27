package com.commerin.telemetri.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.commerin.telemetri.data.local.entities.TelemetryEventEntity
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.local.dao.DrivingEventDao
import com.commerin.telemetri.data.local.dao.TripSummaryDao
import com.commerin.telemetri.data.local.converters.DatabaseConverters
import com.commerin.telemetri.data.local.entities.converters.MotionDataConverter
import com.commerin.telemetri.data.local.entities.converters.LocationDataConverter
import com.commerin.telemetri.data.local.entities.converters.SensorDataListConverter

@Database(
    entities = [
        TelemetryEventEntity::class,
        DrivingEventEntity::class,
        TripSummaryEntity::class
    ],
    version = 2, // Increment version to trigger migration
    exportSchema = false
)
@TypeConverters(
    DatabaseConverters::class,
    LocationDataConverter::class,
    MotionDataConverter::class,
    SensorDataListConverter::class
)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryEventDao(): TelemetryEventDao
    abstract fun drivingEventDao(): DrivingEventDao
    abstract fun tripSummaryDao(): TripSummaryDao
}
