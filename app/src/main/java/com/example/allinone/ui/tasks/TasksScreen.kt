package com.example.allinone.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allinone.ui.components.StandardTaskCard
import com.example.allinone.ui.components.TaskFilterChip
import com.example.allinone.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

// 暫時定義，以便 UI 正常運作，組員開發後可移除
data class TaskItem(
    val title: String,
    val quadrantId: Int,
    val progress: Int = 0, // 0: not yet, 1: in progress, 2: done
    val detail: String = "",
    val dueTime: Long? = null
) {
    val statusString: String
        get() = when (progress) {
            1 -> "in progress"
            2 -> "done"
            else -> "not yet"
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    initialShowEdit: Boolean = false // 用於 Preview 測試彈窗
) {
    var viewMode by remember { mutableStateOf("list") }
    var selectedQuadrants by remember { mutableStateOf(setOf(0, 1, 2, 3, 4)) }
    var selectedProgress by remember { mutableStateOf(setOf(0, 1, 2)) } // 改用數值對應資料表
    var searchQuery by remember { mutableStateOf("") }

    // 控制編輯視窗
    var showEditScreen by remember { mutableStateOf(initialShowEdit) }
    var currentEditingTask by remember { mutableStateOf<TaskItem?>(null) }

    // 模擬資料列表
    val taskList = remember {
        mutableStateListOf(
            TaskItem("專案 A 報告", 0, 0),
            TaskItem("回覆緊急郵件", 1, 1),
            TaskItem("整理辦公桌", 2, 2),
            TaskItem("接聽推銷電話", 3, 1),
            TaskItem("未分類雜事", 4, 0)
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentEditingTask = null
                    showEditScreen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // 頂部控制區
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                TaskHeaderSection(viewMode) { viewMode = it }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search your plan...") },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape
                )

                if (viewMode == "list") {
                    Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val progressOptions = listOf("not yet" to 0, "in progress" to 1, "done" to 2)
                        progressOptions.forEach { (label, value) ->
                            TaskFilterChip(label = label, isSelected = selectedProgress.contains(value)) {
                                selectedProgress = if (selectedProgress.contains(value)) selectedProgress - value else selectedProgress + value
                            }
                        }
                    }
                } else {
                    QuadrantColorMatrix(selectedSet = selectedQuadrants) { id ->
                        selectedQuadrants = if (selectedQuadrants.contains(id)) selectedQuadrants - id else selectedQuadrants + id
                    }
                }
            }

            // 過濾邏輯
            val filteredTasks = taskList.filter { task ->
                val qMatch = selectedQuadrants.contains(task.quadrantId)
                val pMatch = if (viewMode == "list") selectedProgress.contains(task.progress) else true
                val sMatch = task.title.contains(searchQuery, ignoreCase = true)
                qMatch && pMatch && sMatch
            }

            if (filteredTasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks) { task ->
                        val index = taskList.indexOf(task)

                        // 轉換日期顯示格式
                        val displayDate = if (task.dueTime != null) {
                            SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(task.dueTime))
                        } else "No date"

                        StandardTaskCard(
                            title = task.title,
                            tags = listOf("Work", task.statusString),
                            date = displayDate,
                            status = task.statusString,
                            onCheckedChange = { isChecked ->
                                if (index != -1) {
                                    val newProgress = if (isChecked) 2 else 1
                                    taskList[index] = taskList[index].copy(progress = newProgress)
                                }
                            },
                            onEditClick = {
                                currentEditingTask = task
                                showEditScreen = true
                            },
                            modifier = Modifier.background(
                                getQuadrantColor(task.quadrantId).copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                        )
                    }
                }
            } else {
                EmptyStateView()
            }
        }

        // --- 全螢幕編輯/新增彈窗 ---
        if (showEditScreen) {
            Dialog(
                onDismissRequest = { showEditScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false) // 關鍵：全螢幕
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TaskEditScreen(
                        initialTitle = currentEditingTask?.title ?: "",
                        initialProgress = currentEditingTask?.progress ?: 0,
                        initialQuadrant = currentEditingTask?.quadrantId ?: 4,
                        initialDetail = currentEditingTask?.detail ?: "",
                        initialDueTime = currentEditingTask?.dueTime,
                        onBack = { showEditScreen = false },
                        onSave = { title, progress, quadrant, detail, dueTime ->
                            // 模擬儲存邏輯
                            val newTask = TaskItem(title, quadrant, progress, detail, dueTime)
                            if (currentEditingTask != null) {
                                val idx = taskList.indexOf(currentEditingTask)
                                if (idx != -1) taskList[idx] = newTask
                            } else {
                                taskList.add(newTask)
                            }
                            showEditScreen = false
                        }
                    )
                }
            }
        }
    }
}

// --- 以下輔助組件配合左1右4配置 ---

@Composable
fun QuadrantColorMatrix(selectedSet: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorQuadrantBtn("?", selectedSet.contains(4), 4, Modifier.weight(1.5f).fillMaxHeight()) { onToggle(4) }
        Column(modifier = Modifier.weight(8.5f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorQuadrantBtn("重要&不緊急", selectedSet.contains(0), 0, Modifier.weight(1f)) { onToggle(0) }
                ColorQuadrantBtn("重要&緊急", selectedSet.contains(1), 1, Modifier.weight(1f)) { onToggle(1) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorQuadrantBtn("不重要&不緊急", selectedSet.contains(2), 2, Modifier.weight(1f)) { onToggle(2) }
                ColorQuadrantBtn("不重要&緊急", selectedSet.contains(3), 3, Modifier.weight(1f)) { onToggle(3) }
            }
        }
    }
}

@Composable
fun ColorQuadrantBtn(label: String, isSelected: Boolean, quadrantId: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val baseColor = getQuadrantColor(quadrantId)
    val containerColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.15f)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp, fontSize = 11.sp),
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun TaskHeaderSection(viewMode: String, onModeChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tasks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.height(36.dp).width(160.dp)) {
                PillButton("list", viewMode == "list") { onModeChange("list") }
                PillButton("Quadrant", viewMode == "quadrant") { onModeChange("quadrant") }
            }
        }
    }
}

@Composable
fun PillButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxHeight().width(80.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("請選擇上方分類以顯示內容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun getQuadrantColor(type: Int): Color {
    return when (type) {
        0 -> Color(0xFF79AFE3)
        1 -> Color(0xFFE38684)
        2 -> Color(0xFF66A869)
        3 -> Color(0xFF925AA2)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    }
}

// --- Previews ---
@Preview(showSystemUi = true, name = "Task 列表首頁")
@Composable
fun TasksScreenMainPreview() {
    AppTheme { TasksScreen(initialShowEdit = false) }
}

@Preview(showSystemUi = true, name = "模擬點擊 FAB 或編輯後的跳轉視窗")
@Composable
fun TasksScreenPopUpPreview() {
    AppTheme { TasksScreen(initialShowEdit = true) }
}