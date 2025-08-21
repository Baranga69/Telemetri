package com.commerin.telemetri.data.sensors


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorDataSource(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getAccelerometerData(): Flow<SensorRaw> = sensorFlow(Sensor.TYPE_ACCELEROMETER)

    private fun sensorFlow(type: Int): Flow<SensorRaw> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(type)
        if (sensor == null) {
            close(IllegalStateException("Sensor not available"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(SensorRaw(type, it.values.clone(), event.timestamp))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}