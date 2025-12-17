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
import com.example.allinone.ui.components.StandardTaskCard
import com.example.allinone.ui.components.TaskFilterChip
import com.example.allinone.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    var viewMode by remember { mutableStateOf("list") }

    // 預設全選
    var selectedQuadrants by remember { mutableStateOf(setOf(0, 1, 2, 3, 4)) }
    var selectedProgress by remember { mutableStateOf(setOf("in progress", "not yet", "done")) }
    var searchQuery by remember { mutableStateOf("") }

    // 使用 mutableStateListOf 確保列表變動能觸發 UI 重繪
    val taskList = remember {
        mutableStateListOf(
            TaskItem("專案 A 報告", 0, "not yet"),
            TaskItem("回覆緊急郵件", 1, "in progress"),
            TaskItem("整理辦公桌", 2, "done"),
            TaskItem("接聽推銷電話", 3, "in progress"),
            TaskItem("未分類雜事", 4, "not yet")
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: 新增邏輯 */ },
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
                        listOf("in progress", "not yet", "done").forEach { label ->
                            TaskFilterChip(label = label, isSelected = selectedProgress.contains(label)) {
                                selectedProgress = if (selectedProgress.contains(label)) selectedProgress - label else selectedProgress + label
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
                val pMatch = if (viewMode == "list") selectedProgress.contains(task.status) else true
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
                        // 取得原始索引以利更新狀態
                        val index = taskList.indexOf(task)

                        StandardTaskCard(
                            title = task.title,
                            tags = listOf("Work", task.status),
                            date = "2025.12.18",
                            status = task.status, // 【關鍵修正】傳入狀態，控制勾選與刪除線
                            onCheckedChange = { isChecked ->
                                // 【關鍵修正】連動邏輯：勾選則 done，取消勾選則 in progress
                                if (index != -1) {
                                    val newStatus = if (isChecked) "done" else "in progress"
                                    taskList[index] = taskList[index].copy(status = newStatus)
                                }
                            },
                            modifier = Modifier.background(
                                getQuadrantColor(task.quadrantId).copy(alpha = 0.25f),
                                RoundedCornerShape(20.dp)
                            )
                        )
                    }
                }
            } else {
                EmptyStateView()
            }
        }
    }
}

// --- 以下輔助組件保持不變 ---

@Composable
fun QuadrantColorMatrix(selectedSet: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            ColorQuadrantBtn("?", isSelected = selectedSet.contains(4), quadrantId = 4) { onToggle(4) }
        }
        Box(modifier = Modifier.weight(8.8f), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp, fontSize = 11.sp),
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
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }
}

data class TaskItem(
    val title: String,
    val quadrantId: Int,
    val status: String = "not yet"
)

@Preview(showBackground = true)
@Composable
fun TasksPreview() { AppTheme { TasksScreen() } }