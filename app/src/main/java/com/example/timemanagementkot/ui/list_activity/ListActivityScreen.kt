package com.example.timemanagementkot.ui.list_activity

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.data.model.ActivityModel
import com.example.timemanagementkot.data.model.PomodoroSettings
import com.example.timemanagementkot.ui.home.formatTimeToHHmm
import com.google.firebase.Timestamp
import java.util.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.example.timemanagementkot.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListActivityScreen(
    viewModel: ListActivityVM? = null,
    onActivityClick: (ActivityModel) -> Unit = {},
    onAddActivityClick: () -> Unit = {},
    onDeleteClick: (ActivityModel) -> Unit = {},
    onBack: () -> Unit
) {
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
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val activityList by finalViewModel?.activities?.collectAsState() ?: remember { mutableStateOf(defaultActivityList) }
    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val trackHeightPx = remember { mutableStateOf(0f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD58099)),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "Danh sách hoạt động",
                        fontSize = 22.sp,
                        color = Color.White,
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
                ){
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
                    }else if (!isLoading && activityList.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Chưa có hoạt động nào", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size: IntSize ->
                                trackHeightPx.value = size.height.toFloat()
                                Log.e("trackHeightPx", "trackHeightPx: {$trackHeightPx.value }")
                            }
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 12.dp, end = 12.dp)
                            ) {
                                items(activityList) { activity ->
                                    ActivityItem(
                                        activity,
                                        onActivityClick,
                                        onDeleteClick = { activityToDelete ->
                                            finalViewModel?.deleteActivity(activityToDelete.activityId, context)
                                        },
                                        showSnackbar = showSnackbar
                                    )
                                }
                            }

                            CustomVerticalScrollbar(
                                listState = listState,
                                trackHeightPx= trackHeightPx.value,
                                modifier = Modifier.align(Alignment.CenterEnd)
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
                ){
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp),
                    ) {
                        Text("Thoát")
                    }

                    Button(
                        onClick = { onAddActivityClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Thêm hoạt động")
                    }
                }
            }
        }
    )


}

@Composable
private fun ActivityItem(
    activity: ActivityModel,
    onActivityClick: (ActivityModel) -> Unit,
    onDeleteClick: (ActivityModel) -> Unit,
    showSnackbar: (String) -> Unit
    ) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xoá") },
            text = { Text("Bạn có chắc chắn muốn xoá hoạt động này không?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(activity)
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
            .height(180.dp)
            .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
            .clickable { onActivityClick(activity) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD58099).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
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
                        text = activity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFD58099)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Bắt đầu: ${formatTimeToHHmm(activity.startTime)}")
                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Kết thúc: ${formatTimeToHHmm(activity.endTime)}")
                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Loại: ${activity.type}")

                    if (activity.repeatDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("Lặp lại: ${activity.repeatDays.joinToString(", ")}")
                    }
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
        }
    }
}

@Composable
fun CustomVerticalScrollbar(
    listState: LazyListState,
    trackHeightPx: Float,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color(0xFFD58099).copy(alpha = 0.1f),
    trackColor: Color = Color.LightGray.copy(alpha = 0.1f)
) {
    val scrollbarWidth = 6.dp
    val density = LocalDensity.current

    Box(modifier = modifier.width(scrollbarWidth)) {
        Box(
            modifier = Modifier
                .height(with(density) { trackHeightPx.toDp() })
                .width(scrollbarWidth)
                .background(trackColor)
        )

        val layoutInfo = listState.layoutInfo
        if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val itemHeightPx = with(density) { 180.dp.toPx() }

            val totalContentHeight = layoutInfo.totalItemsCount * itemHeightPx

            val thumbHeightPx = (trackHeightPx * trackHeightPx) / (layoutInfo.totalItemsCount * itemHeightPx)

            if (totalContentHeight > trackHeightPx) {
                val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                val scrollableHeight = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
                
                val scrollOffset = listState.firstVisibleItemIndex * itemHeightPx +
                        listState.firstVisibleItemScrollOffset

                val scrollProgress = (scrollOffset / scrollableHeight).coerceIn(0f, 1f)

                val maxThumbOffset = trackHeightPx - thumbHeightPx
                val thumbOffsetPx = scrollProgress * maxThumbOffset

                animateDpAsState(
                    targetValue = with(density) { thumbOffsetPx.toDp() },
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = LinearEasing
                    )
                ).value.let { animatedOffset ->
                    Box(
                        modifier = Modifier
                            .width(scrollbarWidth)
                            .height(with(density) { thumbHeightPx.toDp() })
                            .offset(y = animatedOffset)
                            .background(
                                color = thumbColor,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListActivityScreenPreview() {
    MaterialTheme {
        ListActivityScreen(
            onActivityClick = { _ -> },
            onAddActivityClick = {},
            onBack = {}
        )
    }
}