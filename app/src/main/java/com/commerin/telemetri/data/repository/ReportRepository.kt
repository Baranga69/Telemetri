package com.commerin.telemetri.data.repository

import com.commerin.telemetri.data.database.*
import com.commerin.telemetri.domain.model.DrivingEvent
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val eventReportDao: EventReportDao,
    private val insuranceReportDao: InsuranceReportDao,
    private val eventSummaryDao: EventSummaryDao
) {

    // Event Reports
    suspend fun saveEventReport(
        title: String,
        content: String,
        driverStatus: String?,
        tripScore: Float?,
        eventCount: Int,
        filePath: String? = null
    ): Long {
        return eventReportDao.insertEventReport(
            EventReportEntity(
                title = title,
                content = content,
                generatedAt = Date(),
                driverStatus = driverStatus,
                tripScore = tripScore,
                eventCount = eventCount,
                filePath = filePath
            )
        )
    }

    fun getAllEventReports(): Flow<List<EventReportEntity>> {
        return eventReportDao.getAllEventReports()
    }

    suspend fun getEventReportById(id: Long): EventReportEntity? {
        return eventReportDao.getEventReportById(id)
    }

    suspend fun deleteEventReport(id: Long) {
        eventReportDao.deleteEventReport(id)
    }

    // Insurance Reports
    suspend fun saveInsuranceReport(
        title: String,
        content: String,
        riskScore: Float?,
        basePremium: Float?,
        estimatedPremium: Float?,
        discountEligible: Boolean,
        filePath: String? = null
    ): Long {
        return insuranceReportDao.insertInsuranceReport(
            InsuranceReportEntity(
                title = title,
                content = content,
                generatedAt = Date(),
                riskScore = riskScore,
                basePremium = basePremium,
                estimatedPremium = estimatedPremium,
                discountEligible = discountEligible,
                filePath = filePath
            )
        )
    }

    fun getAllInsuranceReports(): Flow<List<InsuranceReportEntity>> {
        return insuranceReportDao.getAllInsuranceReports()
    }

    suspend fun getInsuranceReportById(id: Long): InsuranceReportEntity? {
        return insuranceReportDao.getInsuranceReportById(id)
    }

    suspend fun deleteInsuranceReport(id: Long) {
        insuranceReportDao.deleteInsuranceReport(id)
    }

    // Event Summary
    suspend fun saveEvent(event: DrivingEvent) {
        eventSummaryDao.insertEvent(
            EventSummaryEntity(
                date = Date(event.timestamp),
                eventType = event.eventType.name,
                severity = event.severity.name,
                speed = event.speed,
                location = "${event.location?.latitude}" + "${event.location?.longitude}",
                confidence = event.confidence,
                timestamp = event.timestamp
            )
        )
    }

    fun getAllEvents(): Flow<List<EventSummaryEntity>> {
        return eventSummaryDao.getAllEvents()
    }

    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<EventSummaryEntity>> {
        return eventSummaryDao.getEventsByDateRange(startDate, endDate)
    }

    fun getEventsByType(eventType: String): Flow<List<EventSummaryEntity>> {
        return eventSummaryDao.getEventsByType(eventType)
    }

    suspend fun getEventCountForDate(date: Date): Int {
        return eventSummaryDao.getEventCountForDate(date)
    }
}
