package com.example.allinone.ui.analysis

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.allinone.data.local.ScheduleSlotWithTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun AnalysisScreen(vm: AnalysisViewModel) {
    val startDay by vm.startDayMillis.collectAsState()
    val endDay by vm.endDayMillis.collectAsState()
    val keyword by vm.keyword.collectAsState()
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
            Text(
                "時間數據分析",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // -------------------------
            // 查詢格子 (Filters)
            // -------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("查詢區間與條件", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = vm::clearFilters) { Text("重置") }
                        Button(onClick = vm::runSearch) { Text("執行查詢") }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateBox(
                            modifier = Modifier.weight(1f),
                            label = "開始日期",
                            millis = startDay,
                            onPick = { vm.setStartDay(it) },
                            onClear = { vm.setStartDay(null) }
                        )
                        DateBox(
                            modifier = Modifier.weight(1f),
                            label = "結束日期",
                            millis = endDay,
                            onPick = { vm.setEndDay(it) },
                            onClear = { vm.setEndDay(null) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyword,
                        onValueChange = vm::setKeyword,
                        label = { Text("搜尋標題") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (ui.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -------------------------
            // 數據看板 (Dashboard)
            // -------------------------
            val summary = ui.summary
            val totalH = msToHours(summary.totalMs)

            MetricCardEmphasis(
                title = "總累計排程時長",
                value = fmtHours(totalH),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // -------------------------
            // 時段分佈圖表 (Chart)
            // -------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("各時段時長分佈", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    val buckets = remember(summary.stats4x3) {
                        listOf(
                            BucketUi("夜", "00–06", summary.stats4x3.sleepTask + summary.stats4x3.sleepFree),
                            BucketUi("早", "06–12", summary.stats4x3.morningTask + summary.stats4x3.morningFree),
                            BucketUi("中", "12–18", summary.stats4x3.afternoonTask + summary.stats4x3.afternoonFree),
                            BucketUi("晚", "18–24", summary.stats4x3.eveningTask + summary.stats4x3.eveningFree),
                        )
                    }
                    val totalAllMs = remember(buckets) { buckets.sumOf { it.totalMs }.coerceAtLeast(1L) }

                    // ✅ 圖表更大一點（你要放大）
                    SimpleBarChart(
                        buckets = buckets,
                        selectedIndex = selectedBucketIndex,
                        onSelect = { selectedBucketIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp) // 原本 200 -> 240
                    )

                    Spacer(Modifier.height(16.dp))

                    // ✅ 選擇後詳細資訊：0 時段也顯示（不再點不了）
                    val selected = selectedBucketIndex?.let { buckets.getOrNull(it) }
                    if (selected != null) {
                        val percent = (selected.totalMs.toDouble() / totalAllMs.toDouble() * 100).toInt()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("${selected.label}（${selected.range}）", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        fmtHours(msToHours(selected.totalMs)),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "佔比 $percent%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "提示：點擊長條圖可查看該時段佔比與詳細時長",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // -------------------------
            // 查詢結果清單
            // -------------------------
            Text(
                text = "本次查詢清單(共 ${ui.items.size} 筆)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))
            TimeBucketLegendRow()
            Spacer(Modifier.height(12.dp))

            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.items.forEach { item ->
                    SlotRowWithTimeBuckets(item)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "本次查詢清單(共 ${ui.items.size} 筆) 已結束",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// -------------------------
// UI：圖表
// -------------------------
private data class BucketUi(val label: String, val range: String, val totalMs: Long)

@Composable
private fun SimpleBarChart(
    buckets: List<BucketUi>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ 先在 Composable 區域把顏色取出（避免 Canvas 內呼叫 MaterialTheme）
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedStroke = MaterialTheme.colorScheme.primary
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    val maxVal = remember(buckets) { buckets.maxOf { it.totalMs }.coerceAtLeast(1L) }
    val maxH = remember(maxVal) { msToHours(maxVal) }
    val ticks = remember(maxH) { max(1, ceil(maxH.toDouble()).toInt()) }

    var hitRects by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // ✅ nativeCanvas 文字 paint（不要在 Canvas 內 new 一堆）
    val labelPaint = remember(labelColor) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
            textSize = 30f
        }
    }

    Canvas(
        modifier = modifier.pointerInput(buckets, hitRects) {
            detectTapGestures { offset ->
                val idx = hitRects.indexOfFirst { it.contains(offset) }
                if (idx != -1) onSelect(idx) // ✅ 0 時段也能點
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val bottomArea = 56f
        val chartH = h - bottomArea

        // grid (Y 軸 1 小時單位)
        for (i in 0..ticks) {
            val y = chartH - (chartH * (i.toFloat() / ticks))
            drawLine(gridColor, Offset(0f, y), Offset(w, y), 1.dp.toPx())
        }

        // bars geometry：柱子更粗、間距更小
        val barGap = 12.dp.toPx()
        val barW = (w - (barGap * (buckets.size - 1))) / buckets.size
        val newRects = ArrayList<Rect>(buckets.size)

        buckets.forEachIndexed { i, b ->
            val barH = chartH * (b.totalMs.toFloat() / maxVal.toFloat())
            val left = i * (barW + barGap)
            val top = chartH - barH

            // ✅ hit rect 覆蓋整個欄位區（0 高度也能點）
            val hit = Rect(left, 0f, left + barW, chartH)
            newRects.add(hit)

            // ✅ selected 背景
            if (selectedIndex == i) {
                drawRect(
                    color = selectedBg,
                    topLeft = Offset(left - 6f, 0f),
                    size = Size(barW + 12f, chartH)
                )
            }

            // bar（0 時段畫淡柱）
            val fill = if (b.totalMs > 0) barColor else barColor.copy(alpha = 0.12f)
            drawRect(
                color = fill,
                topLeft = Offset(left, top),
                size = Size(barW, barH)
            )

            // selection stroke（0 時段也給小框）
            if (selectedIndex == i) {
                val frameTop = if (barH > 0f) top else (chartH - 6f)
                val frameH = if (barH > 0f) barH else 6f
                drawRect(
                    color = selectedStroke,
                    topLeft = Offset(left, frameTop),
                    size = Size(barW, frameH),
                    style = Stroke(3.dp.toPx())
                )
            }

            // X label
            drawContext.canvas.nativeCanvas.drawText(
                b.label,
                left + barW / 2f,
                chartH + 36f,
                labelPaint
            )
        }

        hitRects = newRects
    }
}


// -------------------------
// UI：列表（保留時段色碼膠囊 + 右側時長）
// -------------------------
@Composable
private fun SlotRowWithTimeBuckets(item: ScheduleSlotWithTask) {
    val title = item.taskTitle ?: item.slot.customTitle ?: "(無標題)"
    val durMs = (item.slot.endTimeMillis - item.slot.startTimeMillis).coerceAtLeast(0L)
    val durText = fmtHours(msToHours(durMs))

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
        colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeBucketPill(
                    primary = c1 ?: MaterialTheme.colorScheme.outline,
                    secondary = c2,
                    modifier = Modifier.padding(end = 10.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = durText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${formatDay(item.slot.dateMillis)}  ${formatHm(item.slot.startTimeMillis)} - ${formatHm(item.slot.endTimeMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeBucketLegendRow() {
    val nightColor = MaterialTheme.colorScheme.primary
    val morningColor = MaterialTheme.colorScheme.secondary
    val afternoonColor = MaterialTheme.colorScheme.tertiary
    val eveningColor = MaterialTheme.colorScheme.error

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        TimeLegendDot(nightColor, "夜 00–06")
        TimeLegendDot(morningColor, "早 06–12")
        TimeLegendDot(afternoonColor, "中 12–18")
        TimeLegendDot(eveningColor, "晚 18–24")
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

@Composable
private fun TimeBucketPill(primary: Color, secondary: Color?, modifier: Modifier = Modifier) {
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
    }
}

// -------------------------
// 小組件：日期格
// -------------------------
@Composable
private fun DateBox(
    modifier: Modifier,
    label: String,
    millis: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit
) {
    val ctx = LocalContext.current
    OutlinedCard(modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (millis == null) "不限" else formatDay(millis),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row {
                TextButton(onClick = {
                    val c = Calendar.getInstance()
                    if (millis != null) c.timeInMillis = millis
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d ->
                            onPick(
                                Calendar.getInstance().apply {
                                    set(y, m, d, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            )
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) { Text("設定") }

                if (millis != null) {
                    TextButton(onClick = onClear) { Text("清除") }
                }
            }
        }
    }
}

// -------------------------
// ✅ Metric：總累計（同底色 + 外框強調，不撞查詢色）
// -------------------------
@Composable
private fun MetricCardEmphasis(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        colors = cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, borderColor.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側 accent bar（更像你想要的「用原本深色當外框」）
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(54.dp)
                    .background(borderColor.copy(alpha = 0.85f), RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// -------------------------
// 時段判斷：回傳此事件橫跨哪些時段（0=00-06, 1=06-12, 2=12-18, 3=18-24）
// -------------------------
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
// helpers
// -------------------------
private fun msToHours(ms: Long): Float = ms.toFloat() / 3_600_000f
private fun fmtHours(h: Float): String = String.format(Locale.getDefault(), "%.1f 小時", max(0f, h))
private fun formatDay(m: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(m))
private fun formatHm(m: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m))
