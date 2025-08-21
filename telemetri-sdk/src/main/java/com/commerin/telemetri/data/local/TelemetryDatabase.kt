package com.commerin.telemetri.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.commerin.telemetri.data.local.entities.TelemetryEventEntity
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.local.entities.converters.MotionDataConverter
import com.commerin.telemetri.data.local.entities.converters.LocationDataConverter
import com.commerin.telemetri.data.local.entities.converters.SensorDataListConverter

@Database(entities = [TelemetryEventEntity::class], version = 1, exportSchema = false)
@TypeConverters(LocationDataConverter::class, MotionDataConverter::class, SensorDataListConverter::class)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryEventDao(): TelemetryEventDao
}
