package com.example.timemanagementkot.data.model

import java.util.Date

data class ActivityType(
    val typeId: String = "",
    val userId: String = "",
    val typeName: String = "",
    val createdAt: Date = Date()
) {
    constructor() : this("", "", "", Date())
}