package com.example.timemanagementkot.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Properties

object Config {
    private const val TAG = "Config"
    private const val KEY_GEMINI_API = "gemini.api.key"

    fun getGeminiApiKey(context: Context): String {
        return try {
            Log.d(TAG, "Đang đọc API key từ local.properties")

            val properties = Properties().apply {
                load(context.assets.open("local.properties"))
            }

            val apiKey = properties.getProperty(KEY_GEMINI_API)
                ?: throw Exception("Missing Gemini API Key in local.properties")

            // Log mask key để bảo mật (chỉ hiển thị 4 ký tự cuối)
            Log.d(TAG, "Đọc API key thành công: ${maskApiKey(apiKey)}")

            apiKey
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đọc API key: ${e.message}")
            throw Exception("Failed to load Gemini API Key", e)
        }
    }

    private fun maskApiKey(key: String): String {
        return if (key.length > 4) "****${key.takeLast(4)}" else "****"
    }
}