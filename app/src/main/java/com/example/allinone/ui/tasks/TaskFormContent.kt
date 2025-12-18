package com.example.allinone.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allinone.ui.components.TaskFilterChip
import com.example.allinone.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    progress: Int,                // 0:not yet, 1:in progress, 2:done
    onProgressChange: (Int) -> Unit,
    quadrant: Int,                // 0-3:象限, 4:?
    onQuadrantChange: (Int) -> Unit,
    detail: String,               // 對應資料表 detail
    onDetailChange: (String) -> Unit,
    dueTime: Long?,              // 對應資料表 due_time
    onDueTimeChange: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueTime ?: System.currentTimeMillis()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. 任務標題 (title)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Task Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("任務名稱...") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }

        // 2. 截止日期 (due_time)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Due Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Switch(
                    checked = dueTime != null,
                    onCheckedChange = { isChecked ->
                        onDueTimeChange(if (isChecked) System.currentTimeMillis() else null)
                    }
                )
            }

            if (dueTime != null) {
                val dateString = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(dueTime))
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(Modifier.width(12.dp))
                        Text(text = dateString, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text("修改", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            }
        }

        // 3. 進度狀態 (progress)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statusList = listOf("not yet" to 0, "in progress" to 1, "done" to 2)
                statusList.forEach { (label, value) ->
                    TaskFilterChip(label = label, isSelected = progress == value) { onProgressChange(value) }
                }
            }
        }

        // 4. 優先象限 (quadrant) - 依照 bcfaed.png 佈局
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Priority (Quadrant)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorQuadrantBtn("?", quadrant == 4, 4, Modifier.weight(1.5f).fillMaxHeight()) { onQuadrantChange(4) }
                Column(modifier = Modifier.weight(8.5f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColorQuadrantBtn("重要&不緊急", quadrant == 0, 0, Modifier.weight(1f)) { onQuadrantChange(0) }
                        ColorQuadrantBtn("重要&緊急", quadrant == 1, 1, Modifier.weight(1f)) { onQuadrantChange(1) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColorQuadrantBtn("不重要&不緊急", quadrant == 2, 2, Modifier.weight(1f)) { onQuadrantChange(2) }
                        ColorQuadrantBtn("不重要&緊急", quadrant == 3, 3, Modifier.weight(1f)) { onQuadrantChange(3) }
                    }
                }
            }
        }

        // 5. 備註 (detail)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = detail,
                onValueChange = onDetailChange,
                placeholder = { Text("(可選)輸入詳細備註內容...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = MaterialTheme.shapes.medium,
                minLines = 4
            )
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDueTimeChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }
}