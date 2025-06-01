package com.example.timemanagementkot.data.model

import com.google.gson.Gson

data class GoalHeader(
    var goalId: String,
    var userId: String,
    var goalTitle: String,
    var activityId: String,
    var goalType: String
) {
    constructor() : this("", "", "", "", "weekly")

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): GoalHeader {
            return Gson().fromJson(json, GoalHeader::class.java)
        }
    }
}
