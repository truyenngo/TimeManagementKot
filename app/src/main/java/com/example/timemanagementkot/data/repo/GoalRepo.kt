package com.example.timemanagementkot.data.repo

import android.util.Log
import com.example.timemanagementkot.data.model.GoalDetail
import com.example.timemanagementkot.data.model.GoalHeader
import com.example.timemanagementkot.data.model.GoalWithDetail
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.util.DataHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class GoalRepo {
    private val db = FirebaseFirestore.getInstance()
    val logTimesCollection = db.collection("logTimes")
    val goalHeaderCollection = db.collection("goalHeaders")
    val goalDetailCollection = db.collection("goalDetails")

    suspend fun addGoalHeader(goalHeader: GoalHeader): Result<GoalHeader> {
        return try {
            val newGoalHeaderRef = goalHeaderCollection.document()
            val goalHeaderWithId = goalHeader.copy(goalId = newGoalHeaderRef.id)

            newGoalHeaderRef.set(goalHeaderWithId).await()

            Result.success(goalHeaderWithId)
        } catch (e: Exception) {
            Log.e("Firestore", "Error adding goal header", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentGoalHeaderWithDetailsAndUpdate(userId: String, goalType: String): Result<List<GoalWithDetail>> {
        return try {
            val headers = goalHeaderCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("goalType", goalType)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(GoalHeader::class.java)?.apply { goalId = doc.id }
                    } catch (e: Exception) {
                        Log.e("GoalRepo", "Invalid header ${doc.id}", e)
                        null
                    }
                }

            val logTimes = logTimesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(LogTimeModel::class.java) }

            val (startDate, endDate) = when (goalType) {
                "weekly" -> DataHelper.getCurrentWeekRange()
                "monthly" -> DataHelper.getCurrentMonthRange()
                else -> DataHelper.getCurrentWeekRange()
            }

            val results = headers.mapNotNull { header ->
                try {
                    val detailDocs = goalDetailCollection
                        .whereEqualTo("goalId", header.goalId)
                        .whereGreaterThanOrEqualTo("startDate", startDate)
                        .whereLessThanOrEqualTo("endDate", endDate)
                        .limit(1)
                        .get()
                        .await()
                        .documents

                    if (detailDocs.isEmpty()) return@mapNotNull null

                    val detailDoc = detailDocs.first()
                    val detail = detailDoc.toObject(GoalDetail::class.java) ?: return@mapNotNull null

                    val totalDuration = logTimes
                        .filter { log ->
                            log.activityId == header.activityId &&
                                    log.date.toDate() in startDate.toDate()..endDate.toDate()
                        }
                        .sumOf { it.duration }

                    val updatedDetail = detail.copy(currentDuration = totalDuration)
                    goalDetailCollection.document(detail.goalDetailId).set(updatedDetail).await()
                    GoalWithDetail(header, updatedDetail)
                } catch (e: Exception) {
                    Log.w("GoalRepo", "Failed to update goalDetail for goalId=${header.goalId}", e)
                    null
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e("GoalRepo", "Failed to fetch/update goal details", e)
            Result.failure(e)
        }
    }

    suspend fun createGoalDetail(goalHeader: GoalHeader, logTimes: List<LogTimeModel>, targetDuration: Long, goalType: String): Boolean {
        return try {
            val (startDate, endDate) = when (goalType) {
                "weekly" -> DataHelper.getCurrentWeekRange()
                "monthly" -> DataHelper.getCurrentMonthRange()
                else -> return false
            }

            val filteredByActivity = logTimes.filter { it.activityId == goalHeader.activityId }
            Log.d("GoalRepo", "Filtered by activity: ${filteredByActivity.size} logs")

            val filteredByDate = filteredByActivity.filter {
                val logDate = it.date.toDate()
                logDate in startDate.toDate()..endDate.toDate()
            }
            Log.d("GoalRepo", "Filtered by date: ${filteredByDate.size} logs")

            val totalDuration = filteredByDate.sumOf { it.duration }
            Log.d("GoalRepo", "Total duration from logs = $totalDuration")

            val newDetailRef = goalDetailCollection.document()

            val goalDetail = GoalDetail(
                goalDetailId = newDetailRef.id,
                goalId = goalHeader.goalId,
                targetDuration = targetDuration,
                currentDuration = totalDuration,
                startDate = startDate,
                endDate = endDate,
                completeStatus = totalDuration >= targetDuration
            )

            Log.d("GoalRepo", "Saving $goalType GoalDetail: $goalDetail")
            newDetailRef.set(goalDetail).await()
            true
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error creating $goalType goal detail", e)
            false
        }
    }

    suspend fun updateGoalDetail(goalDetailId: String, newTargetDuration: Long): Result<Boolean> {
        return try {
            val goalDetailDoc = goalDetailCollection.document(goalDetailId).get().await()

            if (!goalDetailDoc.exists()) {
                Log.w("Firestore", "GoalDetail with ID $goalDetailId not found")
                return Result.failure(Exception("GoalDetail not found"))
            }

            goalDetailDoc.reference.update("targetDuration", newTargetDuration).await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to update GoalDetail", e)
            Result.failure(e)
        }
    }

    suspend fun getGoalHeaderForUser(userId: String): Result<List<GoalHeader>> {
        return try {
            val goalHeaders = goalHeaderCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(GoalHeader::class.java)?.apply {
                            goalId = doc.id
                        }
                    } catch (e: Exception) {
                        Log.w("Firestore", "Skipped invalid goal header ${doc.id}", e)
                        null
                    }
                }

            Result.success(goalHeaders)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get goal headers for user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getGoalDetailForCurrentWeek(goalHeader: GoalHeader, startOfWeek: Timestamp, endOfWeek: Timestamp): Result<GoalDetail?> {
        return try {
            val goalDetail = goalDetailCollection
                .whereEqualTo("goalId", goalHeader.goalId)
                .whereLessThan("startDate", endOfWeek)
                .whereGreaterThan("endDate", startOfWeek)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(GoalDetail::class.java)

            Log.e("Goal", "Found existing week detail: $goalDetail")
            Result.success(goalDetail)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get GoalDetail for goalId ${goalHeader.goalId}", e)
            Result.failure(e)
        }
    }

    suspend fun getLatestGoalDetail(goalHeader: GoalHeader): Result<GoalDetail?> {
        return try {
            val (startOfWeek, _) = DataHelper.getCurrentWeekRange()

            val latestDetail = goalDetailCollection
                .whereEqualTo("goalId", goalHeader.goalId)
                .whereLessThan("endDate", startOfWeek)
                .orderBy("endDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(GoalDetail::class.java)

            if (latestDetail == null) {
                Log.w("Firestore", "No previous GoalDetail found for goalId: ${goalHeader.goalId}")
            } else {
                Log.d("Firestore", "Found previous GoalDetail for goalId: ${goalHeader.goalId}, targetDuration = ${latestDetail.targetDuration}")
            }

            Result.success(latestDetail)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get latest GoalDetail for goalId ${goalHeader.goalId}", e)
            Result.failure(e)
        }
    }

    suspend fun getLogTimesForActivityInCurrentWeek(activityId: String, userId: String): Result<List<LogTimeModel>> {
        return try {
            val (startOfWeek, endOfWeek) = DataHelper.getCurrentWeekRange()

            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfWeek)
                .whereLessThanOrEqualTo("date", endOfWeek)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LogTimeModel::class.java)?.apply {

                    }
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading logTimes for activity in current week", e)
            Result.failure(e)
        }
    }

    suspend fun createGoalDetailsForCurrentWeekIfNeeded(userId: String) {
        val headersResult = getGoalHeaderForUser(userId)
        if (headersResult.isFailure) {
            Log.e("Goal", "Failed to get GoalHeaders for userId: $userId", headersResult.exceptionOrNull())
            return
        }

        val headers = headersResult.getOrNull() ?: return
        val (startOfWeek, endOfWeek) = DataHelper.getCurrentWeekRange()

        headers.forEach { header ->
            Log.d("Goal", "Checking GoalHeader with goalId: ${header.goalId}")

            val detailResult = getGoalDetailForCurrentWeek(header, startOfWeek, endOfWeek)
            val existingDetail = detailResult.getOrNull()
            if (existingDetail != null) {
                Log.e("Goal", "GoalDetail already exists for goalId ${header.goalId} in current week.")
                return@forEach
            }

            val latestDetailResult = getLatestGoalDetail(header)
            if (latestDetailResult.isFailure) {
                Log.e("Goal", "Failed to get latest GoalDetail for goalId: ${header.goalId}", latestDetailResult.exceptionOrNull())
            }

            val targetDuration = latestDetailResult.getOrNull()?.targetDuration ?: 0L
            Log.d("Goal", "targetDuration for goalId ${header.goalId}: $targetDuration")

            val logsResult = getLogTimesForActivityInCurrentWeek(header.activityId, userId)
            if (logsResult.isFailure) {
                Log.e("Goal", "Failed to get logTimes for activityId: ${header.activityId}", logsResult.exceptionOrNull())
                return@forEach
            }

            val logs = logsResult.getOrNull() ?: return@forEach
            val currentDuration = logs.sumOf { it.duration }
            Log.d("Goal", "currentDuration for goalId ${header.goalId}: $currentDuration")

            val newDetailRef = goalDetailCollection.document()

            val newDetail = GoalDetail(
                goalDetailId = newDetailRef.id,
                goalId = header.goalId,
                targetDuration = targetDuration,
                currentDuration = currentDuration,
                startDate = startOfWeek,
                endDate = endOfWeek,
                completeStatus = currentDuration >= targetDuration
            )

            try {
                newDetailRef.set(newDetail).await()
                Log.d("Goal", "Created new GoalDetail for goalId ${header.goalId}")
            } catch (e: Exception) {
                Log.e("Goal", "Failed to create GoalDetail for goalId ${header.goalId}", e)
            }
        }
    }

    suspend fun getGoalDetailForMonth(goalHeader: GoalHeader, startOfMonth: Timestamp, endOfMonth: Timestamp): Result<GoalDetail?> {
        return try {
            val goalDetail = goalDetailCollection
                .whereEqualTo("goalId", goalHeader.goalId)
                .whereGreaterThanOrEqualTo("startDate", startOfMonth)
                .whereLessThanOrEqualTo("endDate", endOfMonth)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(GoalDetail::class.java)

            Result.success(goalDetail)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get GoalDetail for goalId ${goalHeader.goalId} in current month", e)
            Result.failure(e)
        }
    }

    suspend fun getLatestGoalDetailForMonth(goalHeader: GoalHeader): Result<GoalDetail?> {
        return try {
            val (startOfMonth, _) = DataHelper.getCurrentMonthRange()

            val latestDetail = goalDetailCollection
                .whereEqualTo("goalId", goalHeader.goalId)
                .whereLessThan("endDate", startOfMonth)
                .orderBy("endDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(GoalDetail::class.java)

            if (latestDetail == null) {
                Log.w("Firestore", "No previous monthly GoalDetail found for goalId: ${goalHeader.goalId}")
            } else {
                Log.d("Firestore", "Found monthly GoalDetail for goalId: ${goalHeader.goalId}, targetDuration = ${latestDetail.targetDuration}")
            }

            Result.success(latestDetail)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get monthly GoalDetail for goalId ${goalHeader.goalId}", e)
            Result.failure(e)
        }
    }

    suspend fun getLogTimesForActivityInMonth(activityId: String, userId: String, start: Timestamp, end: Timestamp): Result<List<LogTimeModel>> {
        return try {
            val snapshot = logTimesCollection
                .whereEqualTo("activityId", activityId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
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
            Log.e("Firestore", "Error loading logTimes for activity in current month", e)
            Result.failure(e)
        }
    }

    suspend fun createGoalDetailsForCurrentMonthIfNeeded(userId: String) {
        val headersResult = getGoalHeaderForUser(userId)
        if (headersResult.isFailure) {
            Log.e("Goal", "Failed to get GoalHeaders for userId: $userId", headersResult.exceptionOrNull())
            return
        }

        val headers = headersResult.getOrNull() ?: return
        val (startOfMonth, endOfMonth) = DataHelper.getCurrentMonthRange()

        headers.filter { it.goalType == "monthly" }.forEach { header ->
            Log.d("Goal", "Checking Monthly GoalHeader with goalId: ${header.goalId}")

            val detailResult = getGoalDetailForMonth(header, startOfMonth, endOfMonth)
            val existingDetail = detailResult.getOrNull()
            if (existingDetail != null) {
                Log.d("Goal", "Monthly GoalDetail already exists for goalId ${header.goalId}.")
                return@forEach
            }

            val latestDetailResult = getLatestGoalDetailForMonth(header)
            if (latestDetailResult.isFailure) {
                Log.e("Goal", "Failed to get latest GoalDetail for goalId: ${header.goalId}", latestDetailResult.exceptionOrNull())
            }
            val targetDuration = latestDetailResult.getOrNull()?.targetDuration ?: 0L
            Log.d("Goal", "targetDuration for goalId ${header.goalId}: $targetDuration")

            val logsResult = getLogTimesForActivityInMonth(header.activityId, userId, startOfMonth, endOfMonth)
            if (logsResult.isFailure) {
                Log.e("Goal", "Failed to get logTimes for activityId: ${header.activityId}", logsResult.exceptionOrNull())
                return@forEach
            }

            val logs = logsResult.getOrNull() ?: return@forEach
            val currentDuration = logs.sumOf { it.duration }
            Log.d("Goal", "currentDuration for goalId ${header.goalId}: $currentDuration")

            val newDetailRef = goalDetailCollection.document()

            val newDetail = GoalDetail(
                goalDetailId = newDetailRef.id,
                goalId = header.goalId,
                targetDuration = targetDuration,
                currentDuration = currentDuration,
                startDate = startOfMonth,
                endDate = endOfMonth,
                completeStatus = currentDuration >= targetDuration
            )

            try {
                newDetailRef.set(newDetail).await()
                Log.d("Goal", "Created new Monthly GoalDetail for goalId ${header.goalId}")
            } catch (e: Exception) {
                Log.e("Goal", "Failed to create Monthly GoalDetail for goalId ${header.goalId}", e)
            }
        }
    }

    suspend fun deleteGoalWithDetails(goalId: String): Result<Boolean> {
        return try {
            val detailQuery = goalDetailCollection
                .whereEqualTo("goalId", goalId)
                .get()
                .await()

            val batch = db.batch()
            detailQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.delete(goalHeaderCollection.document(goalId))

            batch.commit().await()

            Log.d("GoalRepo", "Successfully deleted goal $goalId and its details")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("GoalRepo", "Failed to delete goal $goalId and its details", e)
            Result.failure(e)
        }
    }

    suspend fun deleteActivityWithRelatedGoals(activityId: String): Result<Boolean> {
        return try {
            val goalsQuery = goalHeaderCollection
                .whereEqualTo("activityId", activityId)
                .get()
                .await()

            val batch = db.batch()
            goalsQuery.documents.forEach { goalDoc ->
                val detailQuery = goalDetailCollection
                    .whereEqualTo("goalId", goalDoc.id)
                    .get()
                    .await()

                detailQuery.documents.forEach { detailDoc ->
                    batch.delete(detailDoc.reference)
                }

                batch.delete(goalDoc.reference)
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}