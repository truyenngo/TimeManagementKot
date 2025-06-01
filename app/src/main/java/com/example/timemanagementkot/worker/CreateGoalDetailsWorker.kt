package com.example.timemanagementkot.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timemanagementkot.data.repo.GoalRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateGoalDetailsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        Log.e("GoalWorker", "Running weekly goal detail worker for userId = $userId")
        return try {
            withContext(Dispatchers.IO) {
                GoalRepo().createGoalDetailsForCurrentWeekIfNeeded(userId)
                GoalRepo().createGoalDetailsForCurrentMonthIfNeeded(userId)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}