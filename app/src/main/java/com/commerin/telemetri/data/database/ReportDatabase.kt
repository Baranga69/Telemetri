package com.commerin.telemetri.data.database

import androidx.room.Database
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EventReportEntity::class,
        InsuranceReportEntity::class,
        EventSummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun eventReportDao(): EventReportDao
    abstract fun insuranceReportDao(): InsuranceReportDao
    abstract fun eventSummaryDao(): EventSummaryDao

    companion object {
        @Volatile
        private var INSTANCE: ReportDatabase? = null

        fun getDatabase(context: Context): ReportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReportDatabase::class.java,
                    "report_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
