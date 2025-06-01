package com.example.timemanagementkot.ui.detail_goal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.model.GoalWithDetail
import com.example.timemanagementkot.data.repo.GoalRepo
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DetailGoalVM : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val goalRepo = GoalRepo()

    private val _goalId = MutableStateFlow("")
    private val _userId = MutableStateFlow("")
    private val _activityId = MutableStateFlow("")

    private val _goalDetailId = MutableStateFlow("")

    private val _targetHour = MutableStateFlow(0)
    val targetHour: StateFlow<Int> = _targetHour

    private val _targetMinute = MutableStateFlow(0)
    val targetMinute: StateFlow<Int> = _targetMinute

    private val _currentHour = MutableStateFlow(0)
    val currentHour: StateFlow<Int> = _currentHour

    private val _currentMinute = MutableStateFlow(0)
    val currentMinute: StateFlow<Int> = _currentMinute

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration

    private val _progressPercentage = MutableStateFlow(0f)
    val progressPercentage: StateFlow<Float> = _progressPercentage

    private val _goalTitle = MutableStateFlow("")
    val activityTitle: StateFlow<String> = _goalTitle

    private val _startDate = MutableStateFlow("30/04/25")
    val startDate: StateFlow<String> = _startDate

    private val _endDate = MutableStateFlow("30/04/25")
    val endDate: StateFlow<String> = _endDate

    private val _goalType = MutableStateFlow("weekly")
    val goalType: StateFlow<String> = _goalType

    private val _completeStatus = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _completeStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _errorTime = MutableStateFlow<String?>(null)
    val errorTime: StateFlow<String?> get() = _errorTime

    fun setGoalValue(goal: GoalWithDetail) {
        val (tgHour, tgMinute) = convertLongToHourMinute(goal.detail.targetDuration)
        _targetHour.value = tgHour
        _targetMinute.value = tgMinute

        val (crHour, crMinute) = convertLongToHourMinute(goal.detail.currentDuration)
        _currentHour.value = crHour
        _currentMinute.value = crMinute

        _goalId.value = goal.header.goalId
        _userId.value = goal.header.userId
        _activityId.value = goal.header.activityId
        _goalTitle.value = goal.header.goalTitle
        _goalType.value = goal.header.goalType

        _goalDetailId.value = goal.detail.goalDetailId
        _startDate.value = formatDate(goal.detail.startDate)
        _endDate.value = formatDate(goal.detail.endDate)
        _currentDuration.value = goal.detail.currentDuration
        _progressPercentage.value = calculateProgress(goal.detail.targetDuration, goal.detail.currentDuration)
        _completeStatus.value = goal.detail.completeStatus
    }

    fun onTargetHourChanged(newHour: Int) {
        _targetHour.value = newHour
    }

    fun onTargetMinuteChanged(newMinute: Int) {
        _targetMinute.value = newMinute
    }

    fun confirmTargetDuration() {
        _errorTime.value = null
        val targetHour = _targetHour.value
        val targetMinute = _targetMinute.value

        // Validate input
        var hasError = false

        // Kiểm tra cả hai giá trị đều bằng 0
        if (targetHour == 0 && targetMinute == 0) {
            _errorTime.value = "Vui lòng nhập thời lượng mục tiêu"
            Log.e("GoalVM", "Error: ${_errorTime.value}")
            hasError = true
        }

        // Kiểm tra số âm
        else if (targetHour < 0 || targetMinute < 0) {
            _errorTime.value = "Thời gian không được âm"
            hasError = true
        }

        // Kiểm tra phút hợp lệ
        else if (targetMinute >= 60) {
            _errorTime.value = "Phút phải nhỏ hơn 60"
            hasError = true
        }

        if (hasError) {
            return // Dừng lại nếu có lỗi, không gọi tiếp
        }

        viewModelScope.launch {
            val goalDetailId = _goalDetailId.value

            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false

            val targetDuration = (_targetHour.value * 60L + _targetMinute.value) * 60_000L
            val result = goalRepo.updateGoalDetail(goalDetailId, targetDuration)

            result.onSuccess { success ->
                if (success) {
                    _saveSuccess.value = true
                    Log.e("DetailGoalVm", "Lưu thành công")

                    val (tgHour, tgMinute) = convertLongToHourMinute(targetDuration)
                    _targetHour.value = tgHour
                    _targetMinute.value = tgMinute

                    val (crHour, crMinute) = convertLongToHourMinute(_currentDuration.value)
                    _currentHour.value = crHour
                    _currentMinute.value = crMinute

                    _progressPercentage.value = calculateProgress(targetDuration, _currentDuration.value)
                } else {
                    _errorMessage.value = "Cập nhật thất bại"
                }
            }.onFailure { e ->
                _errorMessage.value = "Lỗi: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    private fun calculateProgress(targetDurationMs: Long, currentDurationMs: Long): Float {
        if (targetDurationMs == 0L) return 0f
        val progress = currentDurationMs.toFloat() / targetDurationMs
        return progress.coerceIn(0f, 1f) * 100f
    }

    fun formatDate(timestamp: Timestamp): String {
        return SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            .format(timestamp.toDate())
    }

    fun convertLongToHourMinute(durationInMillis: Long): Pair<Int, Int> {
        val totalMinutes = durationInMillis / 60_000L
        val hours = (totalMinutes / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return Pair(hours, minutes)
    }
}