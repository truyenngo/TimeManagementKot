package com.example.timemanagementkot.data.repo

import android.util.Log
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityType
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.data.model.StatsModel
import com.example.timemanagementkot.data.model.TimeAdjustmentSuggestion
import com.example.timemanagementkot.util.DataHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.snapshots
import java.util.Date

class ActivityRepo {
    private val db = FirebaseFirestore.getInstance()
    val activitiesCollection = db.collection("activities")
    val logTimesCollection = db.collection("logTimes")
    val goalHeaderCollection = db.collection("goalHeaders")
    val statsCollection = db.collection("stats")

    private val goalRepo = GoalRepo()

    suspend fun addActivity(model: ActivityModel): Result<String> {
        return try {
            require(model.userId.isNotEmpty()) { "User ID must not be empty" }
            require(model.title.isNotEmpty()) { "Title must not be empty" }
            require(model.type.isNotEmpty()) { "Type must not be empty" }
            require(model.startTime <= model.endTime) { "startTime must be before or equal to endTime" }

            val documentRef = activitiesCollection.document()
            val newModel = model.copy(activityId = documentRef.id)
            documentRef.set(newModel).await()
            Result.success(newModel.activityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivitiesByRepeatDay(userId: String, dayOfWeek: String): Result<List<ActivityModel>> {
        return try {
            if (userId.isEmpty() || dayOfWeek.isEmpty()) {
                throw IllegalArgumentException("User ID and day of week must not be empty")
            }

            val querySnapshot = activitiesCollection
                .whereEqualTo("userId", userId)
                .whereArrayContains("repeatDays", dayOfWeek)
                .get()
                .await()

            val activities = querySnapshot.documents.mapNotNull { document ->
                document.toObject(ActivityModel::class.java)?.copy(activityId = document.id)
            }
            Result.success(activities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivitiesByUserAndRepeatDays(userId: String, repeatDays: List<String>): List<ActivityModel> {
        val results = mutableListOf<ActivityModel>()
        val usedIds = mutableSetOf<String>()
        for (day in repeatDays) {
            val dayResult = getActivitiesByRepeatDay(userId, day)
            if (dayResult.isSuccess) {
                val activities = dayResult.getOrThrow()
                for (activity in activities) {
                    if (activity.activityId !in usedIds) {
                        results.add(activity)
                        usedIds.add(activity.activityId)
                    }
                }
            }
        }
        return results
    }

    suspend fun checkTimeConflict(userId: String, startTime: Timestamp, endTime: Timestamp, repeatDays: List<String>, excludeActivityId: String? = null): String? {
        val activities = getActivitiesByUserAndRepeatDays(userId, repeatDays)
        Log.e("ConflictCheck", "Tổng số hoạt động trong ngày trùng: ${activities.size}")

        val conflicts = mutableListOf<String>()

        val startSeconds = DataHelper.getTimeOfDayInSeconds(startTime)
        val endSeconds = DataHelper.getTimeOfDayInSeconds(endTime)

        for (activity in activities) {
            if (excludeActivityId != null && activity.activityId == excludeActivityId) continue

            val activityStartSeconds = DataHelper.getTimeOfDayInSeconds(activity.startTime)
            val activityEndSeconds = DataHelper.getTimeOfDayInSeconds(activity.endTime)

            if (startSeconds <= activityEndSeconds && endSeconds >= activityStartSeconds) {
                val conflictDays = activity.repeatDays.joinToString(", ")
                val conflictMsg = "Hoạt động '${activity.title}' (lặp vào $conflictDays) từ ${formatTimestamp(activity.startTime)} đến ${formatTimestamp(activity.endTime)}"
                Log.e("CheckConflict", "So sánh với: $conflictMsg")
                conflicts.add(conflictMsg)
            }
        }
        return if (conflicts.isNotEmpty()) {
            "Có xung đột thời gian với:\n" + conflicts.joinToString("\n")
        } else {
            null
        }
    }

    fun formatTimestamp(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    suspend fun checkTitleConflict(userId: String, title: String, excludeActivityId: String? = null): String? {
        val activities = getActivitiesByUserId(userId).getOrNull() ?: return null
        for (activity in activities) {
            if (excludeActivityId != null && activity.activityId == excludeActivityId) continue
            if (activity.title.equals(title, ignoreCase = true)) {
                return "Tiêu đề '$title' đã tồn tại trong hoạt động khác"
            }
        }
        return null
    }

    suspend fun getActivitiesByUserId(userId: String): Result<List<ActivityModel>> {
        return try {
            if (userId.isEmpty()) {
                throw IllegalArgumentException("User ID must not be empty")
            }

            val querySnapshot = activitiesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val activities = querySnapshot.documents.mapNotNull { document ->
                document.toObject(ActivityModel::class.java)?.copy(activityId = document.id)
            }

            Log.d("Firestore", "Loaded ${activities.size} activities for userId: $userId")
            Result.success(activities)
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading activities for userId: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun updateActivityById(activityId: String, updatedActivity: ActivityModel): Result<Unit> {
        return try {
            require(activityId.isNotEmpty()) { "Activity ID must not be empty" }
            require(updatedActivity.userId.isNotEmpty()) { "User ID must not be empty" }

            val documentRef = activitiesCollection.document(activityId)

            val updates = mapOf(
                "title" to updatedActivity.title,
                "startTime" to updatedActivity.startTime,
                "endTime" to updatedActivity.endTime,
                "type" to updatedActivity.type,
                "repeatDays" to updatedActivity.repeatDays,

                "pomodoroSettings.pomodoroEnable" to updatedActivity.pomodoroSettings.pomodoroEnable,
                "pomodoroSettings.focusMinutes" to updatedActivity.pomodoroSettings.focusMinutes,
                "pomodoroSettings.breakMinutes" to updatedActivity.pomodoroSettings.breakMinutes
            )

            documentRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Firestore", "Error updating activity with id $activityId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteActivityById(id: String): Result<Unit> {
        return try {
            require(id.isNotEmpty()) { "Document ID must not be empty" }

            activitiesCollection.document(id).delete().await()

            Log.d("Firestore", "Deleted activity with id: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Firestore", "Error deleting activity with id $id", e)
            Result.failure(e)
        }
    }

    suspend fun saveLogTime(model: LogTimeModel): Result<String> {
        return try {
            require(model.activityId.isNotEmpty()) { "Activity ID must not be empty" }
            require(model.userId.isNotEmpty()) { "User ID must not be empty" }
            require(model.actualStart <= model.actualEnd) { "actualStart must be before or equal to actualEnd" }
            require(model.duration > 0) { "Duration must be positive" }

            val documentRef = db.collection("logTimes").document()
            val newModel = model.copy(logTimeId = documentRef.id)
            println("LogTime to save: $newModel")
            documentRef.set(newModel).await()
            Result.success(newModel.logTimeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivitiesByDate(userId: String, dateOffset: Int = 0): Result<List<ActivityWithLogTime>> {
        return try {
            if (userId.isEmpty()) {
                throw IllegalArgumentException("User ID must not be empty")
            }

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                add(Calendar.DATE, dateOffset)
            }

            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> error("Invalid day of week")
            }

            val activities = activitiesCollection
                .whereArrayContains("repeatDays", dayOfWeek)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(ActivityModel::class.java)?.copy(activityId = doc.id)
                }

            val (startOfDay, endOfDay) = getDateRangeForVN(calendar)
            Log.e("UTC_RANGE", "For offset $dateOffset ($dayOfWeek): Start: ${startOfDay} | End: ${endOfDay}")

            val logTimes = logTimesCollection
                .whereIn("activityId", activities.map { it.activityId })
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(LogTimeModel::class.java)?.copy(logTimeId = doc.id)
                }

            val results = activities.map { activity ->
                ActivityWithLogTime(
                    activity = activity,
                    completeStatus = logTimes.any { it.activityId == activity.activityId && it.completeStatus }
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDateRangeForVN(calendar: Calendar): Pair<Timestamp, Timestamp> {
        val startCalendar = calendar.clone() as Calendar
        startCalendar.apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = Timestamp(startCalendar.time)

        val endCalendar = startCalendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = Timestamp(endCalendar.time)

        return start to end
    }

    suspend fun getActivitiesWithoutGoalHeader(userId: String, goalType: String): Result<List<ActivityModel>> {
        return try {
            if (userId.isEmpty()) {
                throw IllegalArgumentException("User ID must not be empty")
            }

            val activitiesResult = getActivitiesByUserId(userId)
            if (activitiesResult.isFailure) {
                return Result.failure(activitiesResult.exceptionOrNull()!!)
            }
            val allActivities = activitiesResult.getOrThrow()

            val goalHeadersResult = goalRepo.getCurrentGoalHeaderWithDetailsAndUpdate(userId, goalType)
            if (goalHeadersResult.isFailure) {
                return Result.failure(goalHeadersResult.exceptionOrNull()!!)
            }
            val allGoalHeaders = goalHeadersResult.getOrThrow()

            val activityIdsWithGoalHeaders = allGoalHeaders.map { it.header.activityId }.toSet()
            val activitiesWithoutGoalHeader = allActivities.filter { it.activityId !in activityIdsWithGoalHeaders }

            Log.d(
                "Firestore",
                "Loaded ${activitiesWithoutGoalHeader.size} activities without $goalType goal headers for user: $userId"
            )

            Result.success(activitiesWithoutGoalHeader)
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading activities without $goalType goal headers", e)
            Result.failure(e)
        }
    }

    suspend fun updateGoalTitlesWhenActivityRenamed(activityId: String, newTitle: String) {
        val snapshot = goalHeaderCollection.whereEqualTo("activityId", activityId).get().await()
        snapshot.documents.forEach { doc ->
            doc.reference.update("goalTitle", newTitle)
        }
    }

    suspend fun getLogTimesForActivityInPeriod(activityId: String, userId: String, goalType: String): Result<List<LogTimeModel>> {
        return try {
            val (startPeriod, endPeriod) = when (goalType) {
                "weekly" -> DataHelper.getCurrentWeekRange()
                "monthly" -> DataHelper.getCurrentMonthRange()
                else -> DataHelper.getCurrentWeekRange()
            }

            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startPeriod)
                .whereLessThanOrEqualTo("date", endPeriod)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LogTimeModel::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(logs)
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading logTimes for activity in period", e)
            Result.failure(e)
        }
    }

    suspend fun getActivityById(activityId: String): ActivityModel? {
        return try {
            require(activityId.isNotEmpty()) { "Activity ID must not be empty" }

            val document = activitiesCollection.document(activityId).get().await()

            if (document.exists()) {
                document.toObject(ActivityModel::class.java)?.also {
                    Log.d("Firestore", "Found activity: ${it.title} (ID: $activityId)")
                }
            } else {
                Log.d("Firestore", "Activity not found with ID: $activityId")
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting activity by ID", e)
            null
        }
    }

    suspend fun isActivityExist(activityId: String): Boolean {
        return try {
            activitiesCollection.document(activityId).get().await().exists()
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Error checking activity existence", e)
            false
        }
    }

    suspend fun getLogsByActivity(activityId: String): Result<List<LogTimeModel>> {
        return try {
            require(activityId.isNotEmpty()) { "Activity ID must not be empty" }

            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .orderBy("date") // Sắp xếp theo ngày
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LogTimeModel::class.java)?.copy(logTimeId = doc.id)
            }

            Result.success(logs)
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading logs for activity $activityId", e)
            Result.failure(e)
        }
    }

    // XU LY AI
    fun getLogsByUser(userId: String): Flow<List<LogTimeModel>> {
        return try {
            Log.d("ActivityRepo", "Đang lấy logs chưa phân tích cho user: ${userId.take(3)}...***")

            logTimesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("analyzedByAI", false)
                .snapshots()
                .map { querySnapshot: QuerySnapshot ->
                    val logs = querySnapshot.documents.mapNotNull { document ->
                        document.toObject<LogTimeModel>()?.copy(logTimeId = document.id)
                    }
                    Log.d("ActivityRepo", "Nhận được ${logs.size} logs chưa phân tích")
                    logs
                }
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Lỗi khi thiết lập listener logs", e)
            throw e
        }
    }

    suspend fun markLogAsAnalyzed(logId: String): Result<Unit> {
        return try {
            Log.d("ActivityRepo", "Đang đánh dấu log $logId đã phân tích...")

            logTimesCollection.document(logId)
                .update("analyzedByAI", true)
                .await()

            Log.d("ActivityRepo", "Đánh dấu thành công log $logId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Lỗi khi đánh dấu log $logId", e)
            Result.failure(e)
        }
    }

    suspend fun getUnanalyzedLogs(activityId: String): Result<List<LogTimeModel>> {
        return try {
            Log.d("ActivityRepo", "Lấy logs chưa phân tích cho activity $activityId")

            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("analyzedByAI", false)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                doc.toObject<LogTimeModel>()?.copy(logTimeId = doc.id)
            }

            Log.d("ActivityRepo", "Tìm thấy ${logs.size} logs chưa phân tích")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Lỗi khi lấy unanalyzed logs", e)
            Result.failure(e)
        }
    }

    suspend fun getActivityWithMostUnanalyzedLogs(userId: String): ActivityModel? {
        return try {
            Log.d("ActivityRepo", "Tìm activity có nhiều logs chưa phân tích nhất...")

            val activities = getActivitiesByUserId(userId).getOrNull() ?: run {
                Log.d("ActivityRepo", "Không tìm thấy activities cho user $userId")
                return null
            }

            val targetActivity = activities.maxByOrNull { activity ->
                getUnanalyzedLogs(activity.activityId).getOrNull()?.size ?: 0
            }

            if (targetActivity != null) {
                Log.d("ActivityRepo", "Activity được chọn để phân tích: ${targetActivity.title} (${targetActivity.activityId})")
            } else {
                Log.d("ActivityRepo", "Không tìm thấy activity phù hợp")
            }

            targetActivity
        } catch (e: Exception) {
            Log.e("ActivityRepo", "Lỗi khi tìm activity", e)
            null
        }
    }

    suspend fun saveSuggestions(suggestions: List<TimeAdjustmentSuggestion>) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        suggestions.forEach { suggestion ->
            val docRef = if (suggestion.sugestionId.isNotEmpty()) {
                db.collection("suggestions").document(suggestion.sugestionId)
            } else {
                db.collection("suggestions").document()
            }
            val suggestionWithId = suggestion.copy(sugestionId = docRef.id)
            batch.set(docRef, suggestionWithId)
        }

        batch.commit().await()
    }

    suspend fun getSuggestionsList(userId: String): List<TimeAdjustmentSuggestion> {
        Log.d("Repo", "Querying for userId: [$userId]")

        return try {
            val snapshot = db.collection("suggestions")
                .whereEqualTo("userId", userId.trim())
                .whereEqualTo("status", "pending")
                .get()
                .await()

            Log.d("Repo", "Query returned ${snapshot.documents.size} documents")

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(TimeAdjustmentSuggestion::class.java)?.copy(sugestionId = doc.id).also {
                    Log.d("Repo", "Document: ${doc.id} - ${doc.data}")
                }
            }
        } catch (e: Exception) {
            Log.e("Repo", "Error getting suggestions", e)
            emptyList()
        }
    }

    suspend fun updateActivityTime(activityId: String, newStartTime: Timestamp, newEndTime: Timestamp): Result<Unit> {
        return try {
            val updates = mapOf(
                "startTime" to newStartTime,
                "endTime" to newEndTime
            )

            activitiesCollection.document(activityId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSuggestionStatus(suggestionId: String, newStatus: String): Result<Unit> {
        return try {
            db.collection("suggestions").document(suggestionId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSuggestion(suggestionId: String): Result<Unit> {
        return try {
            db.collection("suggestions").document(suggestionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // THONG KE
    suspend fun getLogsWithActivityTitle(userId: String, date: Date): List<Pair<LogTimeModel, String>> {
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        try {
            val logSnapshot = logTimesCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get()
                .await()

            val logs = logSnapshot.documents.mapNotNull { it.toObject(LogTimeModel::class.java) }

            if (logs.isEmpty()) {
                return emptyList()
            }

            val activityIds = logs.map { it.activityId }.distinct()

            if (activityIds.isEmpty()) {
                return logs.map { it to "No Title" }
            }

            val activityTitlesMap = mutableMapOf<String, String>()

            activityIds.chunked(10).forEachIndexed { index, chunk ->
                val activitySnapshot = activitiesCollection
                    .whereIn("activityId", chunk)
                    .get()
                    .await()

                activitySnapshot.documents.forEach { doc ->
                    val id = doc.getString("activityId") ?: ""
                    val title = doc.getString("title") ?: "No Title"
                    if (id.isNotEmpty()) {
                        activityTitlesMap[id] = title
                    }
                }
            }

            val result = logs.map { log ->
                val title = activityTitlesMap[log.activityId] ?: "No Title"
                log to title
            }

            return result

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun getStatsForActivityInPeriod(activityId: String, type: String, startPeriod: Timestamp, endPeriod: Timestamp): StatsModel? {
        return try {
            val snapshot = statsCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("type", type)
                .whereEqualTo("periodStart", startPeriod)
                .whereEqualTo("periodEnd", endPeriod)
                .get()
                .await()

            if (snapshot.isEmpty) null
            else snapshot.documents.first().toObject(StatsModel::class.java)?.copy(statId = snapshot.documents.first().id)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get StatsModel", e)
            null
        }
    }

    suspend fun saveStatsModel(statsModel: StatsModel): Boolean {
        return try {
            if (statsModel.statId.isBlank()) {
                val docRef = statsCollection.add(statsModel).await()
                val newId = docRef.id
                statsCollection.document(newId).update("statId", newId).await()
            } else {
                statsCollection.document(statsModel.statId).set(statsModel).await()
            }
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to save StatsModel", e)
            false
        }
    }

    suspend fun getActivitiesForUser(userId: String): List<ActivityModel> {
        return try {
            val snapshot = activitiesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ActivityModel::class.java)?.copy(activityId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get activities for user $userId", e)
            emptyList()
        }
    }

    suspend fun getLogTimesForActivityInPeriod(activityId: String, userId: String, startPeriod: Timestamp, endPeriod: Timestamp): List<LogTimeModel> {
        return try {
            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startPeriod)
                .whereLessThanOrEqualTo("date", endPeriod)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LogTimeModel::class.java)?.copy(logTimeId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get logTimes for activity $activityId", e)
            emptyList()
        }
    }

    fun calculateMissedCount(activity: ActivityModel, logs: List<LogTimeModel>, startPeriod: Timestamp, endPeriod: Timestamp): Int {
        val repeatDays = activity.repeatDays
        if (repeatDays.isEmpty()) return 0

        val startCal = Calendar.getInstance().apply { time = startPeriod.toDate() }
        val endCal = Calendar.getInstance().apply { time = endPeriod.toDate() }

        var totalExpected = 0

        val currentCal = startCal.clone() as Calendar
        while (!currentCal.after(endCal)) {
            val dayLabel = when (currentCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }
            if (repeatDays.contains(dayLabel)) {
                totalExpected++
            }
            currentCal.add(Calendar.DATE, 1)
        }

        val completedCount = logs.count { it.completeStatus }

        return (totalExpected - completedCount).coerceAtLeast(0)
    }

    fun calculatePendingCount(activity: ActivityModel, logs: List<LogTimeModel>, startPeriod: Timestamp, endPeriod: Timestamp): Int {
        val repeatDays = activity.repeatDays
        if (repeatDays.isEmpty()) return 0

        val now = Calendar.getInstance()

        val endCal = Calendar.getInstance().apply { time = endPeriod.toDate() }
        if (now.after(endCal)) return 0

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val startCal = now.clone() as Calendar
        val logDatesSet = logs.map { sdf.format(it.date.toDate()) }.toSet()

        var pendingCount = 0
        val currentCal = startCal.clone() as Calendar

        while (!currentCal.after(endCal)) {
            val dayLabel = when (currentCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }

            if (repeatDays.contains(dayLabel)) {
                val dayString = sdf.format(currentCal.time)
                if (!logDatesSet.contains(dayString)) {
                    pendingCount++
                }
            }
            currentCal.add(Calendar.DATE, 1)
        }
        return pendingCount
    }

    suspend fun createOrUpdateStatsForCurrentWeek(userId: String) {
        val activities = getActivitiesForUser(userId)
        if (activities.isEmpty()) return

        val (startOfWeek, endOfWeek) = DataHelper.getCurrentWeekRange()
        val periodLabel = "Tuần ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(startOfWeek.toDate())} - " +
                "${SimpleDateFormat("dd/MM", Locale.getDefault()).format(endOfWeek.toDate())}"

        activities.forEach { activity ->
            val existingStats = getStatsForActivityInPeriod(activity.activityId,"week", startOfWeek, endOfWeek)

            if (existingStats == null) {
                val logs = getLogTimesForActivityInPeriod(activity.activityId, userId, startOfWeek, endOfWeek)
                val totalDuration = logs.sumOf { it.duration }
                val completedCount = logs.count { it.completeStatus }
                val missedCount = calculateMissedCount(activity, logs, startOfWeek, endOfWeek)
                val pendingCount = calculatePendingCount(activity, logs, startOfWeek, endOfWeek)

                val newStats = StatsModel(
                    statId = "",
                    activityId = activity.activityId,
                    userId = userId,
                    type = "week",
                    periodLabel = periodLabel,
                    periodStart = startOfWeek,
                    periodEnd = endOfWeek,
                    totalDuration = totalDuration,
                    completedCount = completedCount,
                    missedCount = missedCount,
                    pendingCount = pendingCount,
                    status = "pending"
                )

                saveStatsModel(newStats)
            } else {
                if (existingStats.status == "pending") {
                    val logs = getLogTimesForActivityInPeriod(activity.activityId, userId, startOfWeek, endOfWeek)
                    val totalDuration = logs.sumOf { it.duration }
                    val completedCount = logs.count { it.completeStatus }
                    val missedCount = calculateMissedCount(activity, logs, startOfWeek, endOfWeek)
                    val pendingCount = calculatePendingCount(activity, logs, startOfWeek, endOfWeek)

                    val updatedStats = existingStats.copy(
                        totalDuration = totalDuration,
                        completedCount = completedCount,
                        missedCount = missedCount,
                        pendingCount = pendingCount
                    )

                    saveStatsModel(updatedStats)
                }
            }
        }
    }

    suspend fun createOrUpdateStatsForCurrentMonth(userId: String) {
        val activities = getActivitiesForUser(userId)
        if (activities.isEmpty()) return

        val (startOfMonth, endOfMonth) = DataHelper.getCurrentMonthRange()
        val periodLabel = "Tháng ${SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(startOfMonth.toDate())}"

        activities.forEach { activity ->
            val existingStats = getStatsForActivityInPeriod(activity.activityId, "month", startOfMonth, endOfMonth)

            if (existingStats == null) {
                val logs = getLogTimesForActivityInPeriod(activity.activityId, userId, startOfMonth, endOfMonth)
                val totalDuration = logs.sumOf { it.duration }
                val completedCount = logs.count { it.completeStatus }
                val missedCount = calculateMissedCount(activity, logs, startOfMonth, endOfMonth)
                val pendingCount = calculatePendingCount(activity, logs, startOfMonth, endOfMonth)

                val newStats = StatsModel(
                    statId = "",
                    activityId = activity.activityId,
                    userId = userId,
                    type = "month",
                    periodLabel = periodLabel,
                    periodStart = startOfMonth,
                    periodEnd = endOfMonth,
                    totalDuration = totalDuration,
                    completedCount = completedCount,
                    missedCount = missedCount,
                    pendingCount = pendingCount,
                    status = "pending"
                )

                saveStatsModel(newStats)
            } else {
                if (existingStats.status == "pending") {
                    val logs = getLogTimesForActivityInPeriod(activity.activityId, userId, startOfMonth, endOfMonth)
                    val totalDuration = logs.sumOf { it.duration }
                    val completedCount = logs.count { it.completeStatus }
                    val missedCount = calculateMissedCount(activity, logs, startOfMonth, endOfMonth)
                    val pendingCount = calculatePendingCount(activity, logs, startOfMonth, endOfMonth)

                    val updatedStats = existingStats.copy(
                        totalDuration = totalDuration,
                        completedCount = completedCount,
                        missedCount = missedCount,
                        pendingCount = pendingCount
                    )

                    saveStatsModel(updatedStats)
                }
            }
        }
    }

    suspend fun getStatsWithTitleBetweenPeriods(userId: String, startPeriod: Date, endPeriod: Date, type: String): List<Pair<StatsModel, String>> {
        try {
            val startTimestamp = Timestamp(startPeriod)
            val endTimestamp = Timestamp(endPeriod)

            val statSnapshot = statsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .whereGreaterThanOrEqualTo("periodStart", startTimestamp)
                .whereLessThanOrEqualTo("periodEnd", endTimestamp)
                .get()
                .await()

            val stats = statSnapshot.documents.mapNotNull { it.toObject(StatsModel::class.java) }

            if (stats.isEmpty()) return emptyList()

            val activityIds = stats.map { it.activityId }.distinct()
            if (activityIds.isEmpty()) {
                return stats.map { it to "No Title" }
            }

            val activityTitlesMap = mutableMapOf<String, String>()

            activityIds.chunked(10).forEach { chunk ->
                val activitySnapshot = activitiesCollection
                    .whereIn("activityId", chunk)
                    .get()
                    .await()

                activitySnapshot.documents.forEach { doc ->
                    val id = doc.getString("activityId") ?: ""
                    val title = doc.getString("title") ?: "No Title"
                    if (id.isNotEmpty()) {
                        activityTitlesMap[id] = title
                    }
                }
            }

            return stats.map { stat ->
                val title = activityTitlesMap[stat.activityId] ?: "No Title"
                stat to title
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun addActivityType(userId: String, typeName: String): Result<String> {
        return try {
            val type = ActivityType(
                userId = userId,
                typeName = typeName
            )

            val docRef = db.collection("activityTypes").document()
            val typeWithId = type.copy(typeId = docRef.id)

            docRef.set(typeWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteActivityType(typeId: String): Result<Unit> {
        return try {
            db.collection("activityTypes").document(typeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserActivityTypes(userId: String): List<ActivityType> {
        return try {
            db.collection("activityTypes")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(ActivityType::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
