package com.example.timemanagementkot.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.content.Context
import androidx.work.Data
import com.example.timemanagementkot.ai.GeminiAnalysisService
import com.example.timemanagementkot.ai.GeminiHelper
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.data.model.TimeAdjustmentSuggestion
import com.example.timemanagementkot.util.DataHelper
import com.example.timemanagementkot.worker.GoalWorkerHelper
import com.google.firebase.Timestamp
import java.util.Date

class HomeVM() : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val authRepo = AuthRepo()

    private lateinit var geminiService: GeminiAnalysisService

    fun initialize(context: Context) {
        if (!::geminiService.isInitialized) {
            geminiService = GeminiAnalysisService(
                GeminiHelper.getGenerativeModel(context)
            )
        }
    }

    private val _activities = MutableStateFlow<List<ActivityWithLogTime>>(emptyList())
    val activities: StateFlow<List<ActivityWithLogTime>> = _activities

    private val _currentDateState = MutableStateFlow("")
    val currentDateState: StateFlow<String> = _currentDateState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCurrentDate = MutableStateFlow(true)
    val isCurrentDate: StateFlow<Boolean> = _isCurrentDate

    private val _currentDateOffset = MutableStateFlow(0)
    val currentDateOffset: StateFlow<Int> = _currentDateOffset

    private val _unanalyzedLogsCount = MutableStateFlow(0)
    val unanalyzedLogsCount: StateFlow<Int> = _unanalyzedLogsCount

    private val _autoAnalysisTriggered = MutableStateFlow(false)
    val autoAnalysisTriggered: StateFlow<Boolean> = _autoAnalysisTriggered

    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions

    private val _suggestions = MutableStateFlow<List<TimeAdjustmentSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TimeAdjustmentSuggestion>> = _suggestions

    private val _selectedSuggestion = MutableStateFlow<TimeAdjustmentSuggestion?>(null)
    val selectedSuggestion: StateFlow<TimeAdjustmentSuggestion?> = _selectedSuggestion

    fun toggleSuggestions() {
        _showSuggestions.value = !_showSuggestions.value
    }

    fun hideSuggestions() {
        _showSuggestions.value = false
    }

    fun selectSuggestion(suggestion: TimeAdjustmentSuggestion) {
        _selectedSuggestion.value = suggestion
    }

    fun clearSelectedSuggestion() {
        _selectedSuggestion.value = null
    }

    fun startAutoAnalysisMonitoring(userId: String) {
        viewModelScope.launch {
            activityRepo.getLogsByUser(userId).collect { logs ->
                val unanalyzedLogs = logs.filter { !it.analyzedByAI }
                _unanalyzedLogsCount.value = unanalyzedLogs.size

                if (unanalyzedLogs.size >= 7 && !_autoAnalysisTriggered.value) {
                    analyzeAndMarkLogs(unanalyzedLogs)
                    _autoAnalysisTriggered.value = true
                } else if (unanalyzedLogs.size < 7) {
                    _autoAnalysisTriggered.value = false
                    loadSuggestions()
                }
            }
        }
    }

    private suspend fun analyzeAndMarkLogs(logs: List<LogTimeModel>) {
        try {
            _isLoading.value = true
            val activityLogs = logs.groupBy { it.activityId }
            val targetActivityId = activityLogs.maxByOrNull { it.value.size }?.key

            targetActivityId?.let { activityId ->
                val activity = activityRepo.getActivityById(activityId) ?: return

                val suggestion = geminiService.analyzeTimeAdjustment(
                    activity,
                    activityLogs[activityId] ?: emptyList()
                )

                val currentTime = Date()
                val timeRequestFormat = SimpleDateFormat("EEE, dd/MM", Locale("vi"))
                val timeRequestStr = timeRequestFormat.format(currentTime)
                val userId = authRepo.getCurrentUser()?.uid ?: return

                val suggestionWithExtra = suggestion.copy(
                    timeRequest = timeRequestStr,
                    status = "pending",
                    sugestionId = "",
                    userId = userId
                )

                activityRepo.saveSuggestions(listOf(suggestionWithExtra))

                logs.forEach { log ->
                    activityRepo.markLogAsAnalyzed(log.logTimeId)
                }

                fetchSuggestions(userId)

            }
        } catch (e: Exception) {
            _errorMessage.value = "Tự động phân tích thất bại: ${e.localizedMessage}"
            Log.e("HomeVM", "Auto analysis error", e)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchSuggestions(userId: String) {
        try {
            _isLoading.value = true
            val suggestions = activityRepo.getSuggestionsList(userId)

            if (suggestions.isNotEmpty()) {
                _suggestions.value = suggestions
                _showSuggestions.value = true
                Log.d("HomeVM", "Fetched ${suggestions.size} suggestions")
            } else {
                _suggestions.value = emptyList()
                _showSuggestions.value = false
                Log.d("HomeVM", "No suggestions found")
            }
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi tải gợi ý: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
        }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUser()?.uid ?: return@launch
            fetchSuggestions(userId)
        }
    }

    fun applySuggestion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val suggestion = _selectedSuggestion.value ?: return@launch
                val activity = _activities.value.find { it.activity.activityId == suggestion.activityId }
                    ?.activity ?: return@launch

                val newStartTime = DataHelper.parseSuggestedTime(activity.startTime, suggestion.suggestedStart)
                val newEndTime = DataHelper.parseSuggestedTime(activity.endTime, suggestion.suggestedEnd)

                activityRepo.updateActivityTime(
                    activityId = activity.activityId,
                    newStartTime = newStartTime,
                    newEndTime = newEndTime
                ).onSuccess {
                    fetchActivitiesForDate(_currentDateOffset.value)
                    activityRepo.updateSuggestionStatus(suggestion.sugestionId, "applied")

                    _showSuggestions.value = false
                    _selectedSuggestion.value = null
                    _errorMessage.value = "Đã áp dụng gợi ý thành công"

                    Log.d("HomeVM", "Updated activity time: ${activity.activityId} " +
                            "to $newStartTime - $newEndTime")
                }.onFailure { e ->
                    _errorMessage.value = "Lỗi khi áp dụng gợi ý: ${e.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi xử lý thời gian: ${e.localizedMessage}"
                Log.e("HomeVM", "Apply suggestion error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSuggestion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val suggestion = _selectedSuggestion.value ?: return@launch

                activityRepo.deleteSuggestion(suggestion.sugestionId).onSuccess {
                    val userId = authRepo.getCurrentUser()?.uid ?: return@onSuccess
                    fetchSuggestions(userId)

                    _selectedSuggestion.value = null
                    _errorMessage.value = "Đã xóa gợi ý thành công"
                }.onFailure { e ->
                    _errorMessage.value = "Lỗi khi xóa gợi ý: ${e.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.localizedMessage}"
                Log.e("HomeVM", "Delete suggestion error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    init {
        fetchActivitiesForDate(0)
    }

    fun fetchActivitiesForDate(offset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentDateOffset.value = offset

            try {
                updateCurrentDate(offset)
                val userId = authRepo.getCurrentUser()?.uid ?: ""
                activityRepo.getActivitiesByDate(userId, offset)
                    .onSuccess { activities ->
                        val sortedActivities = activities.sortedWith(
                            compareBy<ActivityWithLogTime> { it.completeStatus }
                                .thenBy { activity ->
                                    val calendar = Calendar.getInstance().apply {
                                        time = activity.activity.startTime.toDate()
                                    }
                                    calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                            calendar.get(Calendar.MINUTE) * 60 +
                                            calendar.get(Calendar.SECOND)
                                }
                        )
                        _activities.value = sortedActivities
                        Log.d("HomeVM", "Fetched ${activities.size} activities for offset $offset")
                    }
                    .onFailure { e ->
                        _errorMessage.value = "Lỗi tải dữ liệu: ${e.localizedMessage}"
                        Log.e("HomeVM", "Fetch error", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.localizedMessage}"
                Log.e("HomeVM", "Unexpected error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPreviousDay() {
        fetchActivitiesForDate(_currentDateOffset.value - 1)
    }

    fun onNextDay() {
        fetchActivitiesForDate(_currentDateOffset.value + 1)
    }

    private fun updateCurrentDate(offset: Int) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.DATE, offset)
        }

        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        _isCurrentDate.value = (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))

        val dayOfWeek = convertCalendarDayToFullDayFormat(calendar.get(Calendar.DAY_OF_WEEK))
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val dateStr = dateFormat.format(calendar.time)

        _currentDateState.value = "$dayOfWeek, $dateStr"
    }

    private fun convertCalendarDayToFullDayFormat(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Thứ 2"
            Calendar.TUESDAY -> "Thứ 3"
            Calendar.WEDNESDAY -> "Thứ 4"
            Calendar.THURSDAY -> "Thứ 5"
            Calendar.FRIDAY -> "Thứ 6"
            Calendar.SATURDAY -> "Thứ 7"
            Calendar.SUNDAY -> "Chủ nhật"
            else -> ""
        }
    }

    fun checkAndCreateGoalDetailIfNeeded(userId: String, context: Context) {
        GoalWorkerHelper.enqueueGoalWorkerIfNeeded(userId, context)
    }


}

