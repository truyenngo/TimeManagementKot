package com.example.timemanagementkot.ui.log_time

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.google.firebase.Timestamp
import androidx.compose.runtime.setValue
import com.example.timemanagementkot.data.model.ActivityWithLogTime
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTimeScreen(
    activityWithLogJson: ActivityWithLogTime,
    viewModel: LogTimeVM? = null,
    onBack: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val defaultCurrentDate = remember { "Thứ 2, 06/05/2025" }
    val defaultTitle = remember { "Hoạt động mẫu" }
    val defaultStartTime = remember { "08:00" }
    val defaultEndTime = remember { "09:00" }
    val defaultElapsedFormatted = remember { "00:00:00" }
    val defaultPomoClock = remember { "00:00" }
    val defaultActualStart = remember { null }
    val defaultActualEnd = remember { null }
    val defaultIsPomodoro = remember { true }
    val defaultFocusMinutes = remember { 25 }
    val defaultBreakMinutes = remember { 5 }

    val currentDate by finalViewModel?.currentDateState?.collectAsState() ?: remember { mutableStateOf(defaultCurrentDate) }
    val title by finalViewModel?.titleState?.collectAsState() ?: remember { mutableStateOf(defaultTitle) }
    val startTime by finalViewModel?.startTimeState?.collectAsState() ?: remember { mutableStateOf(defaultStartTime) }
    val endTime by finalViewModel?.endTimeState?.collectAsState() ?: remember { mutableStateOf(defaultEndTime) }
    val elapsedFormatted by finalViewModel?.elapsedFormatted?.collectAsState() ?: remember { mutableStateOf(defaultElapsedFormatted) }
    val pomoClock by finalViewModel?.pomoClockState?.collectAsState() ?: remember { mutableStateOf(defaultPomoClock)}
    val actualStartTime by finalViewModel?.actualStartState?.collectAsState() ?: remember { mutableStateOf(defaultActualStart) }
    val actualEndTime by finalViewModel?.actualEndState?.collectAsState() ?: remember { mutableStateOf(defaultActualEnd) }
    val pomodoroEnable by finalViewModel?.pomodoroEnableState?.collectAsState() ?: remember { mutableStateOf(defaultIsPomodoro) }
    val focusMinutes by finalViewModel?.focusMinutesState?.collectAsState() ?: remember { mutableIntStateOf(defaultFocusMinutes) }
    val breakMinutes by finalViewModel?.breakMinutesState?.collectAsState() ?: remember { mutableIntStateOf(defaultBreakMinutes) }
    val isFocusPhase by finalViewModel?.isFocusPhase?.collectAsState() ?: remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }
    var hasEnded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(activityWithLogJson) {
        finalViewModel?.setLogTimeDefault(activityWithLogJson)
    }

    fun handleSave(){
        coroutineScope.launch {
            finalViewModel?.saveLogTime()
            showSnackbar("Ghi lại thời gian thành công")
            kotlinx.coroutines.delay(1000)
            onComplete()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "Ghi nhận thời gian: $currentDate",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD7566A),
                        textAlign = TextAlign.Center
                    )
                }

                Column (
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ){
                    Column (
                        modifier = Modifier
                            .padding(10.dp)
                    ){
                        Text(
                            text = "Hoạt động: $title",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Dự kiến: $startTime - $endTime",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = elapsedFormatted,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    finalViewModel?.startTimer()
                                    finalViewModel?.onStartButtonClick()
                                    finalViewModel?.startPomodoroCycle(focusMinutes, breakMinutes)
                                    hasStarted = true
                                    hasEnded = false
                                },
                                enabled = !hasStarted && !hasEnded
                            ) {
                                Text("Bắt đầu")
                            }

                            Button(
                                onClick = {
                                    finalViewModel?.stopTimer()
                                    finalViewModel?.onEndButtonClick()
                                    finalViewModel?.stopPomodoroTimer()
                                    hasEnded = true
                                    hasStarted = false
                                },
                                enabled = hasStarted && !hasEnded
                            ) {
                                Text("Kết thúc")
                            }
                        }

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Đã bắt đầu vào: ${finalViewModel?.formatTimeStampToHHmmss(actualStartTime)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Đã kết thúc vào: ${finalViewModel?.formatTimeStampToHHmmss(actualEndTime)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Column (
                        modifier = Modifier
                            .padding(10.dp)
                    ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Chế độ Pomodoro")
                            Switch(
                                checked = pomodoroEnable,
                                onCheckedChange = {finalViewModel?.onPomodoroToggle(it)},
                                enabled = finalViewModel != null
                            )
                        }

                        if (pomodoroEnable){
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedTextField(
                                    value = focusMinutes.toString(),
                                    onValueChange = { finalViewModel?.onFocusMinutesChange(it.toIntOrNull() ?: 0) },
                                    label = { Text("Phút tập trung") },
                                    enabled = finalViewModel != null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(130.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isFocusPhase) Color.Green else Color.Black,
                                        unfocusedBorderColor = if (isFocusPhase) Color.Green else Color.Black
                                    )

                                )

                                OutlinedTextField(
                                    value = breakMinutes.toString(),
                                    onValueChange = { finalViewModel?.onBreakMinutesChange(it.toIntOrNull() ?: 0) },
                                    label = { Text("Phút nghỉ") },
                                    enabled = finalViewModel != null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(130.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (!isFocusPhase) Color.Yellow else Color.Black,
                                        unfocusedBorderColor = if (!isFocusPhase) Color.Yellow else Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .border(
                                        width = 2.dp,
                                        color = when {
                                            hasStarted && isFocusPhase -> Color.Green
                                            hasStarted && !isFocusPhase -> Color.Yellow
                                            else -> Color.Gray
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = pomoClock,
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.Black
                                )
                            }


                            Spacer(modifier = Modifier.height(32.dp))

                        }
                    }
                }

                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Button(
                        onClick = {
                            if (hasStarted) {
                                showSnackbar("Đang ghi lại thời gian, không thể thoát")
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .width(160.dp)
                            .padding(10.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFD58099))
                    ) {
                        Text("Thoát")
                    }

                    Button(
                        onClick = {
                            if (hasStarted) {
                                showSnackbar("Đang ghi lại thời gian, không thể lưu")
                            } else {
                                handleSave()
                            }
                        },
                        modifier = Modifier
                            .width(160.dp)
                            .padding(10.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFD58099))
                    ) {
                        Text("Hoàn thành")
                    }
                }
            }
        }
    )


}

@Preview(showBackground = true)
@Composable
fun LogTimeScreenPreview() {
    val startTime = Timestamp.now()

    val sampleActivity = ActivityWithLogTime(
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

    MaterialTheme {
        LogTimeScreen(
            activityWithLogJson = sampleActivity,
            onBack = {},
            onComplete = {}
        )
    }
}
