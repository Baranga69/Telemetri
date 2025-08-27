package com.commerin.telemetri.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface EventReportDao {
    @Insert
    suspend fun insertEventReport(report: EventReportEntity): Long

    @Query("SELECT * FROM event_reports ORDER BY generatedAt DESC")
    fun getAllEventReports(): Flow<List<EventReportEntity>>

    @Query("SELECT * FROM event_reports WHERE id = :id")
    suspend fun getEventReportById(id: Long): EventReportEntity?

    @Query("DELETE FROM event_reports WHERE id = :id")
    suspend fun deleteEventReport(id: Long)
}

@Dao
interface InsuranceReportDao {
    @Insert
    suspend fun insertInsuranceReport(report: InsuranceReportEntity): Long

    @Query("SELECT * FROM insurance_reports ORDER BY generatedAt DESC")
    fun getAllInsuranceReports(): Flow<List<InsuranceReportEntity>>

    @Query("SELECT * FROM insurance_reports WHERE id = :id")
    suspend fun getInsuranceReportById(id: Long): InsuranceReportEntity?

    @Query("DELETE FROM insurance_reports WHERE id = :id")
    suspend fun deleteInsuranceReport(id: Long)
}

@Dao
interface EventSummaryDao {
    @Insert
    suspend fun insertEvent(event: EventSummaryEntity): Long

    @Query("SELECT * FROM event_summary ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventSummaryEntity>>

    @Query("SELECT * FROM event_summary WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<EventSummaryEntity>>

    @Query("SELECT * FROM event_summary WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<EventSummaryEntity>>

    @Query("SELECT COUNT(*) FROM event_summary WHERE date = :date")
    suspend fun getEventCountForDate(date: Date): Int

    @Query("DELETE FROM event_summary WHERE id = :id")
    suspend fun deleteEvent(id: Long)
}
