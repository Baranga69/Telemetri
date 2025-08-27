package com.commerin.telemetri.di

import android.content.Context
import com.commerin.telemetri.data.database.*
import com.commerin.telemetri.data.repository.ReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReportDatabase(@ApplicationContext context: Context): ReportDatabase {
        return ReportDatabase.getDatabase(context)
    }

    @Provides
    fun provideEventReportDao(database: ReportDatabase): EventReportDao {
        return database.eventReportDao()
    }

    @Provides
    fun provideInsuranceReportDao(database: ReportDatabase): InsuranceReportDao {
        return database.insuranceReportDao()
    }

    @Provides
    fun provideEventSummaryDao(database: ReportDatabase): EventSummaryDao {
        return database.eventSummaryDao()
    }

    @Provides
    @Singleton
    fun provideReportRepository(
        eventReportDao: EventReportDao,
        insuranceReportDao: InsuranceReportDao,
        eventSummaryDao: EventSummaryDao
    ): ReportRepository {
        return ReportRepository(eventReportDao, insuranceReportDao, eventSummaryDao)
    }
}
