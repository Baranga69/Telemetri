package com.commerin.telemetri.data.local.entities.converters

import androidx.room.TypeConverter
import com.commerin.telemetri.domain.model.MotionData
import com.google.gson.Gson

object MotionDataConverter {
    @TypeConverter
    @JvmStatic
    fun fromMotionData(motion: MotionData?): String? {
        return if (motion == null) null else Gson().toJson(motion)
    }

    @TypeConverter
    @JvmStatic
    fun toMotionData(motionString: String?): MotionData? {
        return if (motionString == null) null else Gson().fromJson(motionString, MotionData::class.java)
    }
}
