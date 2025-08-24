package com.commerin.telemetri.core

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.InetSocketAddress
import java.net.Socket
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
        private const val TAG = "NetworkSpeedTestService"

        // Multiple test endpoints for redundancy and accuracy
        private val TEST_ENDPOINTS = listOf(
            TestEndpoint(
                name = "Cloudflare",
                downloadUrl = "https://speed.cloudflare.com/__down?bytes=",
                uploadUrl = "https://speed.cloudflare.com/__up",
                pingHost = "1.1.1.1",
                pingPort = 53
            ),
            TestEndpoint(
                name = "Google",
                downloadUrl = "https://www.gstatic.com/hostedimg/",
                uploadUrl = null, // Google doesn't have a public upload test
                pingHost = "8.8.8.8",
                pingPort = 53
            ),
            TestEndpoint(
                name = "OpenDNS",
                downloadUrl = null,
                uploadUrl = null,
                pingHost = "208.67.222.222",
                pingPort = 53
            )
        )

        private const val DOWNLOAD_SIZE_BYTES = 5_000_000 // 5MB per test
        private const val UPLOAD_SIZE_BYTES = 1_000_000   // 1MB per test
        private const val PING_TIMEOUT_MS = 3000
        private const val TEST_DURATION_MS = 15000L // 15 seconds max per test
        private const val PING_SAMPLES = 10 // Number of ping measurements
        private const val SPEED_TEST_SAMPLES = 3 // Number of speed test runs
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
                progress = 0.1f
            ) ?: SpeedTestResult(currentTestType = NetworkTestType.PING, progress = 0.1f)
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
                        _speedTestResult.value?.copy(progress = progress)
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
                progress = 0.33f
            ) ?: SpeedTestResult(currentTestType = NetworkTestType.DOWNLOAD, progress = 0.33f)
        )

        return withContext(Dispatchers.IO) {
            val downloadSpeeds = mutableListOf<Float>()

            for (sample in 0 until SPEED_TEST_SAMPLES) {
                try {
                    // Try different endpoints for redundancy
                    for (endpoint in TEST_ENDPOINTS) {
                        if (endpoint.downloadUrl != null) {
                            val speed = performSingleDownloadTest(endpoint, sample)
                            if (speed > 0) {
                                downloadSpeeds.add(speed)
                                break
                            }
                        }
                    }

                    // Update progress
                    val progress = 0.33f + ((sample + 1).toFloat() / SPEED_TEST_SAMPLES) * 0.33f
                    _speedTestResult.postValue(
                        _speedTestResult.value?.copy(progress = progress)
                    )

                } catch (e: Exception) {
                    Log.w(TAG, "Download test sample $sample failed", e)
                }
            }

            val avgDownloadSpeed = if (downloadSpeeds.isNotEmpty()) {
                downloadSpeeds.average().toFloat()
            } else {
                0f
            }

            _speedTestResult.postValue(
                _speedTestResult.value?.copy(
                    downloadSpeed = avgDownloadSpeed,
                    progress = 0.66f
                )
            )

            avgDownloadSpeed
        }
    }

    private fun performSingleDownloadTest(endpoint: TestEndpoint, sample: Int): Float {
        return try {
            val url = if (endpoint.name == "Cloudflare") {
                URL("${endpoint.downloadUrl}$DOWNLOAD_SIZE_BYTES")
            } else {
                // For other endpoints, use a known large file
                URL("https://www.gstatic.com/hostedimg/382a91be96472318_large")
            }

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "NetworkTelemetrySDK/1.0")

            val startTime = System.currentTimeMillis()
            val expectedSize = connection.contentLength.toLong()
            val inputStream: InputStream = BufferedInputStream(connection.inputStream)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead

                // Stop if we've reached our target size or timeout
                if (totalBytesRead >= DOWNLOAD_SIZE_BYTES ||
                    System.currentTimeMillis() - startTime > TEST_DURATION_MS) {
                    break
                }
            }

            inputStream.close()
            connection.disconnect()

            val endTime = System.currentTimeMillis()
            val timeTakenSeconds = (endTime - startTime) / 1000.0

            // Verify we got meaningful data
            if (totalBytesRead < 1000 || timeTakenSeconds < 0.1) {
                Log.w(TAG, "Download test incomplete: ${totalBytesRead} bytes in ${timeTakenSeconds}s")
                return 0f
            }

            val speedMbps = ((totalBytesRead * 8) / (timeTakenSeconds * 1_000_000)).toFloat()
            Log.d(TAG, "Download sample $sample: ${totalBytesRead} bytes in ${timeTakenSeconds}s = ${speedMbps} Mbps")

            speedMbps
        } catch (e: Exception) {
            Log.e(TAG, "Download test failed for ${endpoint.name}", e)
            0f
        }
    }

    private suspend fun runEnhancedUploadTest(): Float {
        _speedTestResult.postValue(
            _speedTestResult.value?.copy(
                currentTestType = NetworkTestType.UPLOAD,
                progress = 0.66f
            ) ?: SpeedTestResult(currentTestType = NetworkTestType.UPLOAD, progress = 0.66f)
        )

        return withContext(Dispatchers.IO) {
            val uploadSpeeds = mutableListOf<Float>()

            for (sample in 0 until SPEED_TEST_SAMPLES) {
                try {
                    // Try endpoints that support upload
                    for (endpoint in TEST_ENDPOINTS) {
                        if (endpoint.uploadUrl != null) {
                            val speed = performSingleUploadTest(endpoint, sample)
                            if (speed > 0) {
                                uploadSpeeds.add(speed)
                                break
                            }
                        }
                    }

                    // Update progress
                    val progress = 0.66f + ((sample + 1).toFloat() / SPEED_TEST_SAMPLES) * 0.34f
                    _speedTestResult.postValue(
                        _speedTestResult.value?.copy(progress = progress)
                    )

                } catch (e: Exception) {
                    Log.w(TAG, "Upload test sample $sample failed", e)
                }
            }

            val avgUploadSpeed = if (uploadSpeeds.isNotEmpty()) {
                uploadSpeeds.average().toFloat()
            } else {
                0f
            }

            avgUploadSpeed
        }
    }

    private fun performSingleUploadTest(endpoint: TestEndpoint, sample: Int): Float {
        return try {
            val url = URL(endpoint.uploadUrl!!)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty("User-Agent", "NetworkTelemetrySDK/1.0")

            val startTime = System.currentTimeMillis()
            val outputStream: OutputStream = connection.outputStream

            // Generate random data for more realistic upload test
            val buffer = ByteArray(8192)
            for (i in buffer.indices) {
                buffer[i] = (Math.random() * 256).toInt().toByte()
            }

            var totalBytesWritten = 0L

            while (totalBytesWritten < UPLOAD_SIZE_BYTES) {
                val bytesToWrite = minOf(buffer.size, (UPLOAD_SIZE_BYTES - totalBytesWritten).toInt())
                outputStream.write(buffer, 0, bytesToWrite)
                outputStream.flush()
                totalBytesWritten += bytesToWrite

                // Check timeout
                if (System.currentTimeMillis() - startTime > TEST_DURATION_MS) break
            }

            outputStream.close()

            // Verify response
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            val endTime = System.currentTimeMillis()

            connection.disconnect()

            // Check for successful upload
            if (responseCode !in 200..299) {
                Log.w(TAG, "Upload failed with response: $responseCode $responseMessage")
                return 0f
            }

            val timeTakenSeconds = (endTime - startTime) / 1000.0

            // Verify we uploaded meaningful data
            if (totalBytesWritten < 1000 || timeTakenSeconds < 0.1) {
                Log.w(TAG, "Upload test incomplete: ${totalBytesWritten} bytes in ${timeTakenSeconds}s")
                return 0f
            }

            val speedMbps = ((totalBytesWritten * 8) / (timeTakenSeconds * 1_000_000)).toFloat()
            Log.d(TAG, "Upload sample $sample: ${totalBytesWritten} bytes in ${timeTakenSeconds}s = ${speedMbps} Mbps")

            speedMbps
        } catch (e: Exception) {
            Log.e(TAG, "Upload test failed for ${endpoint.name}", e)
            0f
        }
    }

    fun cleanup() {
        currentTestJob?.cancel()
        scope.cancel()
    }
}
