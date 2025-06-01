package com.example.timemanagementkot.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.notifications.AlarmScheduler
import com.example.timemanagementkot.notifications.NotificationHelper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ActivityNotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Received notification intent")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Parse activity data
                val activityJson = intent?.getStringExtra("activity_data") ?: run {
                    Log.w(TAG, "Intent missing activity_data")
                    return@launch
                }

                val activity = Gson().fromJson(activityJson, ActivityModel::class.java)
                Log.d(TAG, "Processing activity: ${activity.title} (${activity.activityId})")

                // 2. Verify activity exists
                val repo = ActivityRepo()
                if (!repo.isActivityExist(activity.activityId)) {
                    Log.d(TAG, "Activity was deleted, canceling notifications")
                    AlarmScheduler(context).cancelAllForActivity(activity.activityId)
                    return@launch
                }

                // 3. Show notification
                NotificationHelper(context).showActivityNotification(activity)

                // 4. Reschedule if needed
                if (activity.repeatDays.isNotEmpty()) {
                    Log.d(TAG, "Rescheduling recurring activity")
                    AlarmScheduler(context).schedule(activity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }
}