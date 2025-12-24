package com.example.allinone.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.entities.TaskEntity

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onTaskClick: (TaskEntity) -> Unit = {}
) {
    val monthAnchor by viewModel.monthAnchorMillis.collectAsState()
    val selectedDay by viewModel.selectedDayMillis.collectAsState()
    val tasks by viewModel.tasksOfSelectedDay.collectAsState()

    val monthModel = remember(monthAnchor) { buildMonthModel(monthAnchor) }

    Column(Modifier.padding(16.dp)) {

        // Header：月份 + 切換
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = viewModel::gotoPrevMonth) { Text("◀") }
            Text(
                text = monthModel.title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = viewModel::gotoNextMonth) { Text("▶") }
        }

        Spacer(Modifier.height(8.dp))

        // Weekdays
        Row(Modifier.fillMaxWidth()) {
            listOf("日","一","二","三","四","五","六").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Month grid
        MonthGrid(
            month = monthModel,
            selectedDayMillis = selectedDay,
            onDayClick = viewModel::selectDay
        )

        Spacer(Modifier.height(12.dp))

        // Selected day tasks
        Text("當天任務（${formatYmd(selectedDay)}）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        if (tasks.isEmpty()) {
            Text("這天沒有設定 due date 的任務。")
        } else {
            tasks.forEach { t ->
                ListItem(
                    headlineContent = { Text(t.title) },
                    supportingContent = { Text(t.detail) },
                    modifier = Modifier.clickable { onTaskClick(t) }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: MonthModel,
    selectedDayMillis: Long,
    onDayClick: (Long) -> Unit
) {
    val rows = month.cells.chunked(7)
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    cell == null -> Color.Transparent
                                    isSameDay(cell.millis, selectedDayMillis) -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable(enabled = cell != null) { cell?.let { onDayClick(it.millis) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cell?.dayText ?: "")
                    }
                }
            }
        }
    }
}

private data class DayCell(val millis: Long, val dayText: String)
private data class MonthModel(val title: String, val cells: List<DayCell?>)

private fun buildMonthModel(anchorMillis: Long): MonthModel {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = anchorMillis
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)

    val year = cal.get(java.util.Calendar.YEAR)
    val month = cal.get(java.util.Calendar.MONTH) // 0-11

    val title = "${year}年${month + 1}月"

    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    val leadingBlanks = firstDayOfWeek - 1

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    val cells = ArrayList<DayCell?>()
    repeat(leadingBlanks) { cells.add(null) }

    for (d in 1..daysInMonth) {
        val c = java.util.Calendar.getInstance()
        c.set(year, month, d, 0, 0, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        cells.add(DayCell(millis = c.timeInMillis, dayText = d.toString()))
    }

    while (cells.size % 7 != 0) cells.add(null)

    return MonthModel(title = title, cells = cells)
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
            ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatYmd(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN)
    return sdf.format(java.util.Date(millis))
}
