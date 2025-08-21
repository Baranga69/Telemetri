package com.commerin.telemetri.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.AudioTelemetryData
import com.commerin.telemetri.domain.model.SoundClassification
import kotlinx.coroutines.*
import kotlin.math.*

class AudioTelemetryService(private val context: Context) {
    companion object {
        private const val TAG = "AudioTelemetryService"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2

        // Audio analysis parameters
        private const val ANALYSIS_WINDOW_MS = 1000 // 1 second analysis window
        private const val NOISE_THRESHOLD_DB = 30.0 // Minimum noise level to consider
        private const val FREQUENCY_BINS = 512
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var analysisJob: Job? = null

    private val _audioData = MutableLiveData<AudioTelemetryData>()
    val audioData: LiveData<AudioTelemetryData> = _audioData

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startAudioTelemetry() {
        if (!hasAudioPermission()) {
            Log.w(TAG, "Audio recording permission not granted")
            return
        }

        if (isRecording) {
            Log.d(TAG, "Audio telemetry already running")
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            ) * BUFFER_SIZE_FACTOR

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            analysisJob = scope.launch {
                performAudioAnalysis(bufferSize)
            }

            Log.d(TAG, "Audio telemetry started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio telemetry", e)
            stopAudioTelemetry()
        }
    }

    fun stopAudioTelemetry() {
        isRecording = false
        analysisJob?.cancel()

        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null

        Log.d(TAG, "Audio telemetry stopped")
    }

    private suspend fun performAudioAnalysis(bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val analysisBuffer = mutableListOf<Short>()
        val samplesPerAnalysis = (SAMPLE_RATE * ANALYSIS_WINDOW_MS) / 1000

        while (isRecording && audioRecord != null) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (bytesRead > 0) {
                    // Add samples to analysis buffer
                    analysisBuffer.addAll(buffer.take(bytesRead))

                    // Perform analysis when we have enough samples
                    if (analysisBuffer.size >= samplesPerAnalysis) {
                        val audioTelemetry = analyzeAudioData(
                            analysisBuffer.take(samplesPerAnalysis).toShortArray()
                        )

                        withContext(Dispatchers.Main) {
                            _audioData.postValue(audioTelemetry)
                        }

                        // Remove analyzed samples, keep overlap for continuity
                        val overlap = samplesPerAnalysis / 4
                        analysisBuffer.subList(0, samplesPerAnalysis - overlap).clear()
                    }
                }

                // Small delay to prevent excessive CPU usage
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio analysis loop", e)
                break
            }
        }
    }

    private fun analyzeAudioData(samples: ShortArray): AudioTelemetryData {
        val timestamp = System.currentTimeMillis()

        // Calculate amplitude metrics
        val amplitude = calculateAmplitude(samples)
        val decibels = amplitudeToDecibels(amplitude)

        // Perform frequency analysis
        val frequencyAnalysis = performFFT(samples)
        val dominantFrequency = findDominantFrequency(frequencyAnalysis)
        val spectralCentroid = calculateSpectralCentroid(frequencyAnalysis)

        // Classify sound type
        val soundClassification = classifySound(
            decibels, dominantFrequency, spectralCentroid, frequencyAnalysis
        )

        // Calculate additional metrics
        val zeroCrossingRate = calculateZeroCrossingRate(samples)
        val spectralRolloff = calculateSpectralRolloff(frequencyAnalysis)
        val spectralFlux = calculateSpectralFlux(frequencyAnalysis)

        return AudioTelemetryData(
            amplitude = amplitude,
            decibels = decibels,
            dominantFrequency = dominantFrequency,
            spectralCentroid = spectralCentroid,
            spectralRolloff = spectralRolloff,
            spectralFlux = spectralFlux,
            zeroCrossingRate = zeroCrossingRate,
            soundClassification = soundClassification,
            frequencySpectrum = frequencyAnalysis.take(FREQUENCY_BINS).toFloatArray(),
            isVoiceDetected = detectVoice(dominantFrequency, spectralCentroid),
            noiseLevelCategory = categorizeNoiseLevel(decibels),
            timestamp = timestamp
        )
    }

    private fun calculateAmplitude(samples: ShortArray): Float {
        var sum = 0.0
        for (sample in samples) {
            sum += abs(sample.toDouble())
        }
        return (sum / samples.size).toFloat()
    }

    private fun amplitudeToDecibels(amplitude: Float): Float {
        return if (amplitude > 0) {
            20 * log10(amplitude / Short.MAX_VALUE)
        } else {
            -96.0f // Minimum dB level for 16-bit audio
        }
    }

    private fun performFFT(samples: ShortArray): FloatArray {
        // Simple magnitude spectrum calculation
        // In a production environment, you might want to use a proper FFT library
        val fftSize = min(samples.size, 1024)
        val spectrum = FloatArray(fftSize / 2)

        for (i in spectrum.indices) {
            var real = 0.0
            var imag = 0.0

            for (j in 0 until fftSize) {
                val angle = -2 * PI * i * j / fftSize
                real += samples[j] * cos(angle)
                imag += samples[j] * sin(angle)
            }

            spectrum[i] = sqrt(real * real + imag * imag).toFloat()
        }

        return spectrum
    }

    private fun findDominantFrequency(spectrum: FloatArray): Float {
        var maxIndex = 0
        var maxValue = 0f

        for (i in spectrum.indices) {
            if (spectrum[i] > maxValue) {
                maxValue = spectrum[i]
                maxIndex = i
            }
        }

        return (maxIndex * SAMPLE_RATE.toFloat()) / (2 * spectrum.size)
    }

    private fun calculateSpectralCentroid(spectrum: FloatArray): Float {
        var weightedSum = 0f
        var totalSum = 0f

        for (i in spectrum.indices) {
            val frequency = (i * SAMPLE_RATE.toFloat()) / (2 * spectrum.size)
            weightedSum += frequency * spectrum[i]
            totalSum += spectrum[i]
        }

        return if (totalSum > 0) weightedSum / totalSum else 0f
    }

    private fun calculateZeroCrossingRate(samples: ShortArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / samples.size
    }

    private fun calculateSpectralRolloff(spectrum: FloatArray, threshold: Float = 0.85f): Float {
        val totalEnergy = spectrum.sum()
        val targetEnergy = totalEnergy * threshold

        var cumulativeEnergy = 0f
        for (i in spectrum.indices) {
            cumulativeEnergy += spectrum[i]
            if (cumulativeEnergy >= targetEnergy) {
                return (i * SAMPLE_RATE.toFloat()) / (2 * spectrum.size)
            }
        }
        return 0f
    }

    private fun calculateSpectralFlux(spectrum: FloatArray): Float {
        // Simplified spectral flux calculation
        // In practice, this would compare with previous frame
        var flux = 0f
        for (i in 1 until spectrum.size) {
            val diff = spectrum[i] - spectrum[i - 1]
            if (diff > 0) flux += diff
        }
        return flux / spectrum.size
    }

    private fun classifySound(
        decibels: Float,
        dominantFreq: Float,
        spectralCentroid: Float,
        spectrum: FloatArray
    ): SoundClassification {
        return when {
            decibels < NOISE_THRESHOLD_DB -> SoundClassification.SILENCE
            detectVoice(dominantFreq, spectralCentroid) -> SoundClassification.HUMAN_VOICE
            dominantFreq < 500 && decibels > 60 -> SoundClassification.TRAFFIC
            spectralCentroid > 3000 && dominantFreq > 2000 -> SoundClassification.MECHANICAL
            dominantFreq in 500f..2000f -> SoundClassification.AMBIENT
            else -> SoundClassification.UNKNOWN
        }
    }

    private fun detectVoice(dominantFreq: Float, spectralCentroid: Float): Boolean {
        // Human voice typically has fundamental frequency between 80-300 Hz
        // and spectral centroid around 1000-3000 Hz
        return dominantFreq in 80f..300f && spectralCentroid in 1000f..3000f
    }

    private fun categorizeNoiseLevel(decibels: Float): String {
        return when {
            decibels < 30 -> "Very Quiet"
            decibels < 50 -> "Quiet"
            decibels < 70 -> "Moderate"
            decibels < 85 -> "Loud"
            else -> "Very Loud"
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cleanup() {
        stopAudioTelemetry()
        scope.cancel()
    }
}
