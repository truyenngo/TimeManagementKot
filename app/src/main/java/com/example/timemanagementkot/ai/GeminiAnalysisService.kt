package com.example.timemanagementkot.ai

import android.util.Log
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.data.model.TimeAdjustmentSuggestion
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GeminiAnalysisService(private val generativeModel: GenerativeModel) {
    private val TAG = "GeminiAnalysis"

    suspend fun analyzeTimeAdjustment(activity: ActivityModel, logs: List<LogTimeModel>): TimeAdjustmentSuggestion = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Bắt đầu phân tích activity: ${activity.title} (ID: ${activity.activityId})")
            Log.d(TAG, "Số lượng logs đầu vào: ${logs.size}")

            val prompt = buildPrompt(activity, logs)
            Log.d(TAG, "Prompt gửi đến Gemini:\n$prompt")

            val response = generativeModel.generateContent(prompt).text
                ?: throw Exception("Gemini trả về null")
            Log.d(TAG, "Phản hồi thô từ Gemini: $response")

            val suggestion = parseResponse(response)
            Log.d(TAG, "Kết quả phân tích:\n" +
                    "- Thời gian hiện tại: ${suggestion.currentStart} - ${suggestion.currentEnd}\n" +
                    "- Đề xuất mới: ${suggestion.suggestedStart} - ${suggestion.suggestedEnd}\n" +
                    "- Lý do: ${suggestion.reason}")

            suggestion
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình phân tích", e)
            throw Exception("Không thể phân tích: ${e.localizedMessage}")
        }
    }

    private fun buildPrompt(activity: ActivityModel, logs: List<LogTimeModel>): String {
        val logEntries = logs.takeLast(7).joinToString("\n") { log ->
            "- ${log.dayOfWeek}: ${log.actualStart.toDate().formatTime()} -> ${
                log.actualEnd.toDate().formatTime()
            }" +
                    " (${log.duration} phút, ${if (log.completeStatus) "✅" else "❌"})"
        }

        return """
        Bạn là trợ lý thông minh phân tích quản lý thời gian. Hãy đề xuất điều chỉnh dựa trên:
        
        ### THÔNG TIN HOẠT ĐỘNG:
        - Tên: ${sanitizeInput(activity.title)}
        - Kế hoạch hiện tại: ${activity.startTime.toDate().formatTime()} -> ${activity.endTime.toDate().formatTime()}
        - Ngày lặp lại: ${activity.repeatDays.joinToString()}
        
        ### LỊCH SỬ 7 NGÀY GẦN NHẤT (đã lọc analyzedByAI=false):
        $logEntries
        
        ### YÊU CẦU:
        1. Phân tích xu hướng thực tế so với kế hoạch
        2. Đề xuất khung giờ mới (HH:mm) nếu cần thiết
        3. Lý do ngắn gọn (< 20 từ)
        
        Chỉ trả về JSON theo mẫu sau, không thêm bất kỳ text nào khác:
        {
          "activityId": "${activity.activityId}",
          "activityTitle": "${sanitizeInput(activity.title)}",
          "currentStart": "${activity.startTime.toDate().formatTime()}",
          "currentEnd": "${activity.endTime.toDate().formatTime()}",
          "suggestedStart": "HH:mm",
          "suggestedEnd": "HH:mm",
          "reason": "Lý do"
        }
    """.trimIndent()
    }

    private fun parseResponse(rawResponse: String): TimeAdjustmentSuggestion {
        return try {
            Log.d(TAG, "Bắt đầu parse response")

            val cleanedResponse = rawResponse
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanedResponse)
            TimeAdjustmentSuggestion(
                activityId = json.getString("activityId"),
                activityTitle = json.getString("activityTitle"),
                currentStart = json.getString("currentStart"),
                currentEnd = json.getString("currentEnd"),
                suggestedStart = json.getString("suggestedStart"),
                suggestedEnd = json.getString("suggestedEnd"),
                reason = json.getString("reason")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse JSON response: ${e.message}\nResponse: $rawResponse")
            throw Exception("Lỗi phân tích phản hồi từ Gemini: ${e.message}", e)
        }
    }

}

private fun sanitizeInput(text: String): String {
    return text.replace("\"", "'")
}

private fun com.google.firebase.Timestamp.toDate(): Date {
    return Date(seconds * 1000)
}

private fun Date.formatTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(this)
}