package com.example.timemanagementkot.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.util.Log
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.notifications.receiver.ActivityNotificationReceiver
import com.google.gson.Gson
import com.google.firebase.Timestamp
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    companion object {
        private const val TAG = "AlarmScheduler"
    }

    private val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

    fun schedule(activity: ActivityModel) {
        try {
            activity.repeatDays.forEach { day ->
                val triggerTime = calculateTriggerTime(activity.startTime, day)
                val requestCode = "${activity.activityId}_$day".hashCode()

                val intent = Intent(context, ActivityNotificationReceiver::class.java).apply {
                    action = "ACTION_${activity.activityId}_$day"
                    putExtra("activity_data", Gson().toJson(activity))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

                Log.d("AlarmDebug", "Đã lên lịch: ${activity.activityId} ngày $day (requestCode: $requestCode)")
            }
        } catch (e: Exception) {
            Log.e("AlarmError", "Lỗi khi lên lịch", e)
        }
    }

    private fun calculateTriggerTime(startTime: Timestamp, day: String): Long {
        return Calendar.getInstance().apply {
            time = startTime.toDate()
            set(Calendar.DAY_OF_WEEK, convertDayToCalendarDay(day))

            if (timeInMillis < System.currentTimeMillis()) {
                Log.d(TAG, "Thời gian đã qua, lên lịch cho tuần sau")
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun convertDayToCalendarDay(day: String): Int {
        return when (day) {
            "T2" -> Calendar.MONDAY
            "T3" -> Calendar.TUESDAY
            "T4" -> Calendar.WEDNESDAY
            "T5" -> Calendar.THURSDAY
            "T6" -> Calendar.FRIDAY
            "T7" -> Calendar.SATURDAY
            "CN" -> Calendar.SUNDAY
            else -> {
                Calendar.SUNDAY
            }
        }
    }

    fun cancelAllForActivity(activityId: String) {
        listOf("T2","T3","T4","T5","T6","T7","CN").forEach { day ->
            cancelExactAlarm("${activityId}_$day")
        }

        val intent = Intent(context, ActivityNotificationReceiver::class.java).apply {
            action = "ACTION_SHOW_${activityId}"
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        pendingIntent?.cancel()
    }

    private fun cancelExactAlarm(requestId: String) {
        try {
            val requestCode = requestId.hashCode()
            val intent = Intent(context, ActivityNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d("Alarm", "Đã hủy alarm: $requestId")
            } ?: Log.d("Alarm", "Không tìm thấy alarm: $requestId")
        } catch (e: Exception) {
            Log.e("Alarm", "Lỗi khi hủy alarm", e)
        }
    }
}