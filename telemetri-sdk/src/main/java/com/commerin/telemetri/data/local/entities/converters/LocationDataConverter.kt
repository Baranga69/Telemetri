package com.commerin.telemetri.data.local.entities.converters

import androidx.room.TypeConverter
import com.commerin.telemetri.domain.model.LocationData
import com.google.gson.Gson

object LocationDataConverter {
    @TypeConverter
    @JvmStatic
    fun fromLocationData(location: LocationData?): String? {
        return if (location == null) null else Gson().toJson(location)
    }

    @TypeConverter
    @JvmStatic
    fun toLocationData(locationString: String?): LocationData? {
        return if (locationString == null) null else Gson().fromJson(locationString, LocationData::class.java)
    }
}
