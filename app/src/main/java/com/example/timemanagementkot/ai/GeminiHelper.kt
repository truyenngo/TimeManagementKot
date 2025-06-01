package com.example.timemanagementkot.ai

import android.content.Context
import android.util.Log
import com.example.timemanagementkot.util.Config
import com.google.ai.client.generativeai.GenerativeModel

object GeminiHelper {
    private const val TAG = "GeminiHelper"
    private const val MODEL_NAME = "gemini-1.5-pro"

    fun getGenerativeModel(context: Context): GenerativeModel {
        return try {
            Log.d(TAG, "Đang khởi tạo GenerativeModel...")

            val apiKey = Config.getGeminiApiKey(context)
            Log.d(TAG, "Đã nhận API Key (masked): ${maskApiKey(apiKey)}")

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )

            Log.d(TAG, "Khởi tạo GenerativeModel thành công")
            model
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khởi tạo GenerativeModel: ${e.message}")
            throw Exception("Failed to initialize Gemini Model", e)
        }
    }

    private fun maskApiKey(key: String): String {
        return if (key.length > 4) "****${key.takeLast(4)}" else "****"
    }
}