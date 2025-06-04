package com.example.timemanagementkot.ui.statistic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.data.model.StatsModel
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsVM : ViewModel() {
    private val activityRepo = ActivityRepo()
    private val authRepo = AuthRepo()

    private val _logsWithTitle = MutableStateFlow<List<Pair<LogTimeModel, String>>>(emptyList())
    val logsWithTitle: StateFlow<List<Pair<LogTimeModel, String>>> = _logsWithTitle

    private val _weeklyStatsWithTitle = MutableStateFlow<List<Pair<StatsModel, String>>>(emptyList())
    val weeklyStatsWithTitle: StateFlow<List<Pair<StatsModel, String>>> = _weeklyStatsWithTitle

    private val _monthlyStatsWithTitle = MutableStateFlow<List<Pair<StatsModel, String>>>(emptyList())
    val monthlyStatsWithTitle: StateFlow<List<Pair<StatsModel, String>>> = _monthlyStatsWithTitle

    private val _selectedFilter = MutableStateFlow("day")
    val selectedFilter: StateFlow<String> get() = _selectedFilter

    private val _currentDateState = MutableStateFlow("")
    val currentDateState: StateFlow<String> = _currentDateState

    private val _currentDateOffset = MutableStateFlow(0)
    val currentDateOffset: StateFlow<Int> = _currentDateOffset

    private val _currentWeekState = MutableStateFlow("")
    val currentWeekState: StateFlow<String> = _currentWeekState

    private val _currentWeekOffset = MutableStateFlow(0)
    val currentWeekOffset: StateFlow<Int> = _currentWeekOffset

    private val _currentMonthState = MutableStateFlow("")
    val currentMonthState: StateFlow<String> = _currentMonthState

    private val _currentMonthOffset = MutableStateFlow(0)
    val currentMonthOffset: StateFlow<Int> = _currentMonthOffset

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId != null) {
                activityRepo.createOrUpdateStatsForWeek(userId, isCurrentWeek = true)
                activityRepo.createOrUpdateStatsForMonth(userId, isCurrentMonth = true)

                fetchDataForWeek(_currentWeekOffset.value)
                fetchDataForMonth(_currentMonthOffset.value)
            }
        }
        fetchDataForDate(0)
    }

    fun onPreviousDay() {
        fetchDataForDate(_currentDateOffset.value - 1)
    }

    fun onNextDay() {
        fetchDataForDate(_currentDateOffset.value + 1)
    }

    fun onPreviousWeek() {
        fetchDataForWeek(_currentWeekOffset.value - 1)
    }

    fun onNextWeek() {
        fetchDataForWeek(_currentWeekOffset.value + 1)
    }

    fun onPreviousMonth() {
        fetchDataForMonth(_currentMonthOffset.value - 1)
    }

    fun onNextMonth() {
        fetchDataForMonth(_currentMonthOffset.value + 1)
    }

    private fun fetchDataForDate(offset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentDateOffset.value = offset

            try {
                updateCurrentDate(offset)
                val userId = getCurrentUserId()
                val date = getDateForOffset(offset)

                if (userId == null) {
                    _errorMessage.value = "User not logged in"
                    _logsWithTitle.value = emptyList()
                    return@launch
                }

                val logs = activityRepo.getLogsWithActivityTitle(userId, date)
                _logsWithTitle.value = logs

                if (logs.isEmpty()) {
                    _errorMessage.value = "Chưa có dữ liệu"
                }

                Log.d("StatisticsVM", "Loaded ${logs.size} logs for date: $date")
            } catch (e: Exception) {
                _errorMessage.value = "Error loading data: ${e.localizedMessage}"
                Log.e("StatisticsVM", "Fetch error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchDataForWeek(weekOffset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentWeekOffset.value = weekOffset

            try {
                updateCurrentWeekDisplay(weekOffset)
                val userId = getCurrentUserId() ?: run {
                    _errorMessage.value = "User not logged in"
                    _weeklyStatsWithTitle.value = emptyList()
                    _currentWeekState.value = ""
                    return@launch
                }

                val startOfWeek = getStartOfWeekForOffset(weekOffset)
                val endOfWeek = getEndOfWeekForOffset(weekOffset)

                val isCurrentWeek = weekOffset == 0
                activityRepo.createOrUpdateStatsForWeek(userId, startOfWeek, endOfWeek, isCurrentWeek)

                val stats = activityRepo.getStatsWithTitleBetweenPeriods(
                    userId,
                    startOfWeek,
                    endOfWeek,
                    "week"
                )

                _weeklyStatsWithTitle.value = stats

                if (stats.isNotEmpty()) {
                    _currentWeekState.value = stats.first().first.periodLabel
                } else {
                    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                    _currentWeekState.value = "Tuần ${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"
                    _errorMessage.value = "Chưa có dữ liệu"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading weekly data: ${e.localizedMessage}"
                _currentWeekState.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchDataForMonth(monthOffset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentMonthOffset.value = monthOffset

            try {
                updateCurrentMonthDisplay(monthOffset)
                val userId = getCurrentUserId() ?: run {
                    _errorMessage.value = "User not logged in"
                    _monthlyStatsWithTitle.value = emptyList()
                    _currentMonthState.value = ""
                    return@launch
                }

                val (startOfMonth, endOfMonth) = getStartEndOfMonthForOffset(monthOffset)

                activityRepo.createOrUpdateStatsForMonth(userId, startOfMonth, endOfMonth)

                val stats = activityRepo.getStatsWithTitleBetweenPeriods(
                    userId,
                    startOfMonth,
                    endOfMonth,
                    "month"
                )

                _monthlyStatsWithTitle.value = stats

                if (stats.isNotEmpty()) {
                    _currentMonthState.value = stats.first().first.periodLabel
                } else {
                    val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    _currentMonthState.value = dateFormat.format(startOfMonth)
                    _errorMessage.value = "Chưa có dữ liệu"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading monthly data: ${e.localizedMessage}"
                _currentMonthState.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getDateForOffset(offset: Int): Date {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, offset)
        }
        return calendar.time
    }

    private fun updateCurrentDate(offset: Int) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, offset)
        }
        val dayOfWeek = convertCalendarDayToFullDayFormat(calendar.get(Calendar.DAY_OF_WEEK))
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val dateStr = dateFormat.format(calendar.time)

        _currentDateState.value = "$dayOfWeek, $dateStr"
    }

    private fun updateCurrentWeekDisplay(weekOffset: Int) {
        val start = getStartOfWeekForOffset(weekOffset)
        val end = getEndOfWeekForOffset(weekOffset)
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        _currentWeekState.value = "Tuần ${dateFormat.format(start)} - ${dateFormat.format(end)}"
    }

    private fun getStartOfWeekForOffset(weekOffset: Int): Date {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val diff = (7 + (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY)) % 7
            add(Calendar.DATE, -diff)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun getEndOfWeekForOffset(weekOffset: Int): Date {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val diff = (7 + (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY)) % 7
            add(Calendar.DATE, -diff)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            add(Calendar.DATE, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }


    private fun updateCurrentMonthDisplay(monthOffset: Int) {
        val (startOfMonth, endOfMonth) = getStartEndOfMonthForOffset(monthOffset)
        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        _currentMonthState.value = "Tháng ${dateFormat.format(startOfMonth)}"
    }

    private fun getStartEndOfMonthForOffset(monthOffset: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time

        return startOfMonth to endOfMonth
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun setSelectedFilter(selectedFilter: String) {
        _selectedFilter.value = selectedFilter
        when (selectedFilter) {
            "day" -> fetchDataForDate(_currentDateOffset.value)
            "week" -> fetchDataForWeek(_currentWeekOffset.value)
            "month" -> fetchDataForMonth(_currentMonthOffset.value)
            else -> fetchDataForDate(_currentDateOffset.value)
        }
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
}