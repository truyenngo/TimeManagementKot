package com.example.timemanagementkot.ui.list_activity

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.repo.GoalRepo
import com.example.timemanagementkot.notifications.AlarmScheduler
import com.example.timemanagementkot.notifications.receiver.ActivityNotificationReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListActivityVM : ViewModel() {
    private val authRepo = AuthRepo()
    private val activityRepo = ActivityRepo()
    private val goalRepo = GoalRepo()

    private val _activities = MutableStateFlow<List<ActivityModel>>(emptyList())
    val activities: StateFlow<List<ActivityModel>> get() = _activities

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    init {
        fetchActivities()
    }

    fun fetchActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = authRepo.getCurrentUserId()
            if (userId != null) {
                val result = activityRepo.getActivitiesByUserId(userId)
                if (result.isSuccess) {
                    val sortedActivities = result.getOrDefault(emptyList())
                        .sortedBy { it.startTime }
                    _activities.value = sortedActivities
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                }
            } else {
                _errorMessage.value = "Không tìm thấy người dùng"
            }
            _isLoading.value = false
        }
    }

    fun deleteActivity(activityId: String, context: Context) {
        viewModelScope.launch {
            AlarmScheduler(context).cancelAllForActivity(activityId)

            activityRepo.deleteActivityById(activityId).onSuccess {
                goalRepo.deleteActivityWithRelatedGoals(activityId)
                _activities.value = _activities.value.filterNot { it.activityId == activityId }
                sendCleanupBroadcast(context, activityId)
            }
        }
    }

    private fun sendCleanupBroadcast(context: Context, activityId: String) {
        val cleanupIntent = Intent(context, ActivityNotificationReceiver::class.java).apply {
            action = "ACTION_CLEANUP_$activityId"
            putExtra("cleanup", true)
        }
        context.sendBroadcast(cleanupIntent)
    }
}
