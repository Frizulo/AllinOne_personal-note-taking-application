package com.example.allinone.ui.schedule

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.ScheduleRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onTaskClick: (TaskEntity) -> Unit = {}
) {
    val monthAnchor by viewModel.monthAnchorMillis.collectAsState()
    val selectedDay by viewModel.selectedDayMillis.collectAsState()
    val tasks by viewModel.tasksOfSelectedDay.collectAsState()

    val mode by viewModel.mode.collectAsState()
    val slots by viewModel.slotsWithTask.collectAsState()
    val stats by viewModel.stats4x3.collectAsState()
    val dialogState by viewModel.slotDialog.collectAsState()

    var isCalendarExpanded by remember { mutableStateOf(false) } // false=週, true=月

    // ✅ 防止連點 / 抖動造成過度重組或重複 DB 請求（容易觸發 ANR）
    var lastNavAt by remember { mutableStateOf(0L) }
    fun throttled(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavAt < 280L) return
        lastNavAt = now
        action()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg: String ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val monthModel = remember(monthAnchor) { buildMonthModel(monthAnchor) }
    val weekTitle = remember(selectedDay) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDay
            set(Calendar.DAY_OF_MONTH, 1)
        }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            CalendarHeader(
                title = if (isCalendarExpanded) monthModel.title else weekTitle,
                expanded = isCalendarExpanded,
                onPrev = {
                    throttled {
                        if (isCalendarExpanded) {
                            viewModel.gotoPrevMonth()
                        } else {
                            val prevWeek = selectedDay - WEEK_MS
                            viewModel.selectDay(prevWeek)
                            // 週模式：只有跨月才更新 anchor，避免每次都重建月資料/觸發重算
                            if (!isSameMonthFast(prevWeek, monthAnchor)) {
                                viewModel.setMonthAnchor(prevWeek)
                            }
                        }
                    }
                },
                onNext = {
                    throttled {
                        if (isCalendarExpanded) {
                            viewModel.gotoNextMonth()
                        } else {
                            val nextWeek = selectedDay + WEEK_MS
                            viewModel.selectDay(nextWeek)
                            if (!isSameMonthFast(nextWeek, monthAnchor)) {
                                viewModel.setMonthAnchor(nextWeek)
                            }
                        }
                    }
                },
                onToggle = {
                    throttled {
                        isCalendarExpanded = !isCalendarExpanded
                        // 切到月模式時，確保 title 的月份跟著目前選到的那天
                        if (isCalendarExpanded && !isSameMonthFast(selectedDay, monthAnchor)) {
                            viewModel.setMonthAnchor(selectedDay)
                        }
                    }
                }
            )

            Spacer(Modifier.height(6.dp))
            WeekdaysRow()
            Spacer(Modifier.height(6.dp))

            CalendarSection(
                month = monthModel,
                selectedDayMillis = selectedDay,
                expanded = isCalendarExpanded,
                onDayClick = { day ->
                    throttled {
                        viewModel.selectDay(day)

                        // ✅ 週模式：只有跨月才更新 anchor（避免每次點都重算）
                        if (!isCalendarExpanded) {
                            return@throttled
                        }

                        // ✅ 月模式：anchor 只在「點到其他月份的日期」時才改
                        if (!isSameMonthFast(day, monthAnchor)) {
                            viewModel.setMonthAnchor(day)
                        }
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {

                    PrimaryTabRow(
                        selectedTabIndex = if (mode == ScheduleViewModel.Mode.Tasks) 0 else 1
                    ) {
                        Tab(
                            selected = mode == ScheduleViewModel.Mode.Tasks,
                            onClick = { viewModel.setMode(ScheduleViewModel.Mode.Tasks) },
                            text = { Text("當天任務") }
                        )
                        Tab(
                            selected = mode == ScheduleViewModel.Mode.Timeline,
                            onClick = { viewModel.setMode(ScheduleViewModel.Mode.Timeline) },
                            text = { Text("時間管理") }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    when (mode) {
                        ScheduleViewModel.Mode.Tasks -> {
                            Column(modifier = Modifier.fillMaxSize()) {

                                // ✅ 顯示「目前選到哪天」
                                Text(
                                    text = "當天任務（${formatYmd(startOfDayMillis(selectedDay))}）",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                                )

                                TasksPanel(
                                    tasks = tasks,
                                    slots = slots,
                                    onTaskClick = onTaskClick,
                                    onScheduleTask = { task ->
                                        viewModel.openCreateTaskSlotDialog(
                                            taskLocalId = task.localId,
                                            taskTitleHint = task.title
                                        )
                                    }
                                )
                            }
                        }

                        ScheduleViewModel.Mode.Timeline -> {
                            TimelinePanel(
                                selectedDayMillis = selectedDay,
                                slots = slots,
                                stats = stats,
                                onAddFreeSlot = { viewModel.openCreateFreeSlotDialog() },
                                onEditSlot = { slot, taskTitle ->
                                    viewModel.openEditSlotDialog(slot, taskTitle)
                                },
                                onDeleteSlot = { slotId -> viewModel.deleteSlot(slotId) }
                            )
                        }
                    }
                }
            }

            SlotDialogs(
                dialogState = dialogState,
                onDismiss = viewModel::closeSlotDialog,
                onUpdateDraft = viewModel::updateDraft,
                onSave = viewModel::saveDraft
            )
        }
    }
}

/* ---------------- Header / Weekdays ---------------- */

@Composable
private fun CalendarHeader(
    title: String,
    expanded: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "月曆（整月）" else "月曆（週）",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onToggle) {
            Text(if (expanded) "收合" else "展開")
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onPrev) { Text("◀") }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext) { Text("▶") }
    }
}

@Composable
private fun WeekdaysRow() {
    Row(Modifier.fillMaxWidth()) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach {
            Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

/* ---------------- Calendar Section (週/月) ---------------- */

@Composable
private fun CalendarSection(
    month: MonthModel,
    selectedDayMillis: Long,
    expanded: Boolean,
    onDayClick: (Long) -> Unit
) {
    if (expanded) {
        MonthGrid(month, selectedDayMillis, onDayClick)
    } else {
        // ✅ 週模式不要再從 month.cells 做 index/subList 切。
        //    monthAnchor 在跨月/快速操作時可能暫時不同步，會導致週列資料不一致甚至卡頓。
        val weekCells: List<DayCell?> = remember(selectedDayMillis) { buildWeekCells(selectedDayMillis) }
        WeekRow(weekCells, selectedDayMillis, onDayClick)
    }
}

private fun buildWeekCells(centerDayMillis: Long): List<DayCell> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = centerDayMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }

    return (0..6).map { offset ->
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, offset)
        DayCell(
            millis = c.timeInMillis,
            dayText = c.get(Calendar.DAY_OF_MONTH).toString()
        )
    }
}


@Composable
private fun WeekRow(
    weekCells: List<DayCell?>,
    selectedDayMillis: Long,
    onDayClick: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekCells.forEach { cell ->
            DayCellBox(
                cell = cell,
                selectedDayMillis = selectedDayMillis,
                onDayClick = onDayClick,
                modifier = Modifier.weight(1f)
            )
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { cell ->
                    DayCellBox(
                        cell = cell,
                        selectedDayMillis = selectedDayMillis,
                        onDayClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayCellBox(
    cell: DayCell?,
    selectedDayMillis: Long,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        cell == null -> Color.Transparent
        isSameDayFast(cell.millis, selectedDayMillis) ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = cell != null) {
                if (cell != null) onDayClick(cell.millis)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = cell?.dayText ?: "")
    }
}

/* ---------------- Panels ---------------- */

@Composable
private fun TasksPanel(
    tasks: List<TaskEntity>,
    slots: List<ScheduleSlotWithTask>,
    onTaskClick: (TaskEntity) -> Unit,
    onScheduleTask: (TaskEntity) -> Unit
) {
    val scheduledCountByTaskId = remember(slots) {
        val m = mutableMapOf<String, Int>()
        slots.forEach { item ->
            val id = item.slot.localTaskId
            if (id != null) m[id] = (m[id] ?: 0) + 1
        }
        m
    }

    val (scheduled, unscheduled) = remember(tasks, scheduledCountByTaskId) {
        tasks.partition { t -> (scheduledCountByTaskId[t.localId] ?: 0) > 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("未排程", style = MaterialTheme.typography.titleSmall) }

        if (unscheduled.isEmpty()) {
            item { Text("全部都已排程 ✅", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }
        } else {
            items(unscheduled, key = { it.localId }) { t ->
                TaskCardRow(
                    task = t,
                    badgeText = "未排",
                    badgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    onTaskClick = onTaskClick,
                    onScheduleTask = onScheduleTask
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text("已排程", style = MaterialTheme.typography.titleSmall)
        }

        if (scheduled.isEmpty()) {
            item { Text("目前沒有已排程的任務。", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }
        } else {
            items(scheduled, key = { it.localId }) { t ->
                val n = scheduledCountByTaskId[t.localId] ?: 0
                TaskCardRow(
                    task = t,
                    badgeText = "已排 $n 段",
                    badgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    onTaskClick = onTaskClick,
                    onScheduleTask = onScheduleTask
                )
            }
        }
    }
}

@Composable
private fun TaskCardRow(
    task: TaskEntity,
    badgeText: String,
    badgeColor: Color,
    onTaskClick: (TaskEntity) -> Unit,
    onScheduleTask: (TaskEntity) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTaskClick(task) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(badgeText, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                if (task.detail.isNotBlank()) {
                    Text(
                        task.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            TextButton(onClick = { onScheduleTask(task) }) { Text("安排") }
        }
    }
}

@Composable
private fun TimelinePanel(
    selectedDayMillis: Long,
    slots: List<ScheduleSlotWithTask>,
    stats: ScheduleRepository.ScheduleStats4x3,
    onAddFreeSlot: () -> Unit,
    onEditSlot: (ScheduleSlotEntity, String?) -> Unit,
    onDeleteSlot: (Long) -> Unit
) {
    val day0 = remember(selectedDayMillis) { startOfDayMillis(selectedDayMillis) }

    val hourHeight = 56.dp
    val minuteHeightDp = hourHeight / 60f
    val totalHeight = hourHeight * 24

    val scrollState = rememberScrollState()
    val sorted = remember(slots) { slots.sortedBy { it.slot.startTimeMillis } }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("時間管理（${formatYmd(day0)}）", style = MaterialTheme.typography.titleMedium)
                Text(
                    "統計單位：分鐘",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Button(
                onClick = onAddFreeSlot,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(40.dp)
            ) { Text("+ 新增行程") }
        }

        Spacer(Modifier.height(10.dp))
        StatsRow4(stats = stats)
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            TimelineBackground(
                totalHeight = totalHeight,
                hourHeight = hourHeight
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                sorted.forEach { item ->
                    val s = item.slot
                    val title = item.taskTitle ?: s.customTitle ?: "（未命名）"
                    val isTask = s.localTaskId != null

                    val startMin = minutesFromDayStart(day0, s.startTimeMillis)
                    val endMin = minutesFromDayStart(day0, s.endTimeMillis)

                    val safeStart = startMin.coerceIn(0, 24 * 60)
                    val safeEnd = endMin.coerceIn(0, 24 * 60).coerceAtLeast(safeStart + 1)
                    val durMin = safeEnd - safeStart

                    val top = minuteHeightDp * safeStart
                    val height = minuteHeightDp * durMin

                    TimelineSlotBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 74.dp, end = 16.dp)
                            .offset(y = top)
                            .height(height),
                        title = title,
                        timeText = "${formatHm(s.startTimeMillis)} - ${formatHm(s.endTimeMillis)}",
                        isTask = isTask,
                        onEdit = { onEditSlot(s, item.taskTitle) },
                        onDelete = { onDeleteSlot(s.slotId) }
                    )
                }
            }
        }
    }
}

/* ---------------- Timeline background + slot styles ---------------- */

@Composable
private fun TimelineBackground(
    totalHeight: Dp,
    hourHeight: Dp,
    railWidth: Dp = 56.dp,
    labelBaselineAdjust: Dp = 6.dp,
    showBoldAt: Set<Int> = setOf(6, 12, 18)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        fun segTop(h: Int) = hourHeight * h
        fun segHeight(from: Int, to: Int) = hourHeight * (to - from)

        SegmentBg(railWidth, segTop(0), segHeight(0, 6), MaterialTheme.colorScheme.secondary, 0.30f) // Sleep
        SegmentBg(railWidth, segTop(6), segHeight(6, 12), MaterialTheme.colorScheme.primary, 0.30f) // Morning
        SegmentBg(railWidth, segTop(12), segHeight(12, 18), MaterialTheme.colorScheme.tertiary, 0.30f) // Afternoon
        SegmentBg(railWidth, segTop(18), segHeight(18, 24), MaterialTheme.colorScheme.error, 0.30f) // Evening

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            repeat(24) { h ->
                val isBold = h in showBoldAt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isBold) 2.dp else 1.dp)
                        .padding(start = railWidth, end = 12.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = if (isBold) 0.60f else 0.28f))
                )
                Spacer(Modifier.height(hourHeight - (if (isBold) 2.dp else 1.dp)))
            }
        }

        Box(
            modifier = Modifier
                .width(railWidth)
                .fillMaxHeight()
        ) {
            repeat(24) { h ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (hourHeight * h) - labelBaselineAdjust)
                ) {
                    Text(
                        text = h.toString().padStart(2, '0'),
                        style = if (h in showBoldAt) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (h in showBoldAt) 0.90f else 0.70f),
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentBg(
    railWidth: Dp,
    top: Dp,
    height: Dp,
    color: Color,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(height)
            .padding(start = railWidth, end = 12.dp)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun TimelineSlotBlock(
    modifier: Modifier = Modifier,
    title: String,
    timeText: String,
    isTask: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val barColor = if (isTask) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.80f)
    }
    val cardBg = if (isTask) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = cardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
                Text(
                    if (isTask) "任務" else "純行程",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }

            TextButton(onClick = onEdit) { Text("編輯") }
            TextButton(onClick = onDelete) { Text("刪除") }
        }
    }
}

/* ---------------- Stats row (Sleep/Morning/Afternoon/Evening) ---------------- */

@Composable
private fun StatsRow4(stats: ScheduleRepository.ScheduleStats4x3) {

    data class Entry(
        val label: String,
        val total: Long,
        val task: Long,
        val free: Long,
        val color: Color
    )

    @Composable
    fun bucketColor(label: String): Color = when (label) {
        "睡" -> MaterialTheme.colorScheme.secondary
        "早" -> MaterialTheme.colorScheme.primary
        "中" -> MaterialTheme.colorScheme.tertiary
        "晚" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val entries = listOf(
        Entry("睡", stats.sleepTotal, stats.sleepTask, stats.sleepFree, bucketColor("睡")),
        Entry("早", stats.morningTotal, stats.morningTask, stats.morningFree, bucketColor("早")),
        Entry("中", stats.afternoonTotal, stats.afternoonTask, stats.afternoonFree, bucketColor("中")),
        Entry("晚", stats.eveningTotal, stats.eveningTask, stats.eveningFree, bucketColor("晚")),
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(entries) { e ->
            val bg = e.color.copy(alpha = 0.18f)

            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                color = bg,
                modifier = Modifier
                    .width(85.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(e.label, style = MaterialTheme.typography.labelMedium)
                        Text("${(e.total / 60_000)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/* ---------------- Month model + utils ---------------- */

private data class DayCell(val millis: Long, val dayText: String)
private data class MonthModel(val title: String, val cells: List<DayCell?>)

private fun buildMonthModel(anchorMillis: Long): MonthModel {
    val cal = Calendar.getInstance()
    cal.timeInMillis = anchorMillis
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val title = "${year}年${month + 1}月"

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    val leadingBlanks = firstDayOfWeek - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = ArrayList<DayCell?>()
    repeat(leadingBlanks) { cells.add(null) }

    for (d in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cells.add(DayCell(millis = cal.timeInMillis, dayText = d.toString()))
    }

    while (cells.size % 7 != 0) cells.add(null)

    return MonthModel(title = title, cells = cells)
}

private fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun minutesFromDayStart(day0: Long, timeMillis: Long): Int {
    val diff = (timeMillis - day0).coerceIn(0L, 24L * 60L * 60_000L)
    return (diff / 60_000L).toInt()
}

private fun dayKey(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

private fun isSameDayFast(a: Long, b: Long): Boolean = dayKey(a) == dayKey(b)

private fun isSameMonthFast(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
}

private fun formatYmd(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
    return sdf.format(Date(millis))
}

private fun formatHm(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.TAIWAN)
    return sdf.format(Date(millis))
}

/* ---------------- Slot dialogs ---------------- */

@Composable
private fun SlotDialogs(
    dialogState: ScheduleViewModel.SlotDialogState,
    onDismiss: () -> Unit,
    onUpdateDraft: ((ScheduleViewModel.SlotDraft) -> ScheduleViewModel.SlotDraft) -> Unit,
    onSave: () -> Unit
) {
    when (dialogState) {
        ScheduleViewModel.SlotDialogState.Hidden -> Unit

        is ScheduleViewModel.SlotDialogState.Editing -> {
            val d = dialogState.draft
            val date0 = d.dateMillis
            val step: Int? = 30

            fun hmToDigits(millis: Long): String = formatHm(millis).replace(":", "")

            var start by remember(d.startTimeMillis) {
                val digits = hmToDigits(d.startTimeMillis)
                mutableStateOf(TextFieldValue(text = digits, selection = TextRange(digits.length)))
            }
            var end by remember(d.endTimeMillis) {
                val digits = hmToDigits(d.endTimeMillis)
                mutableStateOf(TextFieldValue(text = digits, selection = TextRange(digits.length)))
            }

            fun digitsOnly(tfv: TextFieldValue): TextFieldValue {
                val digits = tfv.text.filter { it.isDigit() }.take(4)
                return TextFieldValue(text = digits, selection = TextRange(digits.length))
            }

            val startOff = TimeInput.digitsToOffsetMillis(start.text, allowStepMinutes = step)
            val endOff = TimeInput.digitsToOffsetMillis(end.text, allowStepMinutes = step)

            val timeError: String? = when {
                start.text.length < 3 || startOff == null ->
                    "開始時間格式錯誤（例：0930 或 09:30；分鐘需符合 30 分步進）"
                end.text.length < 3 || endOff == null ->
                    "結束時間格式錯誤（例：1800 或 18:00；分鐘需符合 30 分步進）"
                endOff <= startOff ->
                    "結束時間必須晚於開始時間"
                else -> null
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (dialogState.isNew) "新增時段" else "編輯時段") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        OutlinedTextField(
                            value = d.customTitle,
                            onValueChange = { v -> onUpdateDraft { it.copy(customTitle = v) } },
                            label = { Text("時段標題（純時間管理必填）") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = d.note,
                            onValueChange = { v -> onUpdateDraft { it.copy(note = v) } },
                            label = { Text("備註（選填）") }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                            OutlinedTextField(
                                value = start,
                                onValueChange = { input ->
                                    val cleaned = digitsOnly(input)
                                    start = cleaned

                                    val off = TimeInput.digitsToOffsetMillis(cleaned.text, allowStepMinutes = step)
                                    if (off != null) {
                                        val newStart = TimeOptions.absoluteMillis(date0, off)
                                        onUpdateDraft { it.copy(startTimeMillis = newStart) }

                                        val endParsed = TimeInput.digitsToOffsetMillis(end.text, allowStepMinutes = step)
                                        if (endParsed == null || endParsed <= off) {
                                            val suggested = off + 60L * 60_000L
                                            val suggestedDigits = TimeOptions.offsetToLabel(suggested).replace(":", "")
                                            end = TextFieldValue(text = suggestedDigits, selection = TextRange(suggestedDigits.length))
                                            val newEnd = TimeOptions.absoluteMillis(date0, suggested)
                                            onUpdateDraft { it.copy(endTimeMillis = newEnd) }
                                        }
                                    }
                                },
                                label = { Text("開始 (HH:mm)") },
                                singleLine = true,
                                isError = timeError?.contains("開始") == true,
                                visualTransformation = HhmmVisualTransformation,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = end,
                                onValueChange = { input ->
                                    val cleaned = digitsOnly(input)
                                    end = cleaned

                                    val off = TimeInput.digitsToOffsetMillis(cleaned.text, allowStepMinutes = step)
                                    if (off != null) {
                                        val newEnd = TimeOptions.absoluteMillis(date0, off)
                                        onUpdateDraft { it.copy(endTimeMillis = newEnd) }
                                    }
                                },
                                label = { Text("結束 (HH:mm)") },
                                singleLine = true,
                                isError = timeError != null && (timeError.contains("結束") || timeError.contains("必須晚於")),
                                visualTransformation = HhmmVisualTransformation,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (timeError != null) {
                            Text(
                                text = timeError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                "限制：30 分鐘步進（分鐘只能 00 或 30）",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onSave, enabled = timeError == null) { Text("儲存") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            )
        }

        is ScheduleViewModel.SlotDialogState.Conflict -> {
            val c = dialogState.conflict
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("時間衝突") },
                text = {
                    Text(
                        "此時段與既有排程衝突：\n" +
                                "${formatHm(c.startTimeMillis)} - ${formatHm(c.endTimeMillis)}\n" +
                                "請調整時間後再儲存。"
                    )
                },
                confirmButton = { Button(onClick = onDismiss) { Text("了解") } }
            )
        }
    }
}

private object HhmmVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = buildString {
            raw.forEachIndexed { i, c ->
                if (i == 2) append(':')
                append(c)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) offset else offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                when {
                    offset <= 2 -> offset
                    offset == 3 -> 2
                    else -> offset - 1
                }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val WEEK_MS = 7L * DAY_MS
