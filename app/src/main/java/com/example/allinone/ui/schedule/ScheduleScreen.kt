package com.example.allinone.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.ScheduleStats3x3
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex



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
    val stats by viewModel.stats3x3.collectAsState()
    val dialogState by viewModel.slotDialog.collectAsState()

    var isCalendarExpanded by remember { mutableStateOf(false) } // false=週, true=月

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg: String ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val monthModel = remember(monthAnchor) { buildMonthModel(monthAnchor) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {

            // ========== 上半段：固定區（不跟清單一起滾） ==========
            CalendarHeader(
                title = monthModel.title,
                expanded = isCalendarExpanded,
                onPrev = {
                    if (isCalendarExpanded) {
                        viewModel.gotoPrevMonth()
                    } else {
                        val prev = selectedDay - 7L * 24 * 60 * 60 * 1000
                        viewModel.selectDay(prev)
                        viewModel.setMonthAnchor(prev)
                    }
                },
                onNext = {
                    if (isCalendarExpanded) {
                        viewModel.gotoNextMonth()
                    } else {
                        val next = selectedDay + 7L * 24 * 60 * 60 * 1000
                        viewModel.selectDay(next)
                        viewModel.setMonthAnchor(next)
                    }
                },
                onToggle = { isCalendarExpanded = !isCalendarExpanded }
            )

            Spacer(Modifier.height(6.dp))

            WeekdaysRow()

            Spacer(Modifier.height(6.dp))

            Box(Modifier.zIndex(1f)) {
                CalendarSection(
                    month = monthModel,
                    selectedDayMillis = selectedDay,
                    expanded = isCalendarExpanded,
                    onDayClick = { day ->
                        viewModel.selectDay(day)
                        viewModel.setMonthAnchor(day)
                        android.util.Log.d("Schedule", "clicked day=${formatYmd(day)}")
123
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ========== 下半段：佔滿剩餘高度（清單可滑） ==========
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {

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

                    Spacer(Modifier.height(8.dp))

                    when (mode) {
                        ScheduleViewModel.Mode.Tasks -> {
                            TasksPanel(
                                tasks = tasks,
                                onTaskClick = onTaskClick,
                                onScheduleTask = { task ->
                                    viewModel.openCreateTaskSlotDialog(
                                        taskLocalId = task.localId,
                                        taskTitleHint = task.title
                                    )
                                }
                            )
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

            // ✅ 你的 SlotDialogs 保留（把你原本那個 SlotDialogs 函式貼回同檔案即可）
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
    // ✅ 把「展開/收合」跟月份列放同一區塊，避免跑版
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
    val selectedKey = remember(selectedDayMillis) { dayKey(selectedDayMillis) }

    val weekCells: List<DayCell?> = remember(month, selectedKey) {
        val idx = month.cells.indexOfFirst { cell ->
            cell != null && dayKey(cell.millis) == selectedKey
        }
        if (idx == -1) month.cells.take(7)
        else {
            val rowStart = (idx / 7) * 7
            month.cells.subList(rowStart, rowStart + 7)
        }
    }

    if (expanded) {
        MonthGrid(month, selectedDayMillis, onDayClick)
    } else {
        WeekRow(weekCells, selectedDayMillis, onDayClick)
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
            // ✅ 不用 clickable，改用 pointerInput 更穩（避免被其他 clickable/scroll 互搶）
            .pointerInput(cell?.millis) {
                if (cell == null) return@pointerInput
                detectTapGestures(
                    onTap = { onDayClick(cell.millis) }
                )
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
    onTaskClick: (TaskEntity) -> Unit,
    onScheduleTask: (TaskEntity) -> Unit
) {
    if (tasks.isEmpty()) {
        Text("這天沒有設定 due date 的任務。")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tasks, key = { it.localId }) { t ->
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
    slots: List<ScheduleSlotWithTask>,
    stats: ScheduleStats3x3,
    onAddFreeSlot: () -> Unit,
    onEditSlot: (ScheduleSlotEntity, String?) -> Unit,
    onDeleteSlot: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "時間管理（${formatYmd(selectedDayMillis)}）",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onAddFreeSlot) { Text("+ 新增時段") }
    }

    Spacer(Modifier.height(6.dp))

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

    Spacer(Modifier.height(8.dp))

    if (slots.isEmpty()) {
        Text("今天尚未安排任何時段。")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(slots, key = { it.slot.slotId }) { item ->
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
}

/* ---------------- Month Model + Utils ---------------- */

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
        val c = Calendar.getInstance()
        c.set(year, month, d, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        cells.add(DayCell(millis = c.timeInMillis, dayText = d.toString()))
    }

    while (cells.size % 7 != 0) cells.add(null)

    return MonthModel(title = title, cells = cells)
}

private fun dayKey(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

private fun isSameDayFast(a: Long, b: Long): Boolean = dayKey(a) == dayKey(b)

private fun formatYmd(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
    return sdf.format(Date(millis))
}

private fun formatHm(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.TAIWAN)
    return sdf.format(Date(millis))
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
            val date0 = d.dateMillis
            val step: Int? = 30 // ✅ 保留 30 分鐘步進

            fun hmToDigits(millis: Long): String = formatHm(millis).replace(":", "")

            // ✅ 用 TextFieldValue 才能穩定控制游標/輸入（避免 1103 問題）
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
                return TextFieldValue(
                    text = digits,
                    selection = TextRange(digits.length) // 游標永遠在末端，避免插入到中間
                )
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

                            // --- 開始時間：digits存放 + 視覺轉換顯示HH:mm ---
                            OutlinedTextField(
                                value = start,
                                onValueChange = { input ->
                                    val cleaned = digitsOnly(input)
                                    start = cleaned

                                    val off = TimeInput.digitsToOffsetMillis(cleaned.text, allowStepMinutes = step)
                                    if (off != null) {
                                        val newStart = TimeOptions.absoluteMillis(date0, off)
                                        onUpdateDraft { it.copy(startTimeMillis = newStart) }

                                        // end 不合法/早於 start -> 自動推 +1hr（仍符合 30 分步進）
                                        val endParsed = TimeInput.digitsToOffsetMillis(end.text, allowStepMinutes = step)
                                        if (endParsed == null || endParsed <= off) {
                                            val suggested = off + 60L * 60_000L
                                            val suggestedDigits = TimeOptions.offsetToLabel(suggested).replace(":", "")
                                            end = TextFieldValue(
                                                text = suggestedDigits,
                                                selection = TextRange(suggestedDigits.length)
                                            )
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

                            // --- 結束時間 ---
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
                    Button(
                        onClick = onSave,
                        enabled = timeError == null
                    ) { Text("儲存") }
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

/**
 * 將純數字 digits（例如 "1130"）顯示成 "11:30"
 * TextField 內部仍維持 digits，避免 IME 亂序。
 */
private object HhmmVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text // digits only
        val out = buildString {
            raw.forEachIndexed { i, c ->
                if (i == 2) append(':')
                append(c)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // digits -> with colon after 2 digits
                return when {
                    offset <= 2 -> offset
                    else -> offset + 1
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                // with colon -> digits
                return when {
                    offset <= 2 -> offset
                    offset == 3 -> 2
                    else -> offset - 1
                }
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
