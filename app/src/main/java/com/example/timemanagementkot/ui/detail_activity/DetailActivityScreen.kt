package com.example.timemanagementkot.ui.detail_activity

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.ActivityType
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.google.firebase.Timestamp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailActivityScreen(
    activity: ActivityModel,
    viewModel: DetailActivityVM? = null,
    onNavigationToList: () -> Unit = {},
    onUpdateClick: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val defaultTitle = remember { "Hoạt động mẫu" }
    val defaultStartTime = remember { "08:00" }
    val defaultEndTime = remember { "09:00" }
    val defaultSelectedDays = remember { setOf("T2", "T4") }
    val defaultIsPomodoro = remember { true }
    val defaultFocusMinutes = remember { 25 }
    val defaultBreakMinutes = remember { 5 }
    val defaultTypeExpanded = remember { false }
    val defaultRepeatDays = remember { listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") }

    val defaultSelectedType = remember { ActivityType(typeName = "Công việc") }
    val defaultTypes = remember { listOf(
        ActivityType(typeName = "Công việc"),
        ActivityType(typeName = "Giải trí"),
        ActivityType(typeName = "Học tập")
    ) }
    val defaultShowAddTypeDialog = remember { false }
    val defaultShowDeleteConfirmDialog = remember { false }
    val defaultTypeToDelete = remember { ActivityType(typeName = "") }

    val title by finalViewModel?.titleState?.collectAsState() ?: remember { mutableStateOf(defaultTitle) }
    val startTime by finalViewModel?.startTimeState?.collectAsState() ?: remember { mutableStateOf(defaultStartTime) }
    val endTime by finalViewModel?.endTimeState?.collectAsState() ?: remember { mutableStateOf(defaultEndTime) }
    val selectedDays by finalViewModel?.selectedDaysState?.collectAsState() ?: remember { mutableStateOf(defaultSelectedDays) }
    val pomodoroEnable by finalViewModel?.pomodoroEnableState?.collectAsState() ?: remember { mutableStateOf(defaultIsPomodoro) }
    val focusMinutes by finalViewModel?.focusMinutesState?.collectAsState() ?: remember { mutableStateOf(defaultFocusMinutes) }
    val breakMinutes by finalViewModel?.breakMinutesState?.collectAsState() ?: remember { mutableStateOf(defaultBreakMinutes) }
    val typeExpanded by finalViewModel?.typeExpandedState?.collectAsState() ?: remember { mutableStateOf(defaultTypeExpanded) }
    val repeatDays = finalViewModel?.repeatDays ?: defaultRepeatDays
    val isEditing by finalViewModel?.isEditingState?.collectAsState() ?: remember { mutableStateOf(false) }

    val selectedType by finalViewModel?.selectedTypeState?.collectAsState() ?: remember { mutableStateOf(defaultSelectedType) }
    val types by finalViewModel?.activityTypes?.collectAsState() ?: remember { mutableStateOf(defaultTypes) }
    val showAddTypeDialog by finalViewModel?.showAddTypeDialog?.collectAsState() ?: remember { mutableStateOf(defaultShowAddTypeDialog) }
    val showDeleteConfirmDialog by finalViewModel?.showDeleteConfirmDialog?.collectAsState() ?: remember { mutableStateOf(defaultShowDeleteConfirmDialog) }
    val typeToDelete by finalViewModel?.typeToDelete?.collectAsState() ?: remember { mutableStateOf(defaultTypeToDelete) }

    val snackbarHostState = remember { SnackbarHostState() }
    val calendar = remember { Calendar.getInstance() }

    val saveSuccessMessage by finalViewModel?.onSaveSuccess?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(saveSuccessMessage) {
        saveSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            finalViewModel?.resetSaveSuccess()
        }
    }

    val error by finalViewModel?.errorState?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            finalViewModel?.resetError()
        }
    }

    LaunchedEffect(activity) {
        finalViewModel?.setActivityDetails(activity)
    }

    fun handleUpdate() {
        finalViewModel?.updateCurrentActivity(context)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Text(
                text = if (isEditing) "Sửa hoạt động" else "Chi tiết hoạt động",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isEditing) Color(0xFFD58099) else Color(0xFF4CAF50))
                    .padding(16.dp)
            )

            Column(modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { finalViewModel?.onTitleChange(it) },
                    label = { Text("Tên hoạt động") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing && finalViewModel != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                val timePicker = { onTimeSet: (String) -> Unit ->
                    TimePickerDialog(
                        context,
                        { _: TimePicker, hour: Int, minute: Int ->
                            onTimeSet(String.format("%02d:%02d", hour, minute))
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { timePicker { finalViewModel?.onStartTimeChange(it) } },
                        enabled = isEditing && finalViewModel != null,
                        modifier = Modifier,
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isEditing && finalViewModel != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Giờ bắt đầu: $startTime")
                    }
                    OutlinedButton(
                        onClick = { timePicker { finalViewModel?.onEndTimeChange(it) } },
                        enabled = isEditing && finalViewModel != null,
                        modifier = Modifier,
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isEditing && finalViewModel != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Giờ kết thúc: $endTime")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Loại hoạt động")
                ExposedDropdownMenuBox(
                    expanded = isEditing && typeExpanded,
                    onExpandedChange = { if (isEditing) finalViewModel?.onTypeExpandedChange(!typeExpanded) }
                ) {
                    OutlinedTextField(
                        value = selectedType?.typeName ?: "Chưa xếp loại",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEditing && typeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = isEditing && finalViewModel != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { finalViewModel?.onTypeExpandedChange(false) }
                    ) {
                        if (types.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Chưa có loại hoạt động") },
                                onClick = {}
                            )
                        } else {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(type.typeName, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    finalViewModel?.prepareToDeleteType(type)
                                                    finalViewModel?.onTypeExpandedChange(false)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa",
                                                    tint = Color.Red
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        finalViewModel?.onSelectedTypeChange(type)
                                        finalViewModel?.onTypeExpandedChange(false)
                                    }
                                )
                            }
                        }

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Thêm loại mới", modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Thêm",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                finalViewModel?.onShowAddTypeDialogChange(true)
                                finalViewModel?.onTypeExpandedChange(false)
                            }
                        )
                    }
                }

                if (showAddTypeDialog) {
                    var newType by remember { mutableStateOf("") }
                    var isError by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = {
                            finalViewModel?.onShowAddTypeDialogChange(false)
                            isError = false
                        },
                        title = { Text("Thêm loại hoạt động") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = newType,
                                    onValueChange = {
                                        newType = it
                                        isError = false
                                    },
                                    label = { Text("Tên loại hoạt động") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = isError,
                                    supportingText = {
                                        if (isError) {
                                            Text(
                                                text = errorMessage,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    when {
                                        newType.isBlank() -> {
                                            isError = true
                                            errorMessage = "Tên loại không được để trống"
                                        }
                                        types.any { it.typeName.equals(newType, ignoreCase = true) } -> {
                                            isError = true
                                            errorMessage = "Tên loại đã tồn tại"
                                        }
                                        else -> {
                                            finalViewModel?.addNewActivityType(newType)
                                        }
                                    }
                                }
                            ) {
                                Text("Thêm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    finalViewModel?.onShowAddTypeDialogChange(false)
                                    isError = false
                                }
                            ) {
                                Text("Hủy")
                            }
                        }
                    )
                }

                if (showDeleteConfirmDialog) {
                    (typeToDelete as? ActivityType)?.let { type ->
                        AlertDialog(
                            onDismissRequest = { finalViewModel?.onShowDeleteConfirmDialogChange(false) },
                            title = { Text("Xác nhận xóa") },
                            text = {
                                Text("Bạn có chắc chắn muốn xóa loại hoạt động '${type.typeName}' không?")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        finalViewModel?.deleteActivityType(type)
                                        finalViewModel?.onShowDeleteConfirmDialogChange(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Xóa")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { finalViewModel?.onShowDeleteConfirmDialogChange(false) }
                                ) {
                                    Text("Hủy")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Lặp lại vào các ngày: ")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeatDays.forEach { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = { finalViewModel?.toggleDaySelection(day) },
                            label = { Text(day, fontSize = 14.sp) },
                            enabled = isEditing && finalViewModel != null,
                            modifier = Modifier,
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.Gray,
                                selectedLabelColor = Color(0xFFD58099),
                                containerColor = if (selectedDays.contains(day)) Color(0xFFD58099).copy(alpha = 0.1f) else Color.Transparent,
                                selectedContainerColor = Color(0xFFD58099).copy(alpha = 0.1f),
                                disabledLabelColor = Color(0xFF4CAF50),
                                disabledContainerColor = if (selectedDays.contains(day)) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Transparent,
                                disabledSelectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Chế độ Pomodoro")
                    Switch(
                        checked = pomodoroEnable,
                        onCheckedChange = { finalViewModel?.onPomodoroToggle(it) },
                        enabled = isEditing && finalViewModel != null,
                        modifier = Modifier,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD58099),
                            checkedTrackColor = Color(0xFFD58099).copy(alpha = 0.5f),
                            disabledCheckedThumbColor = Color(0xFF4CAF50),
                            disabledCheckedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }

                if (pomodoroEnable) {
                    OutlinedTextField(
                        value = focusMinutes.toString(),
                        onValueChange = { finalViewModel?.onFocusMinutesChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Phút tập trung") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing && finalViewModel != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = breakMinutes.toString(),
                        onValueChange = { finalViewModel?.onBreakMinutesChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Phút nghỉ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing && finalViewModel != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isEditing) Color(0xFFD58099).copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (isEditing) {
                            finalViewModel?.onIsEditChange(false)
                            finalViewModel?.setActivityDetails(activity)
                        } else {
                            onNavigationToList()
                        }
                    },
                    modifier = Modifier.width(160.dp).padding(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) Color(0xFFD58099) else Color(0xFF4CAF50))
                ) {
                    Text(if (isEditing) "Hủy" else "Trở lại")
                }

                Button(
                    onClick = {
                        if (isEditing) {
                            handleUpdate()
                        } else {
                            finalViewModel?.onIsEditChange(true)
                        }
                    },
                    modifier = Modifier.width(160.dp).padding(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) Color(0xFFD58099) else Color(0xFF4CAF50)),
                    enabled = finalViewModel != null
                ) {
                    Text(if (isEditing) "Lưu hoạt động" else "Sửa hoạt động")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailActivityScreenPreview() {
    val startTime = Timestamp.now()

    val sampleActivity = ActivityModel(
        activityId = "1",
        userId = "user_1",
        title = "Đọc sách",
        type = "Học tập",
        startTime = startTime,
        endTime = Timestamp(startTime.seconds + 3600, startTime.nanoseconds),
        repeatDays = listOf("T2", "T4"),
        pomodoroSettings = PomodoroSettings(
            pomodoroEnable = true,
            focusMinutes = 25,
            breakMinutes = 5
        )
    )

    MaterialTheme {
        DetailActivityScreen(
            activity = sampleActivity,
            onNavigationToList = {},
            onUpdateClick = {}
        )
    }
}