package com.example.timemanagementkot.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.timemanagementkot.R
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.ui.MainActivity

/**
 * Helper class để quản lý tất cả các thông báo trong ứng dụng
 * Xử lý tạo kênh thông báo và hiển thị các loại thông báo khác nhau
 */
class NotificationHelper(private val context: Context) {
    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "activity_channel"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        Log.d(TAG, "Khởi tạo NotificationHelper")
        createNotificationChannel()
    }

    /**
     * Tạo kênh thông báo (bắt buộc cho Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "Đang tạo notification channel...")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your scheduled activities"
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Đã tạo thành công kênh thông báo: $CHANNEL_ID")
        } else {
            Log.d(TAG, "Không cần tạo channel (API < 26)")
        }
    }

    /**
     * Hiển thị thông báo test (dùng để debug)
     */
    fun showTestNotification() {
        Log.d(TAG, "Chuẩn bị hiển thị thông báo test...")
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(9999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị test notification", e)
        }
    }

    /**
     * Hiển thị thông báo cho một hoạt động cụ thể
     * @param activity Hoạt động cần thông báo
     */
    fun showActivityNotification(activity: ActivityModel) {
        Log.d(TAG, "Chuẩn bị thông báo cho hoạt động: ${activity.title}")

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("activity_id", activity.activityId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                activity.activityId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏰ ${activity.title}")
                .setContentText("Đã đến giờ bắt đầu hoạt động: ${activity.title}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(100, 200, 300, 400))
                .build()

            notificationManager.notify(activity.activityId.hashCode(), notification)
            Log.d(TAG, "Đã hiển thị thông báo thành công cho: ${activity.title}")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị thông báo cho ${activity.title}", e)
        }
    }
}