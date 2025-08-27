package com.commerin.telemetri.core.di

import android.content.Context
import androidx.room.Room
import com.commerin.telemetri.core.TelemetryApi
import com.commerin.telemetri.core.EnhancedTelematicsManager
import com.commerin.telemetri.core.DrivingEventDetectionEngine
import com.commerin.telemetri.core.DriverDetectionEngine
import com.commerin.telemetri.core.AdaptivePowerManager
import com.commerin.telemetri.core.PerformanceTelemetryService
import com.commerin.telemetri.core.NetworkTelemetryService
import com.commerin.telemetri.core.AudioTelemetryService
import com.commerin.telemetri.data.local.TelemetryDatabase
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.local.dao.DrivingEventDao
import com.commerin.telemetri.data.local.dao.TripSummaryDao
import com.commerin.telemetri.data.repository.TelemetryEventRepository
import com.commerin.telemetri.data.repository.EnhancedTelemetryRepository
import com.commerin.telemetri.domain.model.TelemetryEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TelemetryDatabase =
        Room.databaseBuilder(
            context,
            TelemetryDatabase::class.java,
            "telemetry_db"
        )
        .fallbackToDestructiveMigration() // Allow database recreation for version 2
        .build()

    @Provides
    @Singleton
    fun provideEnhancedTelematicsManager(@ApplicationContext context: Context): EnhancedTelematicsManager =
        EnhancedTelematicsManager.getInstance(context)

    @Provides
    @Singleton
    fun provideDrivingEventDetectionEngine(@ApplicationContext context: Context): DrivingEventDetectionEngine =
        DrivingEventDetectionEngine(context)

    @Provides
    @Singleton
    fun provideDriverDetectionEngine(@ApplicationContext context: Context): DriverDetectionEngine =
        DriverDetectionEngine(context)

    @Provides
    @Singleton
    fun provideAdaptivePowerManager(@ApplicationContext context: Context): AdaptivePowerManager =
        AdaptivePowerManager(context)

    @Provides
    @Singleton
    fun providePerformanceTelemetryService(@ApplicationContext context: Context): PerformanceTelemetryService =
        PerformanceTelemetryService(context)

    @Provides
    @Singleton
    fun provideNetworkTelemetryService(@ApplicationContext context: Context): NetworkTelemetryService =
        NetworkTelemetryService(context)

    @Provides
    @Singleton
    fun provideAudioTelemetryService(@ApplicationContext context: Context): AudioTelemetryService =
        AudioTelemetryService(context)

    @Provides
    fun provideTelemetryEventDao(db: TelemetryDatabase): TelemetryEventDao = db.telemetryEventDao()

    @Provides
    fun provideDrivingEventDao(db: TelemetryDatabase): DrivingEventDao = db.drivingEventDao()

    @Provides
    fun provideTripSummaryDao(db: TelemetryDatabase): TripSummaryDao = db.tripSummaryDao()

    @Provides
    @Singleton
    fun provideTelemetryEventRepository(dao: TelemetryEventDao): TelemetryEventRepository =
        TelemetryEventRepository(dao)

    @Provides
    @Singleton
    fun provideEnhancedTelemetryRepository(
        drivingEventDao: DrivingEventDao,
        tripSummaryDao: TripSummaryDao,
        telemetryEventDao: TelemetryEventDao
    ): EnhancedTelemetryRepository =
        EnhancedTelemetryRepository(drivingEventDao, tripSummaryDao, telemetryEventDao)

    @Provides
    @Singleton
    fun provideTelemetryApi(): TelemetryApi = object : TelemetryApi {
        override suspend fun uploadEvents(events: List<TelemetryEvent>): List<String> {
            // TODO: Implement real network upload
            return events.map { it.eventId }
        }
        override fun isNetworkAvailable(): Boolean = true // TODO: Implement real network check
    }
}
