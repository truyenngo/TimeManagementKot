package com.example.timemanagementkot.data.model

import com.google.gson.Gson

data class GoalWithDetail(
    val header: GoalHeader,
    val detail: GoalDetail
){
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): GoalWithDetail {
            return Gson().fromJson(json, GoalWithDetail::class.java)
        }
    }
}
