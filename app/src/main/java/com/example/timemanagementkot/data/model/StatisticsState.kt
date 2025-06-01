package com.example.timemanagementkot.data.model

data class StatisticsState(
    val totalActivities: Int = 0, // Tổng số hoạt động
    val totalDuration: Long = 0,  // Tổng thời gian (milliseconds)
    val peakHour: Int = -1,       // Giờ cao điểm, -1 nếu không có
    val durationByType: List<ActivityStats> = emptyList() // Thống kê theo loại hoạt động
)

data class ActivityStats(
    val type: String,             // Loại hoạt động (e.g., "Học", "Làm việc")
    val totalDuration: Long      // Tổng thời gian cho loại hoạt động này (milliseconds)
)
