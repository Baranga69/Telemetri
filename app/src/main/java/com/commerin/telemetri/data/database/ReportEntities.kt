package com.commerin.telemetri.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "event_reports")
data class EventReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val generatedAt: Date,
    val driverStatus: String?,
    val tripScore: Float?,
    val eventCount: Int,
    val filePath: String? = null
)

@Entity(tableName = "insurance_reports")
data class InsuranceReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val generatedAt: Date,
    val riskScore: Float?,
    val basePremium: Float?,
    val estimatedPremium: Float?,
    val discountEligible: Boolean,
    val filePath: String? = null
)

@Entity(tableName = "event_summary")
data class EventSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val eventType: String,
    val severity: String,
    val speed: Float,
    val location: String?,
    val confidence: Float,
    val timestamp: Long
)
