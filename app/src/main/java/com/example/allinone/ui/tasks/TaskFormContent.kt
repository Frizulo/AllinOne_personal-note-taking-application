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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    tag: String,
    onTagChange: (String) -> Unit,
    progress: Int,
    onProgressChange: (Int) -> Unit,
    quadrant: Int,
    onQuadrantChange: (Int) -> Unit,
    detail: String,
    onDetailChange: (String) -> Unit,
    dueTime: Long?,
    onDueTimeChange: (Long?) -> Unit
) {
    val scroll = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(scroll)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("任務名稱") },
            placeholder = { Text("任務名稱...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tag,
            onValueChange = onTagChange,
            label = { Text("Tag") },
            placeholder = { Text("例如：Work / Study") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("進度", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = progress == 0,
                onClick = { onProgressChange(0) },
                label = { Text("Not yet") }
            )
            FilterChip(
                selected = progress == 1,
                onClick = { onProgressChange(1) },
                label = { Text("In progress") }
            )
            FilterChip(
                selected = progress == 2,
                onClick = { onProgressChange(2) },
                label = { Text("Done") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("重要緊急程度", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = detail,
            onValueChange = onDetailChange,
            label = { Text("詳細") },
            placeholder = { Text("(可選)輸入詳細備註內容...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (dueTime == null) "設定到期日" else "已設定到期日（可修改）")
        }
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
