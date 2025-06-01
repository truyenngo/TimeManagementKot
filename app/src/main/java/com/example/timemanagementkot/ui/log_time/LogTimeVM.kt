package com.example.timemanagementkot.ui.log_time

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import com.example.timemanagementkot.data.model.LogTimeModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.String

class LogTimeVM : ViewModel() {
    private val activityRepo = ActivityRepo()

    private val _currentDateState = MutableStateFlow("")
    val currentDateState: StateFlow<String> = _currentDateState

    private val _elapsedFormatted = MutableStateFlow("00:00:00")
    val elapsedFormatted: StateFlow<String> = _elapsedFormatted

    private var timerJob: Job? = null

    fun startTimer() {
        timerJob?.cancel()
        var elapsed = 0L

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                elapsed += 1
                _elapsedFormatted.value = formatElapsedDuration(elapsed)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun formatElapsedDuration(duration: Long): String {
        val hours = (duration / 3600) % 24
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private val _isFocusPhase = MutableStateFlow(true)
    val isFocusPhase: StateFlow<Boolean> = _isFocusPhase

    // Setup pomodoro clock
    private val _pomoClockState = MutableStateFlow("00:00")
    val pomoClockState: StateFlow<String> = _pomoClockState

    private var pomoTimerJob: Job? = null

    fun startPomodoroCycle(focusMinutes: Int, breakMinutes: Int) {
        pomoTimerJob?.cancel()
        pomoTimerJob = viewModelScope.launch {
            while (true) {
                val isFocus = _isFocusPhase.value
                val duration = if (isFocus) focusMinutes * 60L else breakMinutes * 60L
                var remainingTime = duration

                while (remainingTime > 0) {
                    delay(1000)
                    remainingTime -= 1
                    _pomoClockState.value = formatDuration(remainingTime)
                }
                _isFocusPhase.value = !_isFocusPhase.value
            }
        }
    }

    fun formatDuration(duration: Long): String {
        val minutes = (duration / 60) % 60
        val seconds = duration % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun stopPomodoroTimer() {
        pomoTimerJob?.cancel()
    }

    // Khai bao cac bien can luu len firestore
    private var currentActivityId: String = ""
    private var currentUserId: String = ""

    private val _titleState = MutableStateFlow("")
    val titleState: StateFlow<String> = _titleState

    private val _startTimeState = MutableStateFlow("00:00")
    val startTimeState: StateFlow<String> = _startTimeState

    private val _endTimeState = MutableStateFlow("00:00")
    val endTimeState: StateFlow<String> = _endTimeState

    private val _actualStartState = MutableStateFlow<Timestamp?>(null)
    val actualStartState: StateFlow<Timestamp?> = _actualStartState

    private val _actualEndState = MutableStateFlow<Timestamp?>(null)
    val actualEndState: StateFlow<Timestamp?> = _actualEndState

    private val _durationState = MutableStateFlow<Long>(0L)
    val durationState: StateFlow<Long> = _durationState

    private val _isCompleteState = MutableStateFlow(false)
    val isCompleteState: StateFlow<Boolean> = _isCompleteState

    // Ham lay gia tri actualStart
    fun onStartButtonClick() {
        val currentTimestamp = Timestamp.now()
        _actualStartState.value = currentTimestamp
        Log.d("TAG", "onStartButtonClick: ${_actualStartState.value}")
    }

    // Ham lay gia tri actualEnd
    fun onEndButtonClick() {
        val currentTimestamp = Timestamp.now()
        _actualEndState.value = currentTimestamp
        _durationState.value = calculateDuration()
        _isCompleteState.value = true
        Log.d("TAG", "onEndButtonClick: ${_actualEndState.value}")
        Log.d("TAG", "onEndButtonClick: ${_durationState.value}")
    }

    // Ham tinh duration
    fun calculateDuration(): Long {
        val start = _actualStartState.value
        val end = _actualEndState.value
        return if (start != null && end != null) {
            end.toDate().time - start.toDate().time
        } else {
            0L
        }
    }

    private val _pomodoroEnableState = MutableStateFlow(false)
    val pomodoroEnableState: StateFlow<Boolean> = _pomodoroEnableState

    private val _focusMinutesState = MutableStateFlow(25)
    val focusMinutesState: StateFlow<Int> = _focusMinutesState

    private val _breakMinutesState = MutableStateFlow(5)
    val breakMinutesState: StateFlow<Int> = _breakMinutesState

    fun onPomodoroToggle(enabled: Boolean) {
        _pomodoroEnableState.value = enabled
    }

    fun onFocusMinutesChange(value: Int) {
        _focusMinutesState.value = value
    }

    fun onBreakMinutesChange(value: Int) {
        _breakMinutesState.value = value
    }

    private val _onSaveSuccess = MutableStateFlow<String?>(null)
    val onSaveSuccess: StateFlow<String?> = _onSaveSuccess

    fun resetSaveSuccess() {
        _onSaveSuccess.value = null
    }

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    fun resetError() {
        _errorState.value = null
    }

    fun setLogTimeDefault(activityWithLog: ActivityWithLogTime) {
        val activity = activityWithLog.activity

        currentActivityId = activity.activityId
        currentUserId = activity.userId
        _currentDateState.value = convertDayToFullDayFormat(getCurrentDayOfWeek()) + ", " + getFormattedCurrentDate()
        _titleState.value = activity.title
        _startTimeState.value = formatTimeToHHmm(activity.startTime)
        _endTimeState.value = formatTimeToHHmm(activity.endTime)
        _pomodoroEnableState.value = activity.pomodoroSettings.pomodoroEnable
        _focusMinutesState.value = activity.pomodoroSettings.focusMinutes
        _breakMinutesState.value = activity.pomodoroSettings.breakMinutes

        // Thêm xử lý isComplete nếu cần
        _isCompleteState.value = activityWithLog.completeStatus
    }

    fun saveLogTime(){
        val id = currentActivityId
        val userId = currentUserId
        val startTime = parseTimeToTimestamp(_startTimeState.value)
        val endTime = parseTimeToTimestamp(_endTimeState.value)

        viewModelScope.launch {
            val logTime = LogTimeModel(
                logTimeId = UUID.randomUUID().toString(),
                activityId = id,
                userId = userId,
                startTime = startTime,
                endTime = endTime,
                date = Timestamp.now(),
                dayOfWeek = getCurrentDayOfWeek(),
                actualStart = _actualStartState.value!!,
                actualEnd = _actualEndState.value!!,
                duration = _durationState.value,
                completeStatus = true
            )
            val result = ActivityRepo().saveLogTime(logTime)
            result.onSuccess {
                _onSaveSuccess.value = "Lưu dữ liệu hoạt động thành công!"
                delay(1500)
            }.onFailure {
                _errorState.value = "Lỗi khi lưu dữ liêu hoạt động: ${it.message}"
            }
        }
    }

    fun convertDayToFullDayFormat(shortDay: String): String {
        return when (shortDay) {
            "T2" -> "Thứ 2"
            "T3" -> "Thứ 3"
            "T4" -> "Thứ 4"
            "T5" -> "Thứ 5"
            "T6" -> "Thứ 6"
            "T7" -> "Thứ 7"
            "CN" -> "Chủ nhật"
            else -> shortDay
        }
    }

    fun formatTimeToHHmm(timestamp: Timestamp): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp.seconds * 1000
        }
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun formatTimeStampToHHmmss(timestamp: Timestamp?): String {
        return timestamp?.let {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = it.seconds * 1000
            }
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.format(calendar.time)
        } ?: "Chưa có"
    }

    fun parseTimeToTimestamp(timeStr: String): Timestamp {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val dateTimeStr = "$today $timeStr"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = sdf.parse(dateTimeStr)
        return Timestamp(date!!)
    }


    fun getCurrentDayOfWeek(): String {
        val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        return when (currentDay) {
            java.util.Calendar.MONDAY -> "T2"
            java.util.Calendar.TUESDAY -> "T3"
            java.util.Calendar.WEDNESDAY -> "T4"
            java.util.Calendar.THURSDAY -> "T5"
            java.util.Calendar.FRIDAY -> "T6"
            java.util.Calendar.SATURDAY -> "T7"
            java.util.Calendar.SUNDAY -> "CN"
            else -> "T2"
        }
    }

    fun getFormattedCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)
        return "$formattedDate"
    }
}