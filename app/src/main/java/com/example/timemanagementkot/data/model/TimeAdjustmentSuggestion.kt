package com.example.timemanagementkot.data.model

data class TimeAdjustmentSuggestion(
    val sugestionId: String = "",
    val userId: String = "",
    val activityId: String,
    val activityTitle: String,
    val currentStart: String,
    val currentEnd: String,
    val suggestedStart: String,
    val suggestedEnd: String,
    val reason: String,
    val timeRequest: String = "",
    val status: String = "pending"
){
    constructor() : this(
        sugestionId = "",
        userId = "",
        activityId = "",
        activityTitle = "",
        currentStart = "",
        currentEnd = "",
        suggestedStart = "",
        suggestedEnd = "",
        reason = "",
        timeRequest = "",
        status = "pending"
    )
}
