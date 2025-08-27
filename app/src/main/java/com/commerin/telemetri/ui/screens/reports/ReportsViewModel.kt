package com.commerin.telemetri.ui.screens.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.data.database.EventReportEntity
import com.commerin.telemetri.data.database.EventSummaryEntity
import com.commerin.telemetri.data.database.InsuranceReportEntity
import com.commerin.telemetri.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _eventReports = MutableLiveData<List<EventReportEntity>>()
    val eventReports: LiveData<List<EventReportEntity>> = _eventReports

    private val _insuranceReports = MutableLiveData<List<InsuranceReportEntity>>()
    val insuranceReports: LiveData<List<InsuranceReportEntity>> = _insuranceReports

    private val _eventSummary = MutableLiveData<List<EventSummaryEntity>>()
    val eventSummary: LiveData<List<EventSummaryEntity>> = _eventSummary

    private val _selectedReport = MutableLiveData<String>()
    val selectedReport: LiveData<String> = _selectedReport

    fun loadReports() {
        viewModelScope.launch {
            // Load event reports
            reportRepository.getAllEventReports().collectLatest { reports ->
                _eventReports.postValue(reports)
            }
        }

        viewModelScope.launch {
            // Load insurance reports
            reportRepository.getAllInsuranceReports().collectLatest { reports ->
                _insuranceReports.postValue(reports)
            }
        }

        viewModelScope.launch {
            // Load event summary
            reportRepository.getAllEvents().collectLatest { events ->
                _eventSummary.postValue(events)
            }
        }
    }

    fun viewReport(report: EventReportEntity) {
        _selectedReport.postValue(report.content)
    }

    fun viewInsuranceReport(report: InsuranceReportEntity) {
        _selectedReport.postValue(report.content)
    }

    fun deleteEventReport(reportId: Long) {
        viewModelScope.launch {
            try {
                reportRepository.deleteEventReport(reportId)
                // Reports will be automatically refreshed via Flow
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error deleting event report", e)
            }
        }
    }

    fun deleteInsuranceReport(reportId: Long) {
        viewModelScope.launch {
            try {
                reportRepository.deleteInsuranceReport(reportId)
                // Reports will be automatically refreshed via Flow
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error deleting insurance report", e)
            }
        }
    }

    fun filterEventsByType(eventType: String) {
        viewModelScope.launch {
            reportRepository.getEventsByType(eventType).collectLatest { events ->
                _eventSummary.postValue(events)
            }
        }
    }

    fun clearEventFilter() {
        viewModelScope.launch {
            reportRepository.getAllEvents().collectLatest { events ->
                _eventSummary.postValue(events)
            }
        }
    }
}
