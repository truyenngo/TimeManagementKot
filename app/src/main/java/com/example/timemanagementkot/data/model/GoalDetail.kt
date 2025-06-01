package com.example.timemanagementkot.data.model

import com.google.firebase.Timestamp
import com.google.gson.Gson

data class GoalDetail(
    var goalDetailId: String,
    var goalId: String,
    var targetDuration: Long,
    var currentDuration: Long,
    var startDate: Timestamp,
    var endDate: Timestamp,
    var completeStatus: Boolean
) {
    constructor() : this("", "", 0, 0, Timestamp.now(), Timestamp.now(), false)

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): GoalDetail {
            return Gson().fromJson(json, GoalDetail::class.java)
        }
    }
}
