package com.example.timemanagementkot.ui.detail_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.data.model.GoalDetail
import com.example.timemanagementkot.data.model.GoalHeader
import com.example.timemanagementkot.data.model.GoalWithDetail
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun DetailGoalScreen(
    goal: GoalWithDetail,
    viewModel: DetailGoalVM? = null,
    onNavigationToList: () -> Unit = {},
    onSaveClick: () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val activityTitle by finalViewModel?.activityTitle?.collectAsState() ?: remember { mutableStateOf("") }

    val targetHour by finalViewModel?.targetHour?.collectAsState() ?: remember { mutableStateOf(0) }
    val targetMinute by finalViewModel?.targetMinute?.collectAsState() ?: remember { mutableStateOf(0) }
    val currentHour by finalViewModel?.currentHour?.collectAsState() ?: remember { mutableStateOf(0) }
    val currentMinute by finalViewModel?.currentMinute?.collectAsState() ?: remember { mutableStateOf(0) }

    val startDate by finalViewModel?.startDate?.collectAsState() ?: remember { mutableStateOf("") }
    val endDate by finalViewModel?.endDate?.collectAsState() ?: remember { mutableStateOf("") }
    val goalType by finalViewModel?.goalType?.collectAsState() ?: remember { mutableStateOf("") }
    val progressPercentage by finalViewModel?.progressPercentage?.collectAsState() ?: remember { mutableStateOf(0f) }

    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val saveSuccess by finalViewModel?.saveSuccess?.collectAsState() ?: remember { mutableStateOf(false) }
    val errorTime by finalViewModel?.errorTime?.collectAsState() ?: remember { mutableStateOf(null) }
    val errorMessage by finalViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }


    LaunchedEffect(goal) {
        finalViewModel?.setGoalValue(goal)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showSnackbar("Cập nhật mục tiêu thành công!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD58099)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chi tiết mục tiêu",
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column {
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
                                    finalViewModel?.onTargetHourChanged(newHour)
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

                            Button(
                                onClick = {
                                    finalViewModel?.confirmTargetDuration()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                                modifier = Modifier
                            ) {
                                Text("Xác nhận", color = Color.White)
                            }
                        }
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
                    Divider(color = Color(0xFFD58099))
                    Spacer(modifier = Modifier.height(20.dp))

                    Column {
                        Text(
                            text = "Thông tin chi tiết:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = activityTitle,
                            onValueChange = {},
                            label = { Text("Tên mục tiêu hoạt động", fontSize = 16.sp) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Đã thực hiện được:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = Color.Black
                            )

                            OutlinedTextField(
                                value = currentHour.toString(),
                                onValueChange = {  },
                                label = { Text("Giờ", fontSize = 16.sp) },
                                readOnly = true,
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black
                                )
                            )
                            OutlinedTextField(
                                value = currentMinute.toString(),
                                onValueChange = {  },
                                label = { Text("Phút", fontSize = 16.sp) },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = {},
                                label = { Text("Ngày bắt đầu", fontSize = 16.sp) },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black
                                )
                            )

                            OutlinedTextField(
                                value = endDate,
                                onValueChange = {},
                                label = { Text("Ngày kết thúc", fontSize = 16.sp) },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = goalType,
                            onValueChange = {},
                            label = { Text("Loại mục tiêu", fontSize = 16.sp) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color(0xFFD58099))
                    Spacer(modifier = Modifier.height(20.dp))

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
                    } else {
                        Column (
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Tiến độ hoàn thành:",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "${progressPercentage.toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD58099),
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            LinearProgressIndicator(
                                progress = progressPercentage / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                                color = if (progressPercentage >= 100f) Color.Green else Color(0xFFD58099),
                                trackColor = Color.LightGray
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Đã làm: ${currentHour}h ${currentMinute}m / Mục tiêu: ${targetHour}h ${targetMinute}m",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
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
                    Button(
                        onClick = { onNavigationToList() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Thoát")
                    }

                    Button(
                        onClick = { onSaveClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Lưu mục tiêu")
                    }
                }
            }
        }
    )

}

@Preview(showBackground = true)
@Composable
fun DetailGoalScreenPreview() {
    val sampleGoal = GoalWithDetail(
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
    )

    MaterialTheme {
        DetailGoalScreen(
            goal = sampleGoal,
            onNavigationToList = {},
            onSaveClick = {}
        )
    }
}
