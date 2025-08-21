package com.commerin.telemetri.core.di

import android.content.Context
import androidx.room.Room
import com.commerin.telemetri.core.TelemetryApi
import com.commerin.telemetri.data.local.TelemetryDatabase
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.repository.TelemetryEventRepository
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
        ).build()

    @Provides
    fun provideTelemetryEventDao(db: TelemetryDatabase): TelemetryEventDao = db.telemetryEventDao()

    @Provides
    @Singleton
    fun provideTelemetryEventRepository(dao: TelemetryEventDao): TelemetryEventRepository =
        TelemetryEventRepository(dao)

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

