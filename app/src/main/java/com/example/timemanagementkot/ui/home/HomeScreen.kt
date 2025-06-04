package com.example.timemanagementkot.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.R
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.example.timemanagementkot.data.model.TimeAdjustmentSuggestion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeVM? = null,
    onActivityListClick: () -> Unit = {},
    onTargetClick: () -> Unit = {},
    onStatisticClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
    onActivityClick: (ActivityWithLogTime) -> Unit = {}
) {
    val defaultActivityList = remember {
        listOf(
            ActivityWithLogTime(
                activity = ActivityModel(
                    activityId = "1",
                    userId = "user1",
                    title = "Hoạt động mẫu 1",
                    type = "Công việc",
                    startTime = Timestamp(Date()),
                    endTime = Timestamp(Date(System.currentTimeMillis() + 3600000)),
                    repeatDays = listOf("Monday", "Wednesday"),
                    pomodoroSettings = PomodoroSettings(true, 25, 5)
                ),
                completeStatus = false
            ),
            ActivityWithLogTime(
                activity = ActivityModel(
                    activityId = "2",
                    userId = "user2",
                    title = "Hoạt động mẫu 2",
                    type = "Giải trí",
                    startTime = Timestamp(Date()),
                    endTime = Timestamp(Date(System.currentTimeMillis() + 1800000)),
                    repeatDays = listOf("Tuesday", "Thursday"),
                    pomodoroSettings = PomodoroSettings(true, 30, 10)
                ),
                completeStatus = true
            )
        )
    }
    val defaultCurrentDate = remember { "Thứ 2, 06/05/2025" }
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null
    val currentDate by finalViewModel?.currentDateState?.collectAsState() ?: remember { mutableStateOf(defaultCurrentDate) }
    val activityListWithLogTime by finalViewModel?.activities?.collectAsState() ?: remember { mutableStateOf(defaultActivityList) }
    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val isCurrentDate by finalViewModel?.isCurrentDate?.collectAsState() ?: remember { mutableStateOf(false) }

    val appContext = LocalContext.current.applicationContext
    val userId = if (!isInPreview) FirebaseAuth.getInstance().currentUser?.uid else "preview-user-id"

    val showSuggestions by finalViewModel?.showSuggestions?.collectAsState() ?: remember { mutableStateOf(false) }
    val suggestions by finalViewModel?.suggestions?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val selectedSuggestion by finalViewModel?.selectedSuggestion?.collectAsState() ?: remember { mutableStateOf(null) }
    val autoAnalysisTriggered by finalViewModel?.autoAnalysisTriggered?.collectAsState() ?: remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        finalViewModel?.initialize(context)
        finalViewModel?.startAutoAnalysisMonitoring(userId?:"")
    }

    if (autoAnalysisTriggered) {
        Log.d("HomeScreen","Da phan tich thanh cong du lieu")
    }

    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            finalViewModel?.checkAndCreateGoalDetailIfNeeded(userId, appContext)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD58099)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(60.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { finalViewModel?.onPreviousDay() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_previous),
                                contentDescription = "Mô tả",
                                tint = Color.Unspecified
                            )
                        }

                        Text(
                            text = currentDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD7566A),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { finalViewModel?.onNextDay() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_next),
                                contentDescription = "Mô tả",
                                tint = Color.Unspecified
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            finalViewModel?.toggleSuggestions()
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = "Notification",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .padding(8.dp)
                                .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
                        )
                    }
                }

                if (showSuggestions) {
                    SuggestionsDialog(
                        suggestions = suggestions,
                        selectedSuggestion = selectedSuggestion,
                        onDismiss = {
                            finalViewModel?.hideSuggestions()
                            finalViewModel?.clearSelectedSuggestion()
                        },
                        onSelect = { suggestion ->
                            finalViewModel?.selectSuggestion(suggestion)
                        },
                        onApply = { suggestion ->
                            finalViewModel?.applySuggestion()
                            finalViewModel?.clearSelectedSuggestion()
                            showSnackbar("Áp dụng gợi ý thành công")
                        },
                        onDelete = { suggestion ->
                            finalViewModel?.deleteSuggestion()
                            finalViewModel?.clearSelectedSuggestion()
                            showSnackbar("Đã xóa gợi ý")
                        }
                    )
                }

                Column (
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp),
                                color = Color(0xFFD58099)
                            )
                        }
                    } else if (!isLoading && activityListWithLogTime.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Chưa có hoạt động nào", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Box(modifier = Modifier
                            .fillMaxSize()
                        ){
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 12.dp, end = 12.dp)
                            ) {
                                items(activityListWithLogTime) { activityWithLogTime ->
                                    ActivityItem(
                                        activityWithLogTime,
                                        onActivityClick,
                                        showSnackbar = showSnackbar,
                                        isCurrentDate = isCurrentDate
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD58099)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem(iconId = R.drawable.ic_activity_list, label = "Danh sách", onClick = onActivityListClick)
                    BottomNavItem(iconId = R.drawable.ic_target, label = "Mục tiêu", onClick = onTargetClick)
                    BottomNavItem(iconId = R.drawable.ic_statistic, label = "Thống kê", onClick = onStatisticClick)
                    BottomNavItem(iconId = R.drawable.ic_setting, label = "Cài đặt", onClick = onSettingClick)
                }
            }
        }
    )
}

@Composable
fun BottomNavItem(iconId: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = label,
            modifier = Modifier
                .size(70.dp)
                .padding(5.dp)
                .border(2.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActivityItem(
    activityWithLogTime: ActivityWithLogTime,
    onActivityClick: (ActivityWithLogTime) -> Unit,
    showSnackbar: (String) -> Unit,
    isCurrentDate: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable {
                if (activityWithLogTime.completeStatus) {
                    showSnackbar("Hoạt động đã được ghi lại trước đó")
                } else if (!isCurrentDate) {
                    showSnackbar("Không thể ghi log vì ngày không hợp lệ")
                } else {
                    onActivityClick(activityWithLogTime)
                }
            }
            .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD58099).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    Text(
                        text = activityWithLogTime.activity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFD58099)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Bắt đầu: ${formatTimeToHHmm(activityWithLogTime.activity.startTime)}")

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Kết thúc: ${formatTimeToHHmm(activityWithLogTime.activity.endTime)}")

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Loại: ${activityWithLogTime.activity.type}")

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Lặp lại: ${activityWithLogTime.activity.repeatDays.joinToString(", ")}")

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = if (activityWithLogTime.completeStatus) "Đã hoàn thành" else "Chưa hoàn thành",
                        color = if (activityWithLogTime.completeStatus) Color.Green else Color.Red
                    )
                }
            }
        }
    }
}

fun formatTimeToHHmm(timestamp: Timestamp): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp.seconds * 1000
    }
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

@Composable
fun SuggestionsDialog(
    suggestions: List<TimeAdjustmentSuggestion>,
    selectedSuggestion: TimeAdjustmentSuggestion?,
    onDismiss: () -> Unit,
    onSelect: (TimeAdjustmentSuggestion) -> Unit,
    onApply: (TimeAdjustmentSuggestion) -> Unit,
    onDelete: (TimeAdjustmentSuggestion) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gợi ý điều chỉnh thời gian",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                if (suggestions.isEmpty()) {
                    Text("Không có gợi ý nào")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(suggestions) { suggestion ->
                            val isSelected = selectedSuggestion == suggestion

                            Column(
                                modifier = Modifier
                                    .clickable { onSelect(suggestion) }
                                    .background(
                                        if (isSelected) Color.LightGray.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                SuggestionItem(suggestion)

                                if (isSelected) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { onDelete(suggestion) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = Color.Red
                                            )
                                        ) {
                                            Text("Xóa")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onApply(suggestion) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD58099)
                                            )
                                        ) {
                                            Text("Áp dụng")
                                        }
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun SuggestionItem(suggestion: TimeAdjustmentSuggestion) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Divider()
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(
                text = suggestion.activityTitle,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD58099)
            )
            Text("${suggestion.timeRequest}")
        }

        Column (
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ){
            Text("Hiện tại: ${suggestion.currentStart} - ${suggestion.currentEnd}")
            Text("Đề xuất: ${suggestion.suggestedStart} - ${suggestion.suggestedEnd}")
            Text(
                text = "Lý do: ${suggestion.reason}",
                fontStyle = FontStyle.Italic,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onActivityListClick = {},
        onTargetClick = {},
        onStatisticClick = {},
        onSettingClick = {},
        onActivityClick = { activityWithLogTime ->

        }
    )
}

