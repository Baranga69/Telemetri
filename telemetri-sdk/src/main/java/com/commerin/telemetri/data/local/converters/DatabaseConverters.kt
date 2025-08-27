package com.commerin.telemetri.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room database type converters for complex data types
 */
class DatabaseConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String?): List<Float>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<Float>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        return if (value == null) null else {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(value, mapType)
        }
    }
}
