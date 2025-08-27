package com.commerin.telemetri.data.local.dao

import androidx.room.*
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for trip summary data with comprehensive analytics capabilities
 */
@Dao
interface TripSummaryDao {

    // ============ INSERTION OPERATIONS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummary(trip: TripSummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummaries(trips: List<TripSummaryEntity>): List<Long>

    @Update
    suspend fun updateTripSummary(trip: TripSummaryEntity)

    // ============ BASIC RETRIEVAL OPERATIONS ============

    @Query("SELECT * FROM trip_summaries WHERE tripId = :tripId")
    suspend fun getTripSummaryById(tripId: String): TripSummaryEntity?

    @Query("SELECT * FROM trip_summaries ORDER BY startTimestamp DESC")
    fun getAllTripSummaries(): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries ORDER BY startTimestamp DESC LIMIT :limit")
    fun getRecentTripSummaries(limit: Int = 50): Flow<List<TripSummaryEntity>>

    // ============ TIME-BASED QUERIES ============

    @Query("SELECT * FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime ORDER BY startTimestamp DESC")
    fun getTripSummariesByTimeRange(startTime: Long, endTime: Long): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime ORDER BY startTimestamp DESC")
    suspend fun getTripSummariesInRange(startTime: Long, endTime: Long): List<TripSummaryEntity>

    @Query("SELECT * FROM trip_summaries WHERE DATE(startTimestamp/1000, 'unixepoch') = DATE(:dateTimestamp/1000, 'unixepoch') ORDER BY startTimestamp DESC")
    fun getTripSummariesByDate(dateTimestamp: Long): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE startTimestamp >= :startTime ORDER BY startTimestamp DESC")
    fun getTripSummariesAfterTime(startTime: Long): Flow<List<TripSummaryEntity>>

    // ============ SCORING AND PERFORMANCE QUERIES ============

    @Query("SELECT * FROM trip_summaries WHERE overallScore >= :minScore ORDER BY overallScore DESC")
    fun getHighScoringTrips(minScore: Float): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE overallScore < :maxScore ORDER BY overallScore ASC")
    fun getLowScoringTrips(maxScore: Float): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE safetyScore < :maxSafetyScore ORDER BY safetyScore ASC")
    fun getUnsafeTrips(maxSafetyScore: Float = 60f): Flow<List<TripSummaryEntity>>

    @Query("SELECT AVG(overallScore) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageOverallScore(startTime: Long, endTime: Long): Float?

    @Query("SELECT AVG(safetyScore) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageSafetyScore(startTime: Long, endTime: Long): Float?

    @Query("SELECT AVG(efficiencyScore) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageEfficiencyScore(startTime: Long, endTime: Long): Float?

    // ============ DISTANCE AND DURATION ANALYTICS ============

    @Query("SELECT SUM(totalDistance) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalDistanceInRange(startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(totalDuration) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalDurationInRange(startTime: Long, endTime: Long): Long?

    @Query("SELECT AVG(averageSpeed) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageSpeed(startTime: Long, endTime: Long): Float?

    @Query("SELECT MAX(maxSpeed) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxSpeedInRange(startTime: Long, endTime: Long): Float?

    @Query("SELECT * FROM trip_summaries WHERE totalDistance >= :minDistance ORDER BY totalDistance DESC")
    fun getLongTrips(minDistance: Float): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE totalDuration >= :minDuration ORDER BY totalDuration DESC")
    fun getLongDurationTrips(minDuration: Long): Flow<List<TripSummaryEntity>>

    // ============ EVENT COUNT ANALYTICS ============

    @Query("SELECT SUM(speedingEventCount) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpeedingEvents(startTime: Long, endTime: Long): Int?

    @Query("SELECT SUM(phoneUsageEventCount) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalPhoneUsageEvents(startTime: Long, endTime: Long): Int?

    @Query("SELECT SUM(criticalEventCount) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCriticalEvents(startTime: Long, endTime: Long): Int?

    @Query("SELECT * FROM trip_summaries WHERE (speedingEventCount + phoneUsageEventCount + hardBrakingEventCount + rapidAccelerationEventCount + harshCorneringEventCount) >= :minEventCount ORDER BY startTimestamp DESC")
    fun getHighEventTrips(minEventCount: Int): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE criticalEventCount > 0 ORDER BY criticalEventCount DESC, startTimestamp DESC")
    fun getTripsWithCriticalEvents(): Flow<List<TripSummaryEntity>>

    // ============ PHONE USAGE ANALYTICS ============

    @Query("SELECT * FROM trip_summaries WHERE phoneUsageEventCount > 0 ORDER BY phoneUsageEventCount DESC")
    fun getTripsWithPhoneUsage(): Flow<List<TripSummaryEntity>>

    @Query("SELECT AVG(phoneUsageConfidenceAvg) FROM trip_summaries WHERE phoneUsageEventCount > 0 AND startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAveragePhoneUsageConfidence(startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(phoneUsageTotalDuration) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalPhoneUsageDuration(startTime: Long, endTime: Long): Long?

    // ============ SPEEDING ANALYTICS ============

    @Query("SELECT * FROM trip_summaries WHERE speedingEventCount > 0 ORDER BY maxSpeedOverLimit DESC")
    fun getTripsWithSpeeding(): Flow<List<TripSummaryEntity>>

    @Query("SELECT MAX(maxSpeedOverLimit) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxSpeedViolation(startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(speedingDuration) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpeedingDuration(startTime: Long, endTime: Long): Long?

    @Query("SELECT SUM(urbanSpeedingCount + ruralSpeedingCount + highwaySpeedingCount) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpeedingViolations(startTime: Long, endTime: Long): Int?

    // ============ ROAD TYPE AND CONTEXT ANALYTICS ============

    @Query("SELECT AVG(highwayPercentage) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageHighwayUsage(startTime: Long, endTime: Long): Float?

    @Query("SELECT AVG(nightDrivingPercentage) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageNightDriving(startTime: Long, endTime: Long): Float?

    @Query("SELECT AVG(rushHourPercentage) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageRushHourDriving(startTime: Long, endTime: Long): Float?

    @Query("SELECT * FROM trip_summaries WHERE nightDrivingPercentage > :minNightPercentage ORDER BY nightDrivingPercentage DESC")
    fun getNightTrips(minNightPercentage: Float = 50f): Flow<List<TripSummaryEntity>>

    // ============ DRIVER PERFORMANCE TRENDS ============

    @Query("SELECT * FROM trip_summaries WHERE driverProfileId = :driverProfileId ORDER BY startTimestamp DESC")
    fun getTripsByDriverProfile(driverProfileId: String): Flow<List<TripSummaryEntity>>

    @Query("SELECT AVG(overallScore) FROM trip_summaries WHERE driverProfileId = :driverProfileId AND startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getDriverAverageScore(driverProfileId: String, startTime: Long, endTime: Long): Float?

    @Query("SELECT COUNT(*) FROM trip_summaries WHERE driverProfileId = :driverProfileId AND overallScore >= :minScore AND startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getDriverGoodTripCount(driverProfileId: String, minScore: Float, startTime: Long, endTime: Long): Int

    // ============ SYNC AND PROCESSING QUERIES ============

    @Query("SELECT * FROM trip_summaries WHERE synced = 0 ORDER BY startTimestamp ASC")
    suspend fun getUnsyncedTripSummaries(): List<TripSummaryEntity>

    @Query("SELECT * FROM trip_summaries WHERE processed = 0 ORDER BY startTimestamp ASC")
    suspend fun getUnprocessedTripSummaries(): List<TripSummaryEntity>

    @Query("SELECT * FROM trip_summaries WHERE reportGenerated = 0 ORDER BY startTimestamp ASC")
    suspend fun getTripsNeedingReports(): List<TripSummaryEntity>

    @Query("UPDATE trip_summaries SET synced = 1, lastUploadAttempt = :timestamp, updatedAt = :timestamp WHERE tripId IN (:tripIds)")
    suspend fun markTripsSynced(tripIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trip_summaries SET processed = 1, updatedAt = :timestamp WHERE tripId IN (:tripIds)")
    suspend fun markTripsProcessed(tripIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trip_summaries SET reportGenerated = 1, updatedAt = :timestamp WHERE tripId = :tripId")
    suspend fun markReportGenerated(tripId: String, timestamp: Long = System.currentTimeMillis())

    // ============ CLEANUP OPERATIONS ============

    @Query("DELETE FROM trip_summaries WHERE tripId = :tripId")
    suspend fun deleteTripSummary(tripId: String)

    @Query("DELETE FROM trip_summaries WHERE startTimestamp < :cutoffTime")
    suspend fun deleteTripSummariesBefore(cutoffTime: Long)

    @Query("DELETE FROM trip_summaries WHERE synced = 1 AND startTimestamp < :cutoffTime")
    suspend fun deleteSyncedTripSummariesBefore(cutoffTime: Long)

    @Query("DELETE FROM trip_summaries")
    suspend fun deleteAllTripSummaries()

    // ============ COUNT AND STATISTICS ============

    @Query("SELECT COUNT(*) FROM trip_summaries")
    suspend fun getTotalTripCount(): Int

    @Query("SELECT COUNT(*) FROM trip_summaries WHERE synced = 0")
    suspend fun getUnsyncedTripCount(): Int

    @Query("SELECT COUNT(*) FROM trip_summaries WHERE startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getTripCountInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM trip_summaries WHERE overallScore >= :minScore AND startTimestamp BETWEEN :startTime AND :endTime")
    suspend fun getGoodTripCount(minScore: Float, startTime: Long, endTime: Long): Int
}
