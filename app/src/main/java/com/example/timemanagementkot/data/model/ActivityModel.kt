package com.example.timemanagementkot.data.model

import com.google.firebase.Timestamp
import com.google.gson.Gson

data class ActivityModel(
    val activityId: String,
    val userId: String,
    var title: String,
    var type: String,
    var startTime: Timestamp,
    var endTime: Timestamp,
    var repeatDays: List<String> = emptyList(),
    var pomodoroSettings: PomodoroSettings
) {
    constructor() : this(
        "", "", "", "", Timestamp.now(), Timestamp.now(),
        emptyList(), PomodoroSettings()
    )

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): ActivityModel {
            return Gson().fromJson(json, ActivityModel::class.java)
        }
    }
}