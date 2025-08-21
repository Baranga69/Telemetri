package com.commerin.telemetri.data.local.entities.converters

import androidx.room.TypeConverter
import com.commerin.telemetri.domain.model.SensorData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SensorDataListConverter {
    @TypeConverter
    @JvmStatic
    fun fromSensorDataList(list: List<SensorData>?): String? {
        return if (list == null) null else Gson().toJson(list)
    }

    @TypeConverter
    @JvmStatic
    fun toSensorDataList(listString: String?): List<SensorData>? {
        return if (listString == null) null else {
            val type = object : TypeToken<List<SensorData>>() {}.type
            Gson().fromJson(listString, type)
        }
    }
}
