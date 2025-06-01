package com.example.timemanagementkot.ui.detail_activity

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityType
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.notifications.AlarmScheduler
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.*

class DetailActivityVM : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val authRepo = AuthRepo()

    private var currentActivityId: String = ""

    private var currentUserId: String = ""

    private val _titleState = MutableStateFlow("")
    val titleState: StateFlow<String> = _titleState

    private val _startTimeState = MutableStateFlow("00:00")
    val startTimeState: StateFlow<String> = _startTimeState

    private val _endTimeState = MutableStateFlow("00:00")
    val endTimeState: StateFlow<String> = _endTimeState

    private val _activityTypes = MutableStateFlow<List<ActivityType>>(emptyList())
    val activityTypes: StateFlow<List<ActivityType>> = _activityTypes

    private val _defaultType = ActivityType(typeName = "Chưa xếp loại")

    private val _selectedTypeState = MutableStateFlow<ActivityType?>(null)
    val selectedTypeState: StateFlow<ActivityType?> = _selectedTypeState

    private val _showAddTypeDialog = MutableStateFlow(false)
    val showAddTypeDialog: StateFlow<Boolean> = _showAddTypeDialog

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog

    private val _typeToDelete = MutableStateFlow<ActivityType?>(null)
    val typeToDelete: StateFlow<ActivityType?> = _typeToDelete

    private val _typeExpandedState = MutableStateFlow(false)
    val typeExpandedState: StateFlow<Boolean> = _typeExpandedState

    val repeatDays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    private val _selectedDaysState = MutableStateFlow(mutableListOf<String>())
    val selectedDaysState: StateFlow<List<String>> = _selectedDaysState

    private val _pomodoroEnableState = MutableStateFlow(false)
    val pomodoroEnableState: StateFlow<Boolean> = _pomodoroEnableState

    private val _focusMinutesState = MutableStateFlow(25)
    val focusMinutesState: StateFlow<Int> = _focusMinutesState

    private val _breakMinutesState = MutableStateFlow(5)
    val breakMinutesState: StateFlow<Int> = _breakMinutesState

    private val _alarmEnableState = MutableStateFlow(false)
    val alarmEnableState: StateFlow<Boolean> = _alarmEnableState

    private val _selectedRingtoneState = MutableStateFlow("Default")
    val selectedRingtoneState: StateFlow<String> = _selectedRingtoneState

    private val _ringtoneVolumeState = MutableStateFlow(0.5f)
    val ringtoneVolumeState: StateFlow<Float> = _ringtoneVolumeState

    private val _isEditing = MutableStateFlow(false)
    val isEditingState: StateFlow<Boolean> = _isEditing

    fun onTitleChange(newTitle: String) {
        _titleState.value = newTitle
    }

    fun onStartTimeChange(newTime: String) {
        _startTimeState.value = newTime
    }

    fun onEndTimeChange(newTime: String) {
        _endTimeState.value = newTime
    }

    fun onShowAddTypeDialogChange(show: Boolean) {
        _showAddTypeDialog.value = show
    }

    fun onShowDeleteConfirmDialogChange(show: Boolean) {
        _showDeleteConfirmDialog.value = show
    }

    fun prepareToDeleteType(type: ActivityType) {
        _typeToDelete.value = type
        _showDeleteConfirmDialog.value = true
    }

    fun onSelectedTypeChange(type: ActivityType) {
        _selectedTypeState.value = type
    }

    fun onTypeExpandedChange(expanded: Boolean) {
        _typeExpandedState.value = expanded
    }

    fun toggleDaySelection(day: String) {
        val current = _selectedDaysState.value.toMutableList()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _selectedDaysState.value = current
    }

    fun onPomodoroToggle(enabled: Boolean) {
        _pomodoroEnableState.value = enabled
    }

    fun onFocusMinutesChange(value: Int) {
        _focusMinutesState.value = value
    }

    fun onBreakMinutesChange(value: Int) {
        _breakMinutesState.value = value
    }

    fun onAlarmToggle(enabled: Boolean) {
        _alarmEnableState.value = enabled
    }

    fun onSelectedRingtoneChange(title: String) {
        _selectedRingtoneState.value = title
    }

    fun onRingtoneVolumeChange(volume: Float) {
        _ringtoneVolumeState.value = volume
    }

    fun onIsEditChange(newValue: Boolean) {
        _isEditing.value = newValue
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

    private fun validateInputs(): Boolean {
        if (_titleState.value.trim().isEmpty()) {
            _errorState.value = "Vui lòng nhập tiêu đề hoạt động"
            return false
        }

        val start = parseTimeToTimestamp(_startTimeState.value)
        val end = parseTimeToTimestamp(_endTimeState.value)
        if (end.seconds <= start.seconds) {
            _errorState.value = "Thời gian kết thúc phải sau thời gian bắt đầu"
            return false
        }

        if (_pomodoroEnableState.value) {
            if (_focusMinutesState.value <= 0 || _breakMinutesState.value <= 0) {
                _errorState.value = "Thời gian Pomodoro phải lớn hơn 0"
                return false
            }
        }

        if (_alarmEnableState.value) {
            if (_selectedRingtoneState.value.isBlank() || _selectedRingtoneState.value == "Default" || _ringtoneVolumeState.value !in 0f..1f) {
                _errorState.value = "Vui lòng chọn âm báo hợp lệ"
                return false
            }
        }

        if (_selectedDaysState.value.isEmpty()) {
            _errorState.value = "Vui lòng chọn ít nhất một ngày lặp lại"
            return false
        }

        resetError()
        return true
    }

    fun setActivityDetails(activity: ActivityModel) {
        currentActivityId = activity.activityId
        currentUserId = activity.userId
        _titleState.value = activity.title
        _startTimeState.value = formatTimeToHHmm(activity.startTime)
        _endTimeState.value = formatTimeToHHmm(activity.endTime)
        _selectedTypeState.value = _activityTypes.value.find { it.typeName == activity.type }
            ?: ActivityType(typeName = activity.type)
        _selectedDaysState.value = activity.repeatDays.toMutableList()
        _pomodoroEnableState.value = activity.pomodoroSettings.pomodoroEnable
        _focusMinutesState.value = activity.pomodoroSettings.focusMinutes
        _breakMinutesState.value = activity.pomodoroSettings.breakMinutes
        _isEditing.value = false
    }

    fun updateCurrentActivity(context: Context) {
        if (!validateInputs()) return

        val startTime = parseTimeToTimestamp(_startTimeState.value)
        val endTime = parseTimeToTimestamp(_endTimeState.value)

        viewModelScope.launch {
            ActivityRepo().checkTitleConflict(currentUserId, _titleState.value, currentActivityId)?.let {
                _errorState.value = it
                return@launch
            }

            ActivityRepo().checkTimeConflict(currentUserId, startTime, endTime, _selectedDaysState.value, currentActivityId)?.let {
                _errorState.value = it
                return@launch
            }

            val updatedActivity = createUpdatedActivityModel()

            AlarmScheduler(context).cancelAllForActivity(currentActivityId)

            activityRepo.updateActivityById(currentActivityId, updatedActivity).onSuccess {
                activityRepo.updateGoalTitlesWhenActivityRenamed(currentActivityId, _titleState.value)

                if (updatedActivity.repeatDays.isNotEmpty()) {
                    try {
                        AlarmScheduler(context).apply {
                            schedule(updatedActivity)
                            Log.d("DetailVM", "Đã lên lịch lại thông báo cho activity ${updatedActivity.activityId}")
                        }
                    } catch (e: Exception) {
                        Log.e("DetailVM", "Lỗi lên lịch thông báo", e)
                    }
                } else {
                    Log.d("DetailVM", "Activity không có repeatDays, không lên lịch thông báo")
                }

                _onSaveSuccess.value = "Cập nhật hoạt động thành công!"
                _isEditing.value = false

            }.onFailure {
                _errorState.value = "Lỗi khi cập nhật: ${it.message}"
            }
        }
    }

    private fun createUpdatedActivityModel(): ActivityModel {
        return ActivityModel(
            activityId = currentActivityId,
            userId = currentUserId,
            title = _titleState.value,
            type = if (_selectedTypeState.value?.typeName == "Chưa xếp loại") "Khác" else _selectedTypeState.value?.typeName ?: "Khác",
            startTime = parseTimeToTimestamp(_startTimeState.value),
            endTime = parseTimeToTimestamp(_endTimeState.value),
            repeatDays = _selectedDaysState.value.toList(),
            pomodoroSettings = PomodoroSettings(
                pomodoroEnable = _pomodoroEnableState.value,
                focusMinutes = _focusMinutesState.value,
                breakMinutes = _breakMinutesState.value
            )
        )
    }

    init {
        loadActivityTypes()
    }

    private fun loadActivityTypes() {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUser()?.uid ?: return@launch
            _activityTypes.value = activityRepo.getUserActivityTypes(userId)
            _selectedTypeState.value = _activityTypes.value.firstOrNull() ?: _defaultType
        }
    }

    fun addNewActivityType(typeName: String) {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUser()?.uid ?: return@launch

            if (_activityTypes.value.any { it.typeName.equals(typeName, ignoreCase = true) }) {
                _errorState.value = "Tên loại hoạt động đã tồn tại"
                return@launch
            }

            if (typeName.isBlank()) {
                _errorState.value = "Tên loại không được để trống"
                return@launch
            }

            try {
                val result = activityRepo.addActivityType(userId, typeName)
                result.onSuccess {
                    loadActivityTypes()
                    _showAddTypeDialog.value = false
                    _errorState.value = null

                    if (_selectedTypeState.value?.typeName == "Chưa xếp loại") {
                        _selectedTypeState.value = _activityTypes.value.lastOrNull()
                    }
                }.onFailure {
                    _errorState.value = "Lỗi khi thêm loại: ${it.message}"
                }
            } catch (e: Exception) {
                _errorState.value = "Lỗi hệ thống: ${e.message}"
            }
        }
    }

    fun deleteActivityType(type: ActivityType) {
        viewModelScope.launch {
            try {
                val result = activityRepo.deleteActivityType(type.typeId)
                result.onSuccess {
                    _activityTypes.value = _activityTypes.value - type
                    if (_selectedTypeState.value?.typeId == type.typeId) {
                        _selectedTypeState.value = _activityTypes.value.firstOrNull() ?: ActivityType(typeName = "Chưa xếp loại")
                    }
                    _showDeleteConfirmDialog.value = false
                }.onFailure {
                    _errorState.value = "Lỗi khi xóa loại: ${it.message}"
                }
            } catch (e: Exception) {
                _errorState.value = "Lỗi khi xóa loại: ${e.message}"
            }
        }
    }

    fun formatTimeToHHmm(timestamp: Timestamp): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp.seconds * 1000
        }
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun parseTimeToTimestamp(timeStr: String): Timestamp {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val dateTimeStr = "$today $timeStr"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = sdf.parse(dateTimeStr)
        return Timestamp(date!!)
    }
}
