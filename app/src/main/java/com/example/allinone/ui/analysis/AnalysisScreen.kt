package com.example.allinone.ui.analysis

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.ScheduleSlotWithTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


@Composable
fun AnalysisScreen(
    vm: AnalysisViewModel
) {
    val startDay by vm.startDayMillis.collectAsState()
    val endDay by vm.endDayMillis.collectAsState()
    val keyword by vm.keyword.collectAsState()
    val includeTask by vm.includeTask.collectAsState()
    val includeFree by vm.includeFree.collectAsState()
    val ui by vm.uiState.collectAsState()

    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { vm.runSearch() }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp, 48.dp)
        ) {

            Text("數據報表 / 查詢", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            // -------------------------
            // Filters (✅獨立底色)
            // -------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("篩選條件", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = vm::clearFilters) { Text("清空") }
                        Button(onClick = vm::runSearch) { Text("查詢") }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateBox(
                            modifier = Modifier.weight(1f),
                            label = "開始日期",
                            dayMillis = startDay,
                            onPick = { vm.setStartDay(it) },
                            onClear = { vm.setStartDay(null) }
                        )
                        DateBox(
                            modifier = Modifier.weight(1f),
                            label = "結束日期",
                            dayMillis = endDay,
                            onPick = { vm.setEndDay(it) },
                            onClear = { vm.setEndDay(null) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = keyword,
                        onValueChange = vm::setKeyword,
                        singleLine = true,
                        label = { Text("篩選標題 + 內容 (關鍵字)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeTask, onCheckedChange = vm::setIncludeTask)
                            Text("顯示 Task")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeFree, onCheckedChange = vm::setIncludeFree)
                            Text("顯示 純行程")
                        }
                    }

                    if (ui.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // -------------------------
            // Dashboard
            // -------------------------
            val summary = ui.summary
            val totalH = msToHours(summary.totalMs)
            val taskH = msToHours(summary.taskMs)
            val freeH = msToHours(summary.freeMs)
            val focusDensity =
                if (summary.totalMs <= 0L) 0f else (summary.taskMs.toFloat() / summary.totalMs.toFloat())

            // ✅總累計時長更突出：primaryContainer + 大字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCardEmphasis(
                    title = "總累計時長",
                    value = fmtHours(totalH),
                    modifier = Modifier.weight(3f)
                )
                MetricCard(
                    title = "Task 佔比（專注度）",
                    value = "${(focusDensity * 100).toInt()}%",
                    modifier = Modifier.weight(2f),
                    container = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    "Task 小時",
                    fmtHours(taskH),
                    Modifier.weight(1f),
                    MaterialTheme.colorScheme.surfaceVariant
                )
                MetricCard(
                    "純行程 小時",
                    fmtHours(freeH),
                    Modifier.weight(1f),
                    MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Text(
                text = "「Task 佔比」＝ Task 時間 ÷ 總時間，用來看你有多少時間花在可交付的任務上。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            // -------------------------
            // Chart
            // -------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "四時段工作時長（Task / 純行程）",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))

                    val buckets = remember(summary.stats4x3) {
                        listOf(
                            BucketUi(
                                "深夜",
                                "00–06",
                                summary.stats4x3.sleepTask,
                                summary.stats4x3.sleepFree
                            ),
                            BucketUi(
                                "早",
                                "06–12",
                                summary.stats4x3.morningTask,
                                summary.stats4x3.morningFree
                            ),
                            BucketUi(
                                "中",
                                "12–18",
                                summary.stats4x3.afternoonTask,
                                summary.stats4x3.afternoonFree
                            ),
                            BucketUi(
                                "晚",
                                "18–24",
                                summary.stats4x3.eveningTask,
                                summary.stats4x3.eveningFree
                            ),
                        )
                    }
                    val totalAllMs =
                        remember(buckets) { buckets.sumOf { it.totalMs }.coerceAtLeast(1L) }

                    StackedBars4WithAxis(
                        buckets = buckets,
                        selectedIndex = selectedBucketIndex,
                        onSelect = { selectedBucketIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(Modifier.height(10.dp))
                    LegendRow()

                    // ✅選擇後資訊：獨立 Highlight Card（不同底色），避免跑版 + 更商業
                    val selected = selectedBucketIndex?.let { buckets.getOrNull(it) }
                    if (selected != null) {
                        val selectedTotal = selected.totalMs
                        val percent =
                            (selectedTotal.toDouble() / totalAllMs.toDouble() * 100.0).coerceIn(
                                0.0,
                                100.0
                            )

                        Spacer(Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = "已選：${selected.label} ${selected.range}  •  佔比 ${percent.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Task ${fmtHours(msToHours(selected.taskMs))} / 純行程 ${
                                        fmtHours(
                                            msToHours(selected.freeMs)
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "該時段總時長：${fmtHours(msToHours(selectedTotal))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "提示：點擊柱狀圖查看該時段的時長與佔比",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -------------------------
            // List title + ✅色碼說明
            // -------------------------
            Text(
                text = "本次查詢清單(共 ${ui.items.size} 筆)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(6.dp))

            TimeBucketLegendRow() // ✅ 新增：深夜/早/中/晚標註列

            Spacer(Modifier.height(8.dp))

            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }

            // -------------------------
            // List
            // -------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.items.forEach { item ->
                    SlotRowWithTimeBuckets(item)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "本次查詢清單(共 ${ui.items.size} 筆) 已結束",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 0.dp)
    )
}

@Composable
private fun DateBox(
    modifier: Modifier = Modifier,
    label: String,
    dayMillis: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit
) {
    val ctx = LocalContext.current
    val cal = remember { Calendar.getInstance() }
    val text = remember(dayMillis) { if (dayMillis == null) "未設定" else formatDay(dayMillis) }

    OutlinedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val base = dayMillis ?: System.currentTimeMillis()
                    cal.timeInMillis = base
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d ->
                            val picked = Calendar.getInstance().apply {
                                set(Calendar.YEAR, y)
                                set(Calendar.MONTH, m)
                                set(Calendar.DAY_OF_MONTH, d)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            onPick(picked)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) { Text("選擇") }
                TextButton(onClick = onClear) { Text("清除") }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    container: Color
) {
    Card(
        modifier = modifier,
        colors = cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun MetricCardEmphasis(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Task")
        LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "純行程")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TimeBucketLegendRow() {
    // 用你列表同一套顏色（飽和、辨識度高）
    val nightColor = MaterialTheme.colorScheme.primary
    val morningColor = MaterialTheme.colorScheme.secondary
    val afternoonColor = MaterialTheme.colorScheme.tertiary
    val eveningColor = MaterialTheme.colorScheme.error

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeLegendDot(color = nightColor, label = "深夜 00–06")
        TimeLegendDot(color = morningColor, label = "早 06–12")
        TimeLegendDot(color = afternoonColor, label = "中 12–18")
        TimeLegendDot(color = eveningColor, label = "晚 18–24")
    }
}

@Composable
private fun TimeLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}


// -------------------------
// Chart (Axis + Click + ✅選取高亮塗色區)
// -------------------------
private data class BucketUi(
    val label: String,
    val range: String,
    val taskMs: Long,
    val freeMs: Long
) {
    val totalMs: Long get() = (taskMs + freeMs).coerceAtLeast(0L)
}

@Composable
private fun StackedBars4WithAxis(
    buckets: List<BucketUi>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    val freeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.86f)
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val selectedStroke = MaterialTheme.colorScheme.primary
    val selectedHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun colorToArgb(c: Color): Int {
        val a = (c.alpha * 255f).toInt().coerceIn(0, 255)
        val r = (c.red * 255f).toInt().coerceIn(0, 255)
        val g = (c.green * 255f).toInt().coerceIn(0, 255)
        val b = (c.blue * 255f).toInt().coerceIn(0, 255)
        return android.graphics.Color.argb(a, r, g, b)
    }

    val labelArgb = remember(labelColor) { colorToArgb(labelColor) }

    // ✅ 1 小時刻度
    val maxTotalMs = (buckets.maxOfOrNull { it.totalMs } ?: 0L).coerceAtLeast(1L)
    val maxHoursRaw = msToHours(maxTotalMs)
    val topTickHours = kotlin.math.ceil(maxHoursRaw.toDouble()).toInt().coerceAtLeast(1)

    var barHitRects by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // ✅ 左邊 Y 軸留給數字的寬度（不再用 Column 畫）
    val yAxisWidth = 44.dp
    val yAxisGap = 8.dp

    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.width(yAxisWidth))
        Spacer(Modifier.width(yAxisGap))

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .pointerInput(buckets, barHitRects) {
                    detectTapGestures { offset ->
                        val idx = barHitRects.indexOfFirst { it.contains(offset) }
                        if (idx != -1) onSelect(idx)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // -------- layout (px) --------
                val leftPad = 6f
                val rightPad = 6f
                val topPad = 6f
                val bottomPad = 78f // X 軸兩行文字 + 下方提示

                val plotH = h - topPad - bottomPad
                val plotBottom = topPad + plotH

                // -------- grid (每 1 小時) --------
                for (hour in 0..topTickHours) {
                    val ratio = hour.toFloat() / topTickHours.toFloat()
                    val y = plotBottom - (plotH * ratio)

                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f
                    )
                }

                // x-axis line
                drawLine(
                    color = axisColor,
                    start = Offset(0f, plotBottom),
                    end = Offset(w, plotBottom),
                    strokeWidth = 3f
                )

                // -------- bars geometry --------
                val n = buckets.size.coerceAtLeast(1)
                val gap = 12f
                val barW = (w - leftPad - rightPad - gap * (n - 1)) / n.toFloat()

                val rectList = ArrayList<Rect>(n)

                buckets.forEachIndexed { idx, b ->
                    val x = leftPad + idx * (barW + gap)

                    val totalHrs = msToHours(b.totalMs)
                    val heightRatio = (totalHrs / topTickHours.toFloat()).coerceIn(0f, 1f)
                    val barH = plotH * heightRatio
                    val barTop = plotBottom - barH

                    val total = b.totalMs
                    val taskRatio = if (total == 0L) 0f else b.taskMs.toFloat() / total.toFloat()
                    val taskH = barH * taskRatio
                    val freeH = barH - taskH

                    if (selectedIndex == idx) {
                        drawRect(
                            color = selectedHighlight,
                            topLeft = Offset(x - 6f, topPad),
                            size = Size(barW + 12f, plotH)
                        )
                    }

                    if (taskH > 0f) {
                        drawRect(
                            color = taskColor,
                            topLeft = Offset(x, barTop),
                            size = Size(barW, taskH)
                        )
                    }
                    if (freeH > 0f) {
                        drawRect(
                            color = freeColor,
                            topLeft = Offset(x, barTop + taskH),
                            size = Size(barW, freeH)
                        )
                    }

                    val rect = Rect(
                        left = x,
                        top = barTop,
                        right = x + barW,
                        bottom = plotBottom
                    )
                    rectList.add(rect)

                    if (selectedIndex == idx) {
                        drawRect(
                            color = selectedStroke,
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height),
                            style = Stroke(width = 6f)
                        )
                    }
                }

                barHitRects = rectList

                // -------- X labels（對齊柱子中心）--------
                val labelY1 = plotBottom + 34f
                val labelY2 = plotBottom + 60f

                val xPaint1 = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 28f
                    color = labelArgb
                }
                val xPaint2 = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 24f
                    color = labelArgb
                }

                drawContext.canvas.nativeCanvas.apply {
                    buckets.forEachIndexed { idx, b ->
                        val x = leftPad + idx * (barW + gap)
                        val centerX = x + barW / 2f
                        drawText(b.label, centerX, labelY1, xPaint1)
                        drawText(b.range, centerX, labelY2, xPaint2)
                    }
                }

                // ✅ -------- Y labels：畫在「plot 區域」對齊 grid 線（不含 bottomPad）--------
                val yPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                    textSize = 26f
                    color = labelArgb
                }

                // 讓文字不要貼線：基線微調
                val baselineAdjust = 8f

                // 這裡的 x 是「Canvas 左邊外面」畫不到，所以我們要回傳給外層去畫
                // 做法：用 drawIntoCanvas 畫在 Canvas 左側負座標（可行）
                drawContext.canvas.nativeCanvas.apply {
                    for (hour in 0..topTickHours) {
                        val ratio = hour.toFloat() / topTickHours.toFloat()
                        val y = plotBottom - (plotH * ratio)

                        // 右對齊到 yAxisWidth 的位置（用負座標往左畫）
                        // yAxisWidth=44dp 大約 44*dp，這裡用 52f 保守留白
                        drawText(
                            hour.toString(),
                            -12f, // 右對齊基準點（越小越往左）
                            y + baselineAdjust,
                            yPaint
                        )
                    }
                }
            }
        }
    }
}




// -------------------------
// List row (✅顏色辨識度：改成膠囊 chip + 可顯示雙色)
// -------------------------
@Composable
private fun SlotRowWithTimeBuckets(item: ScheduleSlotWithTask) {
    val title = item.taskTitle ?: item.slot.customTitle ?: "(未命名)"
    val isTask = item.slot.localTaskId != null
    val durMs = (item.slot.endTimeMillis - item.slot.startTimeMillis).coerceAtLeast(0L)

    // 時段顏色（更飽和：用 main 色系，而不是 container 色）
    val nightColor = MaterialTheme.colorScheme.primary
    val morningColor = MaterialTheme.colorScheme.secondary
    val afternoonColor = MaterialTheme.colorScheme.tertiary
    val eveningColor = MaterialTheme.colorScheme.error

    val bucketIds = remember(item.slot.startTimeMillis, item.slot.endTimeMillis) {
        computeTimeBuckets(item.slot.startTimeMillis, item.slot.endTimeMillis)
            .distinct()
            .take(2)
    }

    fun colorOf(idx: Int): Color? = when (idx) {
        0 -> nightColor
        1 -> morningColor
        2 -> afternoonColor
        3 -> eveningColor
        else -> null
    }

    val c1 = bucketIds.getOrNull(0)?.let(::colorOf)
    val c2 = bucketIds.getOrNull(1)?.let(::colorOf)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                // ✅辨識度更高的「時段膠囊」：單色 / 雙色
                TimeBucketPill(
                    primary = c1 ?: MaterialTheme.colorScheme.outline,
                    secondary = c2,
                    modifier = Modifier.padding(end = 10.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                AssistChip(
                    onClick = { },
                    label = { Text(if (isTask) "Task" else "純行程") }
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${formatDay(item.slot.dateMillis)}  ${formatHm(item.slot.startTimeMillis)} - ${formatHm(item.slot.endTimeMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "時長：${fmtHours(msToHours(durMs))}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TimeBucketPill(
    primary: Color,
    secondary: Color?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .height(14.dp)
            .width(26.dp)
            .background(primary.copy(alpha = 0.95f), shape)
    ) {
        if (secondary != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .background(secondary.copy(alpha = 0.95f), shape)
            )
        }
        // 外框增加對比（讓亮底也看得清楚）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent, shape)
        )
    }
}

/**
 * 回傳此事件橫跨哪些時段（0=00-06, 1=06-12, 2=12-18, 3=18-24）
 */
private fun computeTimeBuckets(startMillis: Long, endMillis: Long): List<Int> {
    val day = 24 * 60 * 60 * 1000L
    fun t(m: Long): Long = (m % day + day) % day

    val s = t(startMillis)
    val eRaw = t(endMillis)
    val e = if (eRaw < s) s else eRaw

    fun bucketOf(timeInDay: Long): Int {
        val h = (timeInDay / (60 * 60 * 1000L)).toInt()
        return when (h) {
            in 0..5 -> 0
            in 6..11 -> 1
            in 12..17 -> 2
            else -> 3
        }
    }

    val boundaries = listOf(
        0L,
        6 * 60 * 60 * 1000L,
        12 * 60 * 60 * 1000L,
        18 * 60 * 60 * 1000L,
        24 * 60 * 60 * 1000L
    )

    val result = mutableListOf<Int>()
    var cur = s
    while (cur < e) {
        val b = bucketOf(cur)
        if (result.lastOrNull() != b) result.add(b)
        val nextBoundary = boundaries.firstOrNull { it > cur } ?: (24 * 60 * 60 * 1000L)
        cur = min(e, nextBoundary)
    }
    if (result.isEmpty()) result.add(bucketOf(s))
    return result
}

// -------------------------
// Formatting helpers
// -------------------------
private fun msToHours(ms: Long): Float = ms.toFloat() / 3_600_000f

private fun fmtHours(h: Float): String {
    val safe = max(0f, h)
    val rounded = (safe * 10f).toInt() / 10f
    return "$rounded 小時"
}

private fun formatDay(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatHm(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun fmtNum(v: Float): String {
    val i = v.toInt()
    return if (kotlin.math.abs(v - i.toFloat()) < 0.001f) i.toString()
    else String.format(Locale.getDefault(), "%.1f", v)
}

private fun niceCeil(v: Float): Float {
    if (v <= 0f) return 1f
    val exp = floor(kotlin.math.log10(v.toDouble())).toInt()
    val base = 10.0.pow(exp.toDouble()).toFloat()
    val n = v / base
    val nice = when {
        n <= 1f -> 1f
        n <= 2f -> 2f
        n <= 5f -> 5f
        else -> 10f
    }
    return nice * base
}
