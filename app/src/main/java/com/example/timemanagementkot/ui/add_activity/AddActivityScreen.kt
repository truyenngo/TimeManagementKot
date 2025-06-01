package com.example.timemanagementkot.ui.add_activity

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import com.example.timemanagementkot.data.model.ActivityType
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: AddActivityViewModel? = null,
    onNavigationToList: () -> Unit = {},
    onSave: () -> Unit = {}
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

    val snackbarHostState = remember { SnackbarHostState() }
    val title by finalViewModel?.titleState?.collectAsState() ?: remember { mutableStateOf(defaultTitle) }
    val startTime by finalViewModel?.startTimeState?.collectAsState() ?: remember { mutableStateOf(defaultStartTime) }
    val endTime by finalViewModel?.endTimeState?.collectAsState() ?: remember { mutableStateOf(defaultEndTime) }
    val selectedDays by finalViewModel?.selectedDaysState?.collectAsState() ?: remember { mutableStateOf(defaultSelectedDays) }
    val isPomodoro by finalViewModel?.isPomodoroState?.collectAsState() ?: remember { mutableStateOf(defaultIsPomodoro) }
    val focusMinutes by finalViewModel?.focusMinutesState?.collectAsState() ?: remember { mutableStateOf(defaultFocusMinutes) }
    val breakMinutes by finalViewModel?.breakMinutesState?.collectAsState() ?: remember { mutableStateOf(defaultBreakMinutes) }
    val typeExpanded by finalViewModel?.typeExpandedState?.collectAsState() ?: remember { mutableStateOf(defaultTypeExpanded) }
    val repeatDays = finalViewModel?.repeatDays ?: defaultRepeatDays

    val selectedType by finalViewModel?.selectedTypeState?.collectAsState() ?: remember { mutableStateOf(defaultSelectedType) }
    val types by finalViewModel?.activityTypes?.collectAsState() ?: remember { mutableStateOf(defaultTypes) }
    val showAddTypeDialog by finalViewModel?.showAddTypeDialog?.collectAsState() ?: remember { mutableStateOf(defaultShowAddTypeDialog) }
    val showDeleteConfirmDialog by finalViewModel?.showDeleteConfirmDialog?.collectAsState() ?: remember { mutableStateOf(defaultShowDeleteConfirmDialog) }
    val typeToDelete by finalViewModel?.typeToDelete?.collectAsState() ?: remember { mutableStateOf(defaultTypeToDelete) }

    val calendar = remember { Calendar.getInstance() }

    val saveSuccessMessage by finalViewModel?.onSaveSuccess?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(saveSuccessMessage) {
        saveSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            finalViewModel?.resetSaveSuccess()
            onSave()
        }
    }

    val error by finalViewModel?.errorState?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            finalViewModel?.resetError()
        }
    }

    fun handleAdd() {
        finalViewModel?.addActivity(context)
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Text(
                text = "Thêm Hoạt Động Mới",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD58099))
                    .padding(16.dp)
            )

            Column(modifier = Modifier
                .padding(16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { finalViewModel?.onTitleChange(it) },
                    label = { Text("Tên hoạt động") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = finalViewModel != null
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { timePicker { finalViewModel?.onStartTimeChange(it) } },
                        enabled = finalViewModel != null
                    ) {
                        Text("Giờ bắt đầu: $startTime")
                    }
                    OutlinedButton(
                        onClick = { timePicker { finalViewModel?.onEndTimeChange(it) } },
                        enabled = finalViewModel != null
                    ) {
                        Text("Giờ kết thúc: $endTime")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Loại hoạt động")
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { finalViewModel?.onTypeExpandedChange(!typeExpanded) }
                ) {
                    OutlinedTextField(
                        value = selectedType?.typeName ?: "Chưa xếp loại",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = finalViewModel != null
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
                            enabled = finalViewModel != null
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
                        checked = isPomodoro,
                        onCheckedChange = { finalViewModel?.onPomodoroToggle(it) },
                        enabled = finalViewModel != null
                    )
                }

                if (isPomodoro) {
                    OutlinedTextField(
                        value = focusMinutes.toString(),
                        onValueChange = { finalViewModel?.onFocusMinutesChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Phút tập trung") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = finalViewModel != null
                    )

                    OutlinedTextField(
                        value = breakMinutes.toString(),
                        onValueChange = { finalViewModel?.onBreakMinutesChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Phút nghỉ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = finalViewModel != null
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD58099).copy(alpha = 0.1f)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onNavigationToList,
                    modifier = Modifier.width(160.dp).padding(10.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFFD58099))
                ) {
                    Text("Trở lại")
                }
                Button(
                    onClick = ::handleAdd,
                    modifier = Modifier.width(180.dp).padding(10.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFFD58099)),
                    enabled = finalViewModel != null
                ) {
                    Text("Thêm hoạt động")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddActivityScreenPreview() {
    MaterialTheme {
        AddActivityScreen(
            onNavigationToList = {},
            onSave = {}
        )
    }
}