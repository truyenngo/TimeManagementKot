package com.example.timemanagementkot.data.model

import com.google.firebase.Timestamp

data class StatsModel(
    val statId: String = "",
    val activityId: String = "",
    val userId: String = "",
    val type: String = "week",
    val periodLabel: String = "",
    val periodStart: Timestamp = Timestamp.now(),
    val periodEnd: Timestamp = Timestamp.now(),
    val totalDuration: Long = 0L,
    val completedCount: Int = 0,
    val missedCount: Int = 0,
    val pendingCount: Int = 0,
    val status: String = "pending"
) {
    constructor() : this(
        statId = "",
        activityId = "",
        userId = "",
        type = "week",
        periodLabel = "",
        periodStart = Timestamp.now(),
        periodEnd = Timestamp.now(),
        totalDuration = 0L,
        completedCount = 0,
        missedCount = 0,
        pendingCount = 0,
        status = "pending"
    )
}
