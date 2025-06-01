package com.example.timemanagementkot.ui.add_activity

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityType
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.example.timemanagementkot.notifications.AlarmScheduler
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddActivityViewModel() : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val authRepo = AuthRepo()

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

    private val _isPomodoroState = MutableStateFlow(false)
    val isPomodoroState: StateFlow<Boolean> = _isPomodoroState

    private val _focusMinutesState = MutableStateFlow(25)
    val focusMinutesState: StateFlow<Int> = _focusMinutesState

    private val _breakMinutesState = MutableStateFlow(5)
    val breakMinutesState: StateFlow<Int> = _breakMinutesState

    private val _isAlarmState = MutableStateFlow(false)
    val isAlarmState: StateFlow<Boolean> = _isAlarmState

    private val _selectedRingtoneState = MutableStateFlow("Default")
    val selectedRingtoneState: StateFlow<String> = _selectedRingtoneState

    private val _ringtoneVolumeState = MutableStateFlow(0.5f)
    val ringtoneVolumeState: StateFlow<Float> = _ringtoneVolumeState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private val _onSaveSuccess = MutableStateFlow<String?>(null)
    val onSaveSuccess: StateFlow<String?> = _onSaveSuccess

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
        _isPomodoroState.value = enabled
    }

    fun onFocusMinutesChange(value: Int) {
        _focusMinutesState.value = value
    }

    fun onBreakMinutesChange(value: Int) {
        _breakMinutesState.value = value
    }

    fun onAlarmToggle(enabled: Boolean) {
        _isAlarmState.value = enabled
    }

    fun onSelectedRingtoneChange(title: String) {
        _selectedRingtoneState.value = title
    }

    fun onRingtoneVolumeChange(volume: Float) {
        _ringtoneVolumeState.value = volume
    }

    fun resetSaveSuccess() {
        _onSaveSuccess.value = null
    }

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

        if (_isPomodoroState.value) {
            if (_focusMinutesState.value <= 0 || _breakMinutesState.value <= 0) {
                _errorState.value = "Thời gian Pomodoro phải lớn hơn 0"
                return false
            }
        }

        if (_isAlarmState.value) {
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

    fun addActivity(context: Context) {
        val userId = authRepo.getCurrentUser()?.uid ?: ""

        if (userId.isBlank()) {
            Log.e("AddActivityVM", "Không có người dùng đăng nhập!")
            _errorState.value = "Vui lòng đăng nhập lại"
            return
        }

        if (!validateInputs()) return

        val startTime = parseTimeToTimestamp(_startTimeState.value)
        val endTime = parseTimeToTimestamp(_endTimeState.value)

        viewModelScope.launch {
            activityRepo.checkTitleConflict(userId, _titleState.value)?.let {
                _errorState.value = it
                return@launch
            }

            ActivityRepo().checkTimeConflict(
                userId,
                startTime,
                endTime,
                _selectedDaysState.value
            )?.let {
                _errorState.value = it
                return@launch
            }

            val activity = createActivityModel(userId)

            activityRepo.addActivity(activity).onSuccess {
                _onSaveSuccess.value = "Thêm hoạt động thành công!"

                if (_selectedDaysState.value.isNotEmpty()) {
                    try {
                        AlarmScheduler(context).schedule(activity)
                        Log.d("AddActivityVM", "Đã lên lịch thông báo")
                    } catch (e: Exception) {
                        Log.e("AddActivityVM", "Lỗi lên lịch thông báo", e)
                    }
                }
            }.onFailure {
                _errorState.value = "Lỗi khi thêm hoạt động: ${it.message}"
            }
        }
    }

    private fun createActivityModel(userId: String): ActivityModel {
        return ActivityModel(
            activityId = UUID.randomUUID().toString(),
            userId = userId,
            title = _titleState.value,
            type = if (_selectedTypeState.value?.typeName == "Chưa xếp loại") "Khác" else _selectedTypeState.value?.typeName ?: "Khác",
            startTime = parseTimeToTimestamp(_startTimeState.value),
            endTime = parseTimeToTimestamp(_endTimeState.value),
            repeatDays = _selectedDaysState.value,
            pomodoroSettings = PomodoroSettings(
                pomodoroEnable = _isPomodoroState.value,
                focusMinutes = _focusMinutesState.value,
                breakMinutes = _breakMinutesState.value
            )
        )
    }

    fun parseTimeToTimestamp(timeStr: String): Timestamp {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val dateTimeStr = "$today $timeStr"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = sdf.parse(dateTimeStr)
        return Timestamp(date!!)
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
}
