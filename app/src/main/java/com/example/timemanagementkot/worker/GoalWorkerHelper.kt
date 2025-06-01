package com.example.timemanagementkot.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest

object GoalWorkerHelper {
    fun enqueueGoalWorkerIfNeeded(userId: String, context: Context) {
        val inputData = workDataOf("userId" to userId)

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<CreateGoalDetailsWorker>()
            .setInputData(inputData)
            .build()

        val uniqueWorkName = "CheckAndCreateGoalDetail_$userId"

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}