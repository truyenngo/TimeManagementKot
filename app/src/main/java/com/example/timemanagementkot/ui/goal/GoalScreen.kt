package com.example.timemanagementkot.ui.goal

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.google.firebase.Timestamp
import java.util.Date
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.timemanagementkot.R
import com.example.timemanagementkot.data.model.GoalHeader
import com.example.timemanagementkot.data.model.GoalDetail
import com.example.timemanagementkot.data.model.GoalWithDetail
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GoalScreen(
    viewModel: GoalVM? = null,
    onGoalClick: (GoalWithDetail) -> Unit = {},
    onAddGoalClick: () -> Unit = {},
    onDeleteClick: (GoalHeader) -> Unit = {},
    onBack: () -> Unit
){
    val defaultActivityList = remember {
        listOf(
            ActivityModel(
                activityId = "1",
                userId = "user1",
                title = "Hoạt động mẫu 1",
                type = "Công việc",
                startTime = Timestamp(Date()),
                endTime = Timestamp(Date(System.currentTimeMillis() + 3600000)),
                repeatDays = listOf("Monday", "Wednesday"),
                pomodoroSettings = PomodoroSettings(true, 25, 5)
            ),
            ActivityModel(
                activityId = "2",
                userId = "user2",
                title = "Hoạt động mẫu 2",
                type = "Giải trí",
                startTime = Timestamp(Date()),
                endTime = Timestamp(Date(System.currentTimeMillis() + 1800000)),
                repeatDays = listOf("Tuesday", "Thursday"),
                pomodoroSettings = PomodoroSettings(true, 30, 10)
            )
        )
    }
    val defaultGoalList = remember {
        listOf(
            GoalWithDetail(
                header = GoalHeader(
                    goalId = "1",
                    userId = "user1",
                    goalTitle = "Mục tiêu mẫu 1",
                    activityId = "1",
                    goalType = "Weekly"
                ),
                detail = GoalDetail(
                    goalDetailId = "1",
                    goalId = "1",
                    startDate = Timestamp(Date()),
                    endDate = Timestamp(Date(System.currentTimeMillis() + 1800000)),
                    currentDuration = 3600000,
                    targetDuration = 7200000,
                    completeStatus = true
                )
            ),
            GoalWithDetail(
                header = GoalHeader(
                    goalId = "1",
                    userId = "user1",
                    goalTitle = "Mục tiêu mẫu 1",
                    activityId = "1",
                    goalType = "Weekly"
                ),
                detail = GoalDetail(
                    goalDetailId = "1",
                    goalId = "1",
                    startDate = Timestamp(Date()),
                    endDate = Timestamp(Date(System.currentTimeMillis() + 1800000)),
                    currentDuration = 3600000,
                    targetDuration = 7200000,
                    completeStatus = true
                )
            ),
        )
    }

    val defaultActivity= ActivityModel(
        activityId = "2",
        userId = "user2",
        title = "Hoạt động mẫu 2",
        type = "Giải trí",
        startTime = Timestamp(Date()),
        endTime = Timestamp(Date(System.currentTimeMillis() + 1800000)),
        repeatDays = listOf("Tuesday", "Thursday"),
        pomodoroSettings = PomodoroSettings(true, 30, 10)
    )

    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val goalType by finalViewModel?.goalType?.collectAsState() ?: remember { mutableStateOf("weekly") }
    val activityList by finalViewModel?.activities?.collectAsState() ?: remember { mutableStateOf(defaultActivityList) }
    val goalHeaderWithDetail by finalViewModel?.goalHeaderWithDetail?.collectAsState() ?: remember { mutableStateOf(defaultGoalList) }
    val showActivityDialog by finalViewModel?.showActivityDialog?.collectAsState() ?: remember { mutableStateOf(false) }
    val selectedActivity by finalViewModel?.selectedActivity?.collectAsState() ?: remember { mutableStateOf(defaultActivity) }
    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    val targetHour by finalViewModel?.targetHour?.collectAsState() ?: remember { mutableStateOf(0) }
    val targetMinute by finalViewModel?.targetMinute?.collectAsState() ?: remember { mutableStateOf(0) }

    val errorTime by finalViewModel?.errorTime?.collectAsState() ?: remember { mutableStateOf(null) }
    val errorMessage by finalViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }
    val createGoalSuccess by finalViewModel?.createGoalSuccess?.collectAsState() ?: remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(createGoalSuccess) {
        if (createGoalSuccess) {
            snackbarHostState.showSnackbar("Tạo mục tiêu thành công!")
            finalViewModel?.resetCreateGoalStatus()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier.fillMaxSize()
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD58099)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mục tiêu",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFD58099))
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterButton("weekly", "Tuần", goalType == "weekly") {
                        finalViewModel?.setGoalType("weekly")
                    }
                    FilterButton("monthly", "Tháng", goalType == "monthly") {
                        finalViewModel?.setGoalType("monthly")
                    }
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
                    } else if (!isLoading && goalHeaderWithDetail.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Chưa có mục tiêu nào", style = MaterialTheme.typography.bodyLarge)
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
                                items(goalHeaderWithDetail) { goal ->
                                    GoalHeaderItem(
                                        goal,
                                        onGoalClick,
                                        onDeleteClick = { goalToDelete ->
                                            finalViewModel?.deleteGoal(goalToDelete.header.goalId)
                                        },
                                        showSnackbar = showSnackbar
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
                ){
                    Button(
                        onClick = {onBack()},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Thoát")
                    }

                    Button(
                        onClick = { finalViewModel?.showDialog()},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Thêm mục tiêu")
                    }
                }
            }
        }
    )


    if (showActivityDialog) {
        AlertDialog(
            onDismissRequest = { finalViewModel?.dismissDialog() },
            title = {
                Text(
                    text = "Chọn một hoạt động",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column (

                ){
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(activityList) { activity ->
                            Divider()
                            ActivityItem(
                                activity = activity,
                                isSelected = selectedActivity?.activityId == activity.activityId,
                                onItemClick = { finalViewModel?.selectActivity(activity) }
                            )
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Nhập thời lượng mục tiêu đặt ra:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = targetHour.toString(),
                            onValueChange = {
                                val newHour = it.toIntOrNull() ?: 0
                                if (it.isEmpty() || newHour >= 0) {
                                    finalViewModel?.onTargetHourChanged(newHour)
                                }
                            },
                            label = { Text("Giờ") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD58099),
                                unfocusedBorderColor = Color(0xFFD58099)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = targetMinute.toString(),
                            onValueChange = {
                                val newMinute = it.toIntOrNull() ?: 0
                                finalViewModel?.onTargetMinuteChanged(newMinute)
                            },
                            label = { Text("Phút") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD58099),
                                unfocusedBorderColor = Color(0xFFD58099)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(
                        modifier = Modifier.height(50.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!errorTime.isNullOrEmpty() || !errorMessage.isNullOrEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                errorTime?.let {
                                    if (it.isNotEmpty()) {
                                        Text(
                                            text = it,
                                            color = Color.Red,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                errorMessage?.let {
                                    if (it.isNotEmpty()) {
                                        Text(
                                            text = it,
                                            color = Color.Red,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.fillMaxHeight())
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { finalViewModel?.dismissDialog() }
                    ) {
                        Text("Thoát")
                    }

                    Button(
                        onClick = {
                            selectedActivity?.let {
                                finalViewModel?.createGoalHeaderWithDetail()
                            }
                        },
                        enabled = selectedActivity != null
                    ) {
                        Text("Tạo")
                    }
                }
            }
        )
    }
}

@Composable
fun FilterButton(
    type: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFD58099) else Color.LightGray
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.DarkGray
        )
    }
}

@Composable
private fun GoalHeaderItem(
    goal: GoalWithDetail,
    onGoalClick: (GoalWithDetail) -> Unit,
    onDeleteClick: (GoalWithDetail) -> Unit,
    showSnackbar: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val progressPercentage = if (goal.detail.targetDuration > 0)
        (goal.detail.currentDuration.toFloat() / goal.detail.targetDuration.toFloat()) * 100f
    else 0f

    val targetHour = goal.detail.targetDuration / 1000 / 60 / 60
    val targetMinute = (goal.detail.targetDuration / 1000 / 60) % 60

    val currentHour = goal.detail.currentDuration / 1000 / 60 / 60
    val currentMinute = (goal.detail.currentDuration / 1000 / 60) % 60

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xoá") },
            text = { Text("Bạn có chắc chắn muốn xoá hoạt động này không?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(goal)
                    showDeleteDialog = false
                    showSnackbar("Xóa thành công")
                }) {
                    Text("Xoá", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Huỷ")
                }
            }
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable { onGoalClick(goal) }
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
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
                        Column {
                            Text(
                                text = goal.header.goalTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFFD58099)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Bắt đầu: ${timestampToDateString(goal.detail.startDate)}",
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                "Kết thúc: ${timestampToDateString(goal.detail.endDate)}",
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        Column (
                            modifier = Modifier.width(70.dp)
                        ){
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .size(70.dp)
                                    .padding(5.dp)
                                    .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Xoá hoạt động",
                                    modifier = Modifier
                                        .size(70.dp)
                                        .padding(5.dp)
                                        .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tiến độ hoàn thành:",
                            fontSize = 16.sp
                        )
                        Text(
                            "${progressPercentage.toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD58099),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    LinearProgressIndicator(
                        progress = progressPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = if (progressPercentage >= 100f) Color.Green else Color(0xFFD58099),
                        trackColor = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Đã làm: ${currentHour}h ${currentMinute}m / Mục tiêu: ${targetHour}h ${targetMinute}m",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: ActivityModel,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .background(if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent)
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD58099).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp
            )
            Text(
                text = "Loại: ${activity.type}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

fun timestampToDateString(timestamp: Timestamp): String {
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return dateFormat.format(timestamp.toDate())
}

@Preview(showBackground = true)
@Composable
fun GoalScreenPreview() {
    GoalScreen(
        onGoalClick = {},
        onAddGoalClick = {},
        onDeleteClick = {},
        onBack = {}
    )
}