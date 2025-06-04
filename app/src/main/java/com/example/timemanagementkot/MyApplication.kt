package com.example.timemanagementkot

import android.app.Application
import android.util.Log
import com.example.timemanagementkot.notifications.NotificationHelper
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    companion object {
        private const val TAG = "AppInit"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Ứng dụng đang khởi tạo...")

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase đã sẵn sàng")

            NotificationHelper(this)
            Log.d(TAG, "Đã tạo kênh thông báo")

        } catch (e: Exception) {
            Log.e(TAG, "LỖI KHI KHỞI TẠO ỨNG DỤNG", e)
        }

        Log.d(TAG, "Khởi tạo hoàn tất")
    }
}