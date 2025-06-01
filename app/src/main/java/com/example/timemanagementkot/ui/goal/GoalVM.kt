package com.example.timemanagementkot.ui.goal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.GoalHeader
import com.example.timemanagementkot.data.model.GoalWithDetail
import com.example.timemanagementkot.data.repo.GoalRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GoalVM : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val authRepo = AuthRepo()
    private val goalRepo = GoalRepo()

    private val _goalHeaderWithDetail = MutableStateFlow<List<GoalWithDetail>>(emptyList())
    val goalHeaderWithDetail: StateFlow<List<GoalWithDetail>> = _goalHeaderWithDetail

    private val _activities = MutableStateFlow<List<ActivityModel>>(emptyList())
    val activities: StateFlow<List<ActivityModel>> get() = _activities

    private val _goalType = MutableStateFlow("weekly")
    val goalType: StateFlow<String> get() = _goalType

    private val _showActivityDialog = MutableStateFlow(false)
    val showActivityDialog: StateFlow<Boolean> get() = _showActivityDialog

    private val _selectedActivity = MutableStateFlow<ActivityModel?>(null)
    val selectedActivity: StateFlow<ActivityModel?> get() = _selectedActivity

    private val _targetHour = MutableStateFlow(0)
    val targetHour: StateFlow<Int> = _targetHour

    private val _targetMinute = MutableStateFlow(0)
    val targetMinute: StateFlow<Int> = _targetMinute

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _errorTime = MutableStateFlow<String?>(null)
    val errorTime: StateFlow<String?> get() = _errorTime

    private val _createGoalSuccess = MutableStateFlow(false)
    val createGoalSuccess: StateFlow<Boolean> get() = _createGoalSuccess


    fun setGoalType(type: String) {
        _goalType.value = type
        loadGoalHeaders()
        loadActivitiesWithoutGoalHeader()
    }

    fun showDialog() {
        _errorTime.value = null
        _showActivityDialog.value = true
    }

    fun dismissDialog() {
        _showActivityDialog.value = false
    }

    fun selectActivity(activity: ActivityModel) {
        _selectedActivity.value = activity
    }

    fun loadGoalHeaders() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userId = authRepo.getCurrentUserId()
                if (userId != null) {
                    goalRepo.getCurrentGoalHeaderWithDetailsAndUpdate(userId, _goalType.value)
                        .onSuccess { listGoalWithDetail ->
                            _goalHeaderWithDetail.value = listGoalWithDetail.sortedBy { it.header.goalTitle }
                            Log.d("GoalVM", "Loaded ${listGoalWithDetail.size} goal headers")
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Lỗi khi tải mục tiêu"
                            Log.e("GoalVM", "Error loading goal headers", e)
                        }
                } else {
                    _errorMessage.value = "Vui lòng đăng nhập để xem mục tiêu"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.message}"
                Log.e("GoalVM", "System error loading goal headers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadActivitiesWithoutGoalHeader() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userId = authRepo.getCurrentUserId()
                if (userId != null) {
                    activityRepo.getActivitiesWithoutGoalHeader(userId, _goalType.value)
                        .onSuccess { activities ->
                            _activities.value = activities.sortedBy { it.startTime }
                            Log.d("GoalVM", "Loaded ${activities.size} activities without goal headers")
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Lỗi khi tải hoạt động chưa có goal header"
                            Log.e("GoalVM", "Error loading activities without goal headers", e)
                        }
                } else {
                    _errorMessage.value = "Vui lòng đăng nhập để xem hoạt động"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.message}"
                Log.e("GoalVM", "System error loading activities without goal headers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGoalHeaderWithDetail() {
        _errorTime.value = null
        val targetHour = _targetHour.value
        val targetMinute = _targetMinute.value

        var hasError = false

        if (targetHour == 0 && targetMinute == 0) {
            _errorTime.value = "Vui lòng nhập thời lượng mục tiêu"
            Log.e("GoalVM", "Error: ${_errorTime.value}")
            hasError = true
        }

        else if (targetHour < 0 || targetMinute < 0) {
            _errorTime.value = "Thời gian không được âm"
            hasError = true
        }

        else if (targetMinute >= 60) {
            _errorTime.value = "Phút phải nhỏ hơn 60"
            hasError = true
        }

        if (hasError) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val currentActivity = _selectedActivity.value
                val userId = authRepo.getCurrentUserId()

                if (currentActivity == null) {
                    _errorMessage.value = "Vui lòng chọn hoạt động"
                    return@launch
                }
                if (userId == null) {
                    _errorMessage.value = "Người dùng chưa đăng nhập"
                    return@launch
                }

                val newGoalHeader = GoalHeader(
                    goalId = "",
                    userId = userId,
                    goalTitle = currentActivity.title,
                    activityId = currentActivity.activityId,
                    goalType = _goalType.value
                )

                val headerResult = goalRepo.addGoalHeader(newGoalHeader)

                headerResult.onSuccess { createdHeader ->
                    Log.e("GoalVM", "Tạo GoalHeader thành công: ${createdHeader.goalId}")

                    activityRepo.getLogTimesForActivityInPeriod(
                        currentActivity.activityId,
                        userId,
                        _goalType.value
                    ).onSuccess { logTimes ->
                        val targetDurationMs = (_targetHour.value * 60L + _targetMinute.value) * 60_000L
                        val detailSuccess = goalRepo.createGoalDetail(createdHeader, logTimes, targetDurationMs, _goalType.value)

                        if (detailSuccess) {
                            Log.d("GoalVM", "Tạo GoalDetail thành công")
                            _createGoalSuccess.value = true
                            dismissDialog()
                            loadGoalHeaders()
                            loadActivitiesWithoutGoalHeader()
                            _showActivityDialog.value = false
                            _selectedActivity.value = null
                        } else {
                            _createGoalSuccess.value = false
                            _errorMessage.value = "Lỗi khi tạo chi tiết mục tiêu"
                        }
                    }.onFailure { e ->
                        _errorMessage.value = "Lỗi khi lấy dữ liệu logTimes: ${e.message}"
                        Log.e("GoalVM", "Lỗi logTimes", e)
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Lỗi khi tạo mục tiêu: ${e.message}"
                    Log.e("GoalVM", "Lỗi tạo GoalHeader", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.message}"
                Log.e("GoalVM", "Lỗi không xác định", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = goalRepo.deleteGoalWithDetails(goalId)

            result.onSuccess {
                loadGoalHeaders()
                loadActivitiesWithoutGoalHeader()
            }.onFailure { e ->
                _errorMessage.value = "Xóa mục tiêu thất bại: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    init {
        loadGoalHeaders()
        loadActivitiesWithoutGoalHeader()
    }

    fun onTargetHourChanged(newHour: Int) {
        _targetHour.value = newHour
    }

    fun onTargetMinuteChanged(newMinute: Int) {
        _targetMinute.value = newMinute
    }

    fun resetCreateGoalStatus() {
        _createGoalSuccess.value = false
    }
}