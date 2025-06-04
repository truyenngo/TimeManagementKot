package com.example.timemanagementkot.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DataHelper {
    fun getCurrentWeekRange(): Pair<Timestamp, Timestamp> {
        val calendar = Calendar.getInstance().apply {
            val today = get(Calendar.DAY_OF_WEEK)
            if (today == Calendar.SUNDAY) {
                add(Calendar.DATE, -6)
            } else {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = Timestamp(calendar.time)

        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeek = Timestamp(calendar.time)

        return Pair(startOfWeek, endOfWeek)
    }


    fun getCurrentMonthRange(): Pair<Timestamp, Timestamp> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = Timestamp(calendar.time)

        calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfMonth = Timestamp(calendar.time)

        return Pair(startOfMonth, endOfMonth)
    }

    fun getTimeOfDayInSeconds(timestamp: Timestamp): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = timestamp.toDate().time
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        return hour * 3600 + minute * 60 + second
    }

    fun parseSuggestedTime(originalTime: Timestamp, newTimeStr: String): Timestamp {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val newTime = timeFormat.parse(newTimeStr) ?: return originalTime

        val calendar = Calendar.getInstance().apply {
            time = originalTime.toDate()

            val newTimeCalendar = Calendar.getInstance().apply { time = newTime }
            set(Calendar.HOUR_OF_DAY, newTimeCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, newTimeCalendar.get(Calendar.MINUTE))
        }

        return Timestamp(calendar.time)
    }

}