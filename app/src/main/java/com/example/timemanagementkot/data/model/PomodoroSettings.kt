package com.example.timemanagementkot.data.model

data class PomodoroSettings(
    var pomodoroEnable: Boolean = false,
    var focusMinutes: Int = 0,
    var breakMinutes: Int = 0
)