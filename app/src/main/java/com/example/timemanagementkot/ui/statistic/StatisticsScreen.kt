package com.example.timemanagementkot.ui.statistic

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.R
import com.example.timemanagementkot.data.model.LogTimeModel
import com.example.timemanagementkot.data.model.StatsModel
import com.google.firebase.Timestamp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker


@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    MaterialTheme {
        StatisticsScreen(
            onBack = {}
        )
    }
}

@Composable
fun StatisticsScreen(
    viewModel: StatisticsVM? = null,
    onBack: () -> Unit
) {
    val fakeLogsWithTitle = remember {
        listOf(
            LogTimeModel(
                logTimeId = "log1",
                activityId = "activity_1",
                userId = "user_123",
                startTime = Timestamp.now(),
                endTime = Timestamp.now(),
                date = Timestamp.now(),
                dayOfWeek = "Thứ 2",
                actualStart = Timestamp.now(),
                actualEnd = Timestamp.now(),
                duration = 60 * 60 * 1000L, // 1 giờ
                completeStatus = true
            ) to "Học lập trình",

            LogTimeModel(
                logTimeId = "log2",
                activityId = "activity_2",
                userId = "user_123",
                startTime = Timestamp.now(),
                endTime = Timestamp.now(),
                date = Timestamp.now(),
                dayOfWeek = "Thứ 2",
                actualStart = Timestamp.now(),
                actualEnd = Timestamp.now(),
                duration = 30 * 60 * 1000L, // 30 phút
                completeStatus = true
            ) to "Tập thể dục",

            LogTimeModel(
                logTimeId = "log3",
                activityId = "activity_3",
                userId = "user_123",
                startTime = Timestamp.now(),
                endTime = Timestamp.now(),
                date = Timestamp.now(),
                dayOfWeek = "Thứ 2",
                actualStart = Timestamp.now(),
                actualEnd = Timestamp.now(),
                duration = 45 * 60 * 1000L, // 45 phút
                completeStatus = false
            ) to "Đọc sách"
        )
    }

    val fakeWeeklyStatsWithTitle: List<Pair<StatsModel, String>> = listOf(
        StatsModel(
            statId = "stat1",
            activityId = "activity_1",
            userId = "user_123",
            type = "week",
            periodLabel = "Tuần 1",
            periodStart = Timestamp.now(),
            periodEnd = Timestamp.now(),
            totalDuration = 3 * 60 * 60 * 1000L, // 3 giờ
            completedCount = 5,
            missedCount = 1,
            pendingCount = 2,
            status = "completed"
        ) to "Học lập trình",

        StatsModel(
            statId = "stat2",
            activityId = "activity_2",
            userId = "user_123",
            type = "week",
            periodLabel = "Tuần 1",
            periodStart = Timestamp.now(),
            periodEnd = Timestamp.now(),
            totalDuration = 2 * 60 * 60 * 1000L, // 2 giờ
            completedCount = 4,
            missedCount = 0,
            pendingCount = 1,
            status = "completed"
        ) to "Tập thể dục",

        StatsModel(
            statId = "stat3",
            activityId = "activity_3",
            userId = "user_123",
            type = "week",
            periodLabel = "Tuần 1",
            periodStart = Timestamp.now(),
            periodEnd = Timestamp.now(),
            totalDuration = 1 * 60 * 60 * 1000L, // 1 giờ
            completedCount = 2,
            missedCount = 2,
            pendingCount = 1,
            status = "pending"
        ) to "Đọc sách"
    )

    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val currentDate by finalViewModel?.currentDateState?.collectAsState() ?: remember { mutableStateOf("Thứ 2, 06/05/2025") }
    val currentWeekState by finalViewModel?.currentWeekState?.collectAsState() ?: remember { mutableStateOf("Thứ 2, 06/05/2025") }
    val currentMonthState by finalViewModel?.currentMonthState?.collectAsState() ?: remember { mutableStateOf("Thứ 2, 06/05/2025") }

    val selectedFilter by finalViewModel?.selectedFilter?.collectAsState() ?: remember { mutableStateOf("daily") }

    val logsWithTitle by finalViewModel?.logsWithTitle?.collectAsState() ?: remember { mutableStateOf(fakeLogsWithTitle) }
    val weeklyStatsWithTitle by finalViewModel?.weeklyStatsWithTitle?.collectAsState() ?: remember { mutableStateOf(fakeWeeklyStatsWithTitle) }
    val monthlyStatsWithTitle by finalViewModel?.monthlyStatsWithTitle?.collectAsState() ?: remember { mutableStateOf(fakeWeeklyStatsWithTitle) }
    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val errorMessage by finalViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    val (activityTitles, dataPoints) = when (selectedFilter) {
        "day" -> {
            val grouped = (logsWithTitle as List<Pair<LogTimeModel, String>>)
                .groupBy { it.second }
                .mapValues { entry ->
                    entry.value.sumOf { it.first.duration } / 1000 / 60
                }
            grouped.keys.toList() to grouped.values.map { it.toInt() }
        }

        "week" -> {
            val grouped = (weeklyStatsWithTitle as List<Pair<StatsModel, String>>)
                .groupBy { it.second }
                .mapValues { entry ->
                    entry.value.sumOf { it.first.totalDuration } / 1000 / 60
                }
            grouped.keys.toList() to grouped.values.map { it.toInt() }
        }

        "month" -> {
            val grouped = (monthlyStatsWithTitle ?: emptyList())
                .groupBy { it.second }
                .mapValues { entry ->
                    entry.value.sumOf { it.first.totalDuration } / 1000 / 60
                }
            grouped.keys.toList() to grouped.values.map { it.toInt() }
        }

        else -> emptyList<String>() to emptyList<Int>()
    }

    val totalActivities = when (selectedFilter) {
        "day" -> logsWithTitle.size
        "week" -> weeklyStatsWithTitle.size
        "month" -> monthlyStatsWithTitle?.size ?: 0
        else -> 0
    }

    val totalDurationInMillis = when (selectedFilter) {
        "day" -> logsWithTitle.sumOf { it.first.duration }
        "week" -> weeklyStatsWithTitle.sumOf { it.first.totalDuration }
        "month" -> monthlyStatsWithTitle?.sumOf { it.first.totalDuration } ?: 0L
        else -> 0L
    }

    val totalDurationDisplay = formatMillisToTimeString(totalDurationInMillis)

    val totalCompletedCount = when(selectedFilter) {
        "week" -> weeklyStatsWithTitle.sumOf { it.first.completedCount }
        "month" -> monthlyStatsWithTitle?.sumOf { it.first.completedCount } ?: 0
        else -> 0
    }

    val totalMissedCount = when(selectedFilter) {
        "week" -> weeklyStatsWithTitle.sumOf { it.first.missedCount }
        "month" -> monthlyStatsWithTitle?.sumOf { it.first.missedCount } ?: 0
        else -> 0
    }

    val totalPendingCount = when(selectedFilter) {
        "week" -> weeklyStatsWithTitle.sumOf { it.first.pendingCount }
        "month" -> monthlyStatsWithTitle?.sumOf { it.first.pendingCount } ?: 0
        else -> 0
    }

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
                text = "Thống kê",
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

        Column (
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        when (selectedFilter) {
                            "day" -> finalViewModel?.onPreviousDay()
                            "week" -> finalViewModel?.onPreviousWeek()
                            "month" -> finalViewModel?.onPreviousMonth()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_previous),
                        contentDescription = "Mô tả",
                        tint = Color.Unspecified
                    )
                }

                Text(
                    text = when (selectedFilter) {
                        "day" -> currentDate
                        "week" -> currentWeekState
                         "month" -> currentMonthState
                        else -> currentDate
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD7566A),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        when (selectedFilter) {
                            "day" -> finalViewModel?.onNextDay()
                            "week" -> finalViewModel?.onNextWeek()
                            "month" -> finalViewModel?.onNextMonth()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Mô tả",
                        tint = Color.Unspecified
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterButton("day", "Ngày", selectedFilter == "day") {
                    finalViewModel?.setSelectedFilter("day")
                }
                FilterButton("week", "Tuần", selectedFilter == "week") {
                    finalViewModel?.setSelectedFilter("week")
                }
                FilterButton("month", "Tháng", selectedFilter == "month") {
                    finalViewModel?.setSelectedFilter("month")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD58099).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    ){
                        Text("Tổng số hoạt động: ${totalActivities}", fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Tổng thời gian: ${totalDurationDisplay}", fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Tổng hợp tiến trình:", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatisticCountItem(label = "Hoàn thành", count = totalCompletedCount, color = Color(0xFF4CAF50))
                        StatisticCountItem(label = "Bỏ lỡ", count = totalMissedCount, color = Color(0xFFF44336))
                        StatisticCountItem(label = "Sắp đến hạn", count = totalPendingCount, color = Color(0xFFFFC107))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Biểu đồ thời lượng theo hoạt động:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            StatisticColumnChart(
                dataPoints = dataPoints.map { it.toInt() },
                labels = activityTitles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 10.dp)
            )

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!errorMessage.isNullOrEmpty()) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD58099),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxHeight()
                        )
                    } else {
                        Spacer(modifier = Modifier.fillMaxHeight())
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
        }
    }
}

@Composable
fun FilterButton(
    key: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFD58099) else Color.LightGray,
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
fun StatisticColumnChart(
    modifier: Modifier = Modifier,
    dataPoints: List<Int> = listOf(),
    labels: List<String> = listOf()
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(dataPoints) {
        if (dataPoints.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(*dataPoints.toTypedArray())
                }
            }
        } else {
            modelProducer.runTransaction {
                columnSeries {
                    series(0)
                }
            }
        }
    }

    val marker = rememberMarker(
        valueFormatter = DefaultCartesianMarker.ValueFormatter { value, _ ->
            "${(value as? Float)?.toInt() ?: 0} phút"
        }
    )

    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(),
        startAxis = VerticalAxis.rememberStart(
            valueFormatter = { _, value, _ ->
                "${value.toInt()} phút"
            }
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = { _, x, _ ->
                labels.getOrNull(x.toInt()) ?: x.toInt().toString()
            }
        )
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier
    )

}

@Composable
fun StatisticCountItem(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "$count",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = color
        )
    }
}

fun formatMillisToTimeString(durationMillis: Long): String {
    val totalMinutes = durationMillis / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%02d:%02d", hours, minutes)
}
