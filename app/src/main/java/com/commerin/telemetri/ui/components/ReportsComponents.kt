package com.commerin.telemetri.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


/**
 * Card showing report generation status - now clickable for navigation
 */
@Composable
fun ReportStatusesCard(
    status: SmartFleetViewModel.ReportGenerationStatus,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status.status) {
                SmartFleetViewModel.ReportStatus.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                SmartFleetViewModel.ReportStatus.ERROR -> Color(0xFFF44336).copy(alpha = 0.1f)
                SmartFleetViewModel.ReportStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.1f)
                SmartFleetViewModel.ReportStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onNavigateToReports // Make the entire card clickable
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status.status) {
                    SmartFleetViewModel.ReportStatus.SUCCESS -> Icons.Default.CheckCircle
                    SmartFleetViewModel.ReportStatus.ERROR -> Icons.Default.Error
                    SmartFleetViewModel.ReportStatus.IN_PROGRESS -> Icons.Default.Refresh
                    SmartFleetViewModel.ReportStatus.IDLE -> Icons.Default.Description
                },
                contentDescription = null,
                tint = when (status.status) {
                    SmartFleetViewModel.ReportStatus.SUCCESS -> Color(0xFF4CAF50)
                    SmartFleetViewModel.ReportStatus.ERROR -> Color(0xFFF44336)
                    SmartFleetViewModel.ReportStatus.IN_PROGRESS -> Color(0xFF2196F3)
                    SmartFleetViewModel.ReportStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (status.status) {
                        SmartFleetViewModel.ReportStatus.SUCCESS -> "Report Generated Successfully"
                        SmartFleetViewModel.ReportStatus.ERROR -> "Report Generation Failed"
                        SmartFleetViewModel.ReportStatus.IN_PROGRESS -> "Generating Report..."
                        SmartFleetViewModel.ReportStatus.IDLE -> "Ready to Generate Reports"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (status.message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (status.filePath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved to: ${status.filePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Add tap hint for navigation
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to view all reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Show progress indicator for in-progress status or navigation arrow
            if (status.status == SmartFleetViewModel.ReportStatus.IN_PROGRESS) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2196F3)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Navigate to reports",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Import for the missing ViewModel classes - these need to be defined in your SmartFleetViewModel
// This is a placeholder to prevent compilation errors
object SmartFleetViewModel {
    enum class ReportStatus {
        IDLE, IN_PROGRESS, SUCCESS, ERROR
    }

    data class ReportGenerationStatus(
        val status: ReportStatus,
        val message: String,
        val filePath: String,
        val timestamp: Long = System.currentTimeMillis()
    )
}
