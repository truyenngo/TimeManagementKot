package com.example.timemanagementkot.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timemanagementkot.data.repo.ActivityRepo
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.notifications.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver để lên lịch lại các thông báo khi thiết bị khởi động lại
 * Đảm bảo các thông báo vẫn hoạt động sau khi reboot
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Nhận được broadcast: ${intent?.action}")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Xử lý khởi động lại thiết bị...")

            try {
                // 1. Kiểm tra và lấy userId
                val authRepo = AuthRepo()
                val userId = authRepo.getCurrentUser()?.uid ?: run {
                    Log.w(TAG, "Không tìm thấy user đang đăng nhập")
                    return
                }
                Log.d(TAG, "User ID: $userId")

                // 2. Lấy danh sách activities từ repository
                val repo = ActivityRepo()
                CoroutineScope(Dispatchers.IO).launch {
                    val activities = repo.getActivitiesByUserId(userId).getOrElse {
                        Log.e(TAG, "Lỗi khi lấy activities", it)
                        emptyList()
                    }

                    Log.d(TAG, "Tìm thấy ${activities.size} activities cần lên lịch lại")

                    // 3. Lên lịch lại từng activity
                    val scheduler = AlarmScheduler(context)
                    activities.forEach { activity ->
                        try {
                            scheduler.schedule(activity)
                            Log.d(TAG, "Đã lên lịch lại thành công cho: ${activity.title}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Lỗi khi lên lịch cho ${activity.title}", e)
                        }
                    }

                    Log.d(TAG, "Hoàn thành lên lịch lại tất cả activities")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi trong quá trình xử lý boot completed", e)
            }
        }
    }
}