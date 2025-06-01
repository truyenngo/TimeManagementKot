package com.example.timemanagementkot.data.model

import com.google.firebase.Timestamp

data class LogTimeModel(
    val logTimeId: String,
    val activityId: String,
    val userId: String,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val date: Timestamp,
    val dayOfWeek: String,
    val actualStart: Timestamp,
    val actualEnd: Timestamp,
    val duration: Long,
    val completeStatus: Boolean,
    val analyzedByAI: Boolean = false
){
    constructor() : this("", "", "", Timestamp.now(), Timestamp.now(), Timestamp.now(), "", Timestamp.now(), Timestamp.now(), 0, false, false)
}
