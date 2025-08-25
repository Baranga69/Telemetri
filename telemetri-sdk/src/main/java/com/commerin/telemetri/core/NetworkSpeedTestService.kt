package com.commerin.telemetri.core

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

data class SpeedTestResult(
    val downloadSpeed: Float = 0f, // Mbps
    val uploadSpeed: Float = 0f,   // Mbps
    val ping: Float = 0f,          // ms
    val jitter: Float = 0f,        // ms (connection stability)
    val packetLoss: Float = 0f,    // percentage
    val isTestRunning: Boolean = false,
    val currentTestType: NetworkTestType? = null,
    val progress: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

enum class NetworkTestType {
    DOWNLOAD, UPLOAD, PING
}

class NetworkSpeedTestService {
    companion object {
        private const val TAG = "NetworkSpeedTest"

        // Optimized test endpoints for maximum performance
        private val TEST_ENDPOINTS = listOf(
            TestEndpoint(
                name = "Cloudflare",
                downloadUrl = "https://speed.cloudflare.com/__down?bytes=",
                uploadUrl = "https://speed.cloudflare.com/__up",
                pingHost = "1.1.1.1",
                pingPort = 53
            ),
            TestEndpoint(
                name = "Fast.com",
                downloadUrl = "https://api.fast.com/netflix/speedtest/v2/cdn",
                uploadUrl = null,
                pingHost = "8.8.8.8",
                pingPort = 53
            ),
            TestEndpoint(
                name = "Google",
                downloadUrl = "https://www.gstatic.com/hostedimg/",
                uploadUrl = null,
                pingHost = "8.8.4.4",
                pingPort = 53
            )
        )

        // Optimized test parameters for high-speed networks
        private const val DOWNLOAD_SIZE_BYTES = 50_000_000 // 50MB for accurate high-speed measurement
        private const val UPLOAD_SIZE_BYTES = 25_000_000   // 25MB for upload test
        private const val PING_TIMEOUT_MS = 3000
        private const val TEST_DURATION_MS = 30000L // 30 seconds max per test for high-speed networks
        private const val BUFFER_SIZE = 65536 // 64KB buffer for optimal throughput
        private const val PING_SAMPLES = 10
        private const val PARALLEL_CONNECTIONS = 4 // Multiple parallel connections for max throughput
        private const val MIN_TEST_DURATION_MS = 5000L // Minimum 5 seconds for accurate measurement
        private const val SPEED_TEST_SAMPLES = 1 // Single optimized test per phase
    }

    data class TestEndpoint(
        val name: String,
        val downloadUrl: String?,
        val uploadUrl: String?,
        val pingHost: String,
        val pingPort: Int
    )

    private val _speedTestResult = MutableLiveData<SpeedTestResult>()
    val speedTestResult: LiveData<SpeedTestResult> = _speedTestResult

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentTestJob: Job? = null

    fun startSpeedTest() {
        if (currentTestJob?.isActive == true) {
            Log.w(TAG, "Speed test already running")
            return
        }

        currentTestJob = scope.launch {
            try {
                _speedTestResult.postValue(
                    SpeedTestResult(isTestRunning = true, progress = 0f)
                )

                // Run ping test with multiple samples
                val (pingResult, jitterResult, packetLossResult) = runEnhancedPingTest()

                // Run download test with multiple samples
                val downloadResult = runEnhancedDownloadTest()

                // Run upload test with multiple samples
                val uploadResult = runEnhancedUploadTest()

                // Final result
                _speedTestResult.postValue(
                    SpeedTestResult(
                        downloadSpeed = downloadResult,
                        uploadSpeed = uploadResult,
                        ping = pingResult,
                        jitter = jitterResult,
                        packetLoss = packetLossResult,
                        isTestRunning = false,
                        currentTestType = null,
                        progress = 1f
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed", e)
                _speedTestResult.postValue(
                    SpeedTestResult(
                        isTestRunning = false,
                        currentTestType = null,
                        progress = 0f,
                        errorMessage = "Test failed: ${e.message}"
                    )
                )
            }
        }
    }

    fun stopSpeedTest() {
        currentTestJob?.cancel()
        _speedTestResult.postValue(
            SpeedTestResult(
                isTestRunning = false,
                currentTestType = null,
                progress = 0f
            )
        )
    }

    private suspend fun runEnhancedPingTest(): Triple<Float, Float, Float> {
        _speedTestResult.postValue(
            _speedTestResult.value?.copy(
                currentTestType = NetworkTestType.PING,
                progress = 0.1f,
                isTestRunning = true
            ) ?: SpeedTestResult(
                currentTestType = NetworkTestType.PING,
                progress = 0.1f,
                isTestRunning = true
            )
        )

        return withContext(Dispatchers.IO) {
            val pingResults = mutableListOf<Long>()
            var successfulPings = 0

            for (i in 0 until PING_SAMPLES) {
                try {
                    // Test multiple endpoints for redundancy
                    for (endpoint in TEST_ENDPOINTS) {
                        try {
                            val pingTime = measureTcpPing(endpoint.pingHost, endpoint.pingPort)
                            if (pingTime > 0) {
                                pingResults.add(pingTime)
                                successfulPings++
                                break // Use first successful ping for this sample
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Ping failed for ${endpoint.name}: ${e.message}")
                        }
                    }

                    // Update progress
                    val progress = 0.1f + (i.toFloat() / PING_SAMPLES) * 0.23f
                    _speedTestResult.postValue(
                        _speedTestResult.value?.copy(
                            progress = progress,
                            isTestRunning = true
                        ) ?: SpeedTestResult(
                            currentTestType = NetworkTestType.PING,
                            progress = progress,
                            isTestRunning = true
                        )
                    )

                    delay(100) // Small delay between pings
                } catch (e: Exception) {
                    Log.w(TAG, "Ping sample $i failed", e)
                }
            }

            val avgPing = if (pingResults.isNotEmpty()) {
                pingResults.average().toFloat()
            } else {
                -1f
            }

            val jitter = if (pingResults.size > 1) {
                calculateJitter(pingResults)
            } else {
                0f
            }

            val packetLoss = if (PING_SAMPLES > 0) {
                ((PING_SAMPLES - successfulPings).toFloat() / PING_SAMPLES) * 100f
            } else {
                100f
            }

            _speedTestResult.postValue(
                _speedTestResult.value?.copy(
                    ping = avgPing,
                    jitter = jitter,
                    packetLoss = packetLoss,
                    progress = 0.33f
                )
            )

            Triple(avgPing, jitter, packetLoss)
        }
    }

    private fun measureTcpPing(host: String, port: Int): Long {
        val startTime = System.currentTimeMillis()
        val socket = Socket()

        return try {
            socket.connect(InetSocketAddress(host, port), PING_TIMEOUT_MS)
            val endTime = System.currentTimeMillis()
            socket.close()
            endTime - startTime
        } catch (e: Exception) {
            socket.close()
            -1L
        }
    }

    private fun calculateJitter(pingResults: List<Long>): Float {
        if (pingResults.size < 2) return 0f

        val mean = pingResults.average()
        val variance = pingResults.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private suspend fun runEnhancedDownloadTest(): Float {
        _speedTestResult.postValue(
            _speedTestResult.value?.copy(
                currentTestType = NetworkTestType.DOWNLOAD,
                progress = 0.33f,
                isTestRunning = true
            ) ?: SpeedTestResult(
                currentTestType = NetworkTestType.DOWNLOAD,
                progress = 0.33f,
                isTestRunning = true
            )
        )

        return withContext(Dispatchers.IO) {
            // Use parallel connections for maximum throughput
            val downloadSpeeds = mutableListOf<Float>()

            try {
                // Run parallel download test for maximum speed
                val speed = performParallelDownloadTest()
                if (speed > 0) {
                    downloadSpeeds.add(speed)
                }

                // Update progress
                _speedTestResult.postValue(
                    _speedTestResult.value?.copy(
                        progress = 0.66f,
                        isTestRunning = true
                    ) ?: SpeedTestResult(
                        currentTestType = NetworkTestType.DOWNLOAD,
                        progress = 0.66f,
                        isTestRunning = true
                    )
                )

            } catch (e: Exception) {
                Log.w(TAG, "Download test failed", e)
            }

            val avgDownloadSpeed = if (downloadSpeeds.isNotEmpty()) {
                downloadSpeeds.average().toFloat()
            } else {
                0f
            }

            _speedTestResult.postValue(
                _speedTestResult.value?.copy(
                    downloadSpeed = avgDownloadSpeed,
                    progress = 0.66f,
                    isTestRunning = true
                ) ?: SpeedTestResult(
                    downloadSpeed = avgDownloadSpeed,
                    progress = 0.66f,
                    isTestRunning = true
                )
            )

            avgDownloadSpeed
        }
    }

    private suspend fun performParallelDownloadTest(): Float = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val totalBytesCounter = AtomicLong(0L)
        val activeConnections = AtomicInteger(0)

        try {
            // Create multiple parallel download jobs
            val downloadJobs = (1..PARALLEL_CONNECTIONS).map { connectionId ->
                async {
                    performHighSpeedDownload(connectionId, totalBytesCounter, activeConnections)
                }
            }

            // Wait for either all jobs to complete or minimum test duration
            val results = downloadJobs.awaitAll()
            val endTime = System.currentTimeMillis()
            val timeTakenSeconds = (endTime - startTime) / 1000.0
            val totalBytesRead = totalBytesCounter.get()

            // Ensure minimum test duration for accurate measurement
            val effectiveTime = maxOf(timeTakenSeconds, MIN_TEST_DURATION_MS / 1000.0)

            if (totalBytesRead > 0 && effectiveTime > 0) {
                val speedMbps = ((totalBytesRead * 8) / (effectiveTime * 1_000_000)).toFloat()
                Log.d(TAG, "Parallel download: ${totalBytesRead / 1_000_000}MB in ${effectiveTime}s = ${speedMbps} Mbps")
                speedMbps
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parallel download test failed", e)
            0f
        }
    }

    private suspend fun performHighSpeedDownload(
        connectionId: Int,
        totalBytesCounter: AtomicLong,
        activeConnections: AtomicInteger
    ): Long = withContext(Dispatchers.IO) {
        var bytesRead = 0L
        activeConnections.incrementAndGet()

        try {
            val url = URL("https://speed.cloudflare.com/__down?bytes=$DOWNLOAD_SIZE_BYTES")
            val connection = url.openConnection() as HttpURLConnection

            // Optimized connection settings for high throughput
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.setRequestProperty("User-Agent", "SpeedTest/1.0")
            connection.setRequestProperty("Accept-Encoding", "identity") // Disable compression
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Connection", "keep-alive")

            val inputStream = BufferedInputStream(connection.inputStream, BUFFER_SIZE)
            val buffer = ByteArray(BUFFER_SIZE)
            val startTime = System.currentTimeMillis()

            var readBytes: Int
            while (inputStream.read(buffer).also { readBytes = it } != -1) {
                bytesRead += readBytes
                totalBytesCounter.addAndGet(readBytes.toLong())

                // Stop if test duration exceeded
                if (System.currentTimeMillis() - startTime > TEST_DURATION_MS) {
                    break
                }
            }

            inputStream.close()
            connection.disconnect()

            Log.v(TAG, "Connection $connectionId downloaded: ${bytesRead / 1_000_000}MB")

        } catch (e: Exception) {
            Log.w(TAG, "Download connection $connectionId failed: ${e.message}")
        } finally {
            activeConnections.decrementAndGet()
        }

        bytesRead
    }

    private suspend fun runEnhancedUploadTest(): Float {
        _speedTestResult.postValue(
            _speedTestResult.value?.copy(
                currentTestType = NetworkTestType.UPLOAD,
                progress = 0.66f,
                isTestRunning = true
            ) ?: SpeedTestResult(
                currentTestType = NetworkTestType.UPLOAD,
                progress = 0.66f,
                isTestRunning = true
            )
        )

        return withContext(Dispatchers.IO) {
            val uploadSpeeds = mutableListOf<Float>()

            try {
                // Use parallel upload test for maximum speed
                val speed = performParallelUploadTest()
                if (speed > 0) {
                    uploadSpeeds.add(speed)
                }

                // Update progress
                _speedTestResult.postValue(
                    _speedTestResult.value?.copy(
                        progress = 1.0f,
                        isTestRunning = true
                    ) ?: SpeedTestResult(
                        currentTestType = NetworkTestType.UPLOAD,
                        progress = 1.0f,
                        isTestRunning = true
                    )
                )

            } catch (e: Exception) {
                Log.w(TAG, "Upload test failed", e)
            }

            val avgUploadSpeed = if (uploadSpeeds.isNotEmpty()) {
                uploadSpeeds.average().toFloat()
            } else {
                0f
            }

            avgUploadSpeed
        }
    }

    private suspend fun performParallelUploadTest(): Float = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val totalBytesCounter = AtomicLong(0L)
        val activeConnections = AtomicInteger(0)

        try {
            // Create multiple parallel upload jobs
            val uploadJobs = (1..PARALLEL_CONNECTIONS).map { connectionId ->
                async {
                    performHighSpeedUpload(connectionId, totalBytesCounter, activeConnections)
                }
            }

            // Wait for either all jobs to complete or minimum test duration
            val results = uploadJobs.awaitAll()
            val endTime = System.currentTimeMillis()
            val timeTakenSeconds = (endTime - startTime) / 1000.0
            val totalBytesWritten = totalBytesCounter.get()

            // Ensure minimum test duration for accurate measurement
            val effectiveTime = maxOf(timeTakenSeconds, MIN_TEST_DURATION_MS / 1000.0)

            if (totalBytesWritten > 0 && effectiveTime > 0) {
                val speedMbps = ((totalBytesWritten * 8) / (effectiveTime * 1_000_000)).toFloat()
                Log.d(TAG, "Parallel upload: ${totalBytesWritten / 1_000_000}MB in ${effectiveTime}s = ${speedMbps} Mbps")
                speedMbps
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parallel upload test failed", e)
            0f
        }
    }

    private suspend fun performHighSpeedUpload(
        connectionId: Int,
        totalBytesCounter: AtomicLong,
        activeConnections: AtomicInteger
    ): Long = withContext(Dispatchers.IO) {
        var bytesWritten = 0L
        activeConnections.incrementAndGet()

        try {
            val url = URL("https://speed.cloudflare.com/__up")
            val connection = url.openConnection() as HttpURLConnection

            // Optimized connection settings for high throughput
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.setRequestProperty("User-Agent", "SpeedTest/1.0")
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Connection", "keep-alive")

            val outputStream = BufferedOutputStream(connection.outputStream, BUFFER_SIZE)
            val buffer = ByteArray(BUFFER_SIZE)

            // Pre-generate random data for upload
            for (i in buffer.indices) {
                buffer[i] = (Math.random() * 256).toInt().toByte()
            }

            val startTime = System.currentTimeMillis()

            while (bytesWritten < UPLOAD_SIZE_BYTES) {
                val bytesToWrite = minOf(buffer.size, (UPLOAD_SIZE_BYTES - bytesWritten).toInt())
                outputStream.write(buffer, 0, bytesToWrite)
                bytesWritten += bytesToWrite
                totalBytesCounter.addAndGet(bytesToWrite.toLong())

                // Stop if test duration exceeded
                if (System.currentTimeMillis() - startTime > TEST_DURATION_MS) {
                    break
                }
            }

            outputStream.flush()
            outputStream.close()

            // Verify response
            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode !in 200..299) {
                Log.w(TAG, "Upload connection $connectionId failed with response: $responseCode")
                return@withContext 0L
            }

            Log.v(TAG, "Connection $connectionId uploaded: ${bytesWritten / 1_000_000}MB")

        } catch (e: Exception) {
            Log.w(TAG, "Upload connection $connectionId failed: ${e.message}")
        } finally {
            activeConnections.decrementAndGet()
        }

        bytesWritten
    }

    fun cleanup() {
        currentTestJob?.cancel()
        scope.cancel()
    }
}
