package com.commerin.telemetri.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 50

    suspend fun generateEventReportPdf(
        context: Context,
        title: String,
        content: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Paint for text
            val titlePaint = Paint().apply {
                textSize = 20f
                isFakeBoldText = true
            }

            val bodyPaint = Paint().apply {
                textSize = 12f
            }

            val smallPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }

            var yPosition = MARGIN + 30f

            // Title
            canvas.drawText(title, MARGIN.toFloat(), yPosition, titlePaint)
            yPosition += 40f

            // Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            canvas.drawText("Generated: ${dateFormat.format(Date())}", MARGIN.toFloat(), yPosition, smallPaint)
            yPosition += 30f

            // Content
            val lines = content.split("\n")
            for (line in lines) {
                if (yPosition > PAGE_HEIGHT - MARGIN) {
                    // Start new page if needed
                    document.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
                    val newPage = document.startPage(newPageInfo)
                    canvas.drawText(line, MARGIN.toFloat(), MARGIN + 30f, bodyPaint)
                    yPosition = MARGIN + 50f
                } else {
                    canvas.drawText(line, MARGIN.toFloat(), yPosition, bodyPaint)
                    yPosition += 15f
                }
            }

            document.finishPage(page)

            // Save to file
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "TelemetriReport_${System.currentTimeMillis()}.pdf"
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }

            document.close()

            Result.success(file.absolutePath)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateInsuranceReportPdf(
        context: Context,
        title: String,
        content: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Similar implementation but with insurance-specific formatting
        generateEventReportPdf(context, title, content)
    }
}
