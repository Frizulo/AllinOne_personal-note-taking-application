package com.example.allinone.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.entities.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.material3.HorizontalDivider


@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onTaskClick: (TaskEntity) -> Unit = {}
) {
    val monthAnchor by viewModel.monthAnchorMillis.collectAsState()
    val selectedDay by viewModel.selectedDayMillis.collectAsState()
    val tasks by viewModel.tasksOfSelectedDay.collectAsState()

    // 新增：模式（Tasks / Timeline）
    val mode by viewModel.mode.collectAsState()

    // 新增：Timeline slots + stats
    val slots by viewModel.slotsWithTask.collectAsState()
    val stats by viewModel.stats3x3.collectAsState()

    // Dialog / message
    val dialogState by viewModel.slotDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val monthModel = remember(monthAnchor) { buildMonthModel(monthAnchor) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {

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
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
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

            // ✅ 新增：下半部 Tabs
            TabRow(selectedTabIndex = if (mode == ScheduleViewModel.Mode.Tasks) 0 else 1) {
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

            Spacer(Modifier.height(10.dp))

            when (mode) {
                ScheduleViewModel.Mode.Tasks -> {
                    TasksPanel(
                        selectedDayMillis = selectedDay,
                        tasks = tasks,
                        onTaskClick = onTaskClick,
                        onScheduleTask = { task ->
                            // ✅ 任務旁的「安排時間」
                            // localId 型別：你的 TaskEntity.localId 是 String UUID
                            viewModel.openCreateTaskSlotDialog(taskLocalId = task.localId, taskTitleHint = task.title)
                        }
                    )
                }

                ScheduleViewModel.Mode.Timeline -> {
                    TimelinePanel(
                        selectedDayMillis = selectedDay,
                        slots = slots,
                        stats = stats,
                        onAddFreeSlot = { viewModel.openCreateFreeSlotDialog() },
                        onEditSlot = { slotEntity, taskTitle ->
                            viewModel.openEditSlotDialog(slotEntity, taskTitle)
                        },
                        onDeleteSlot = { slotId ->
                            viewModel.deleteSlot(slotId)
                        }
                    )
                }
            }
        }

        // ✅ 新增：Slot Dialog（簡化版）
        SlotDialogs(
            dialogState = dialogState,
            onDismiss = viewModel::closeSlotDialog,
            onUpdateDraft = viewModel::updateDraft,
            onSave = viewModel::saveDraft
        )
    }
}

@Composable
private fun TasksPanel(
    selectedDayMillis: Long,
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onScheduleTask: (TaskEntity) -> Unit
) {
    Text("當天任務（${formatYmd(selectedDayMillis)}）", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))

    if (tasks.isEmpty()) {
        Text("這天沒有設定 due date 的任務。")
    } else {
        tasks.forEach { t ->
            ListItem(
                headlineContent = { Text(t.title) },
                supportingContent = { Text(t.detail) },
                trailingContent = {
                    TextButton(onClick = { onScheduleTask(t) }) { Text("安排時間") }
                },
                modifier = Modifier.clickable { onTaskClick(t) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TimelinePanel(
    selectedDayMillis: Long,
    slots: List<com.example.allinone.data.local.ScheduleSlotWithTask>,
    stats: com.example.allinone.data.repo.ScheduleStats3x3,
    onAddFreeSlot: () -> Unit,
    onEditSlot: (com.example.allinone.data.local.entities.ScheduleSlotEntity, String?) -> Unit,
    onDeleteSlot: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("時間管理（${formatYmd(selectedDayMillis)}）", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Button(onClick = onAddFreeSlot) { Text("+ 新增時段") }
    }

    Spacer(Modifier.height(8.dp))

    // ✅ 先用文字呈現統計（之後再做漂亮卡片/3x3表格）
    Text(
        "統計（分鐘）— 早: T=${stats.morningTotal / 60000} 任務=${stats.morningTask / 60000} 純=${stats.morningFree / 60000}",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "統計（分鐘）— 中: T=${stats.afternoonTotal / 60000} 任務=${stats.afternoonTask / 60000} 純=${stats.afternoonFree / 60000}",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "統計（分鐘）— 晚: T=${stats.eveningTotal / 60000} 任務=${stats.eveningTask / 60000} 純=${stats.eveningFree / 60000}",
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(Modifier.height(10.dp))

    if (slots.isEmpty()) {
        Text("今天尚未安排任何時段。")
        return
    }

    slots.forEach { item ->
        val s = item.slot
        val title = item.taskTitle ?: s.customTitle ?: "（未命名時段）"
        val timeText = "${formatHm(s.startTimeMillis)} - ${formatHm(s.endTimeMillis)}"

        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(timeText) },
            trailingContent = {
                Row {
                    TextButton(onClick = { onEditSlot(s, item.taskTitle) }) { Text("編輯") }
                    TextButton(onClick = { onDeleteSlot(s.slotId) }) { Text("刪除") }
                }
            }
        )
        HorizontalDivider()
    }
}

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
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (dialogState.isNew) "新增時段" else "編輯時段") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // title（純時間管理必填；任務 slot 可留空）
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
                        Text("時間：${formatHm(d.startTimeMillis)} - ${formatHm(d.endTimeMillis)}")
                        Text(
                            "（下一步再加時間選擇器；目前先用預設時間 + 快速排程）",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onSave) { Text("儲存") }
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
                confirmButton = {
                    Button(onClick = onDismiss) { Text("了解") }
                }
            )
        }
    }
}

/* --------- 原本月曆元件保留 --------- */

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
    val cal = Calendar.getInstance()
    cal.timeInMillis = anchorMillis
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) // 0-11

    val title = "${year}年${month + 1}月"

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    val leadingBlanks = firstDayOfWeek - 1

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = ArrayList<DayCell?>()
    repeat(leadingBlanks) { cells.add(null) }

    for (d in 1..daysInMonth) {
        val c = Calendar.getInstance()
        c.set(year, month, d, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        cells.add(DayCell(millis = c.timeInMillis, dayText = d.toString()))
    }

    while (cells.size % 7 != 0) cells.add(null)

    return MonthModel(title = title, cells = cells)
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun formatYmd(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
    return sdf.format(Date(millis))
}

private fun formatHm(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.TAIWAN)
    return sdf.format(Date(millis))
}
