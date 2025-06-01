package com.example.timemanagementkot.data.model

import com.google.gson.Gson

data class ActivityWithLogTime(
    val activity: ActivityModel,
    val completeStatus: Boolean
){
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): ActivityWithLogTime {
            return Gson().fromJson(json, ActivityWithLogTime::class.java)
        }
    }
}
