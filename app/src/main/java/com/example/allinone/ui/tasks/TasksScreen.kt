package com.example.allinone.ui.tasks

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.ui.components.StandardTaskCard
import com.example.allinone.ui.components.TaskFilterChip
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel
) {
    var selectedQuadrants by remember { mutableStateOf(setOf(0, 1, 2, 3, 4)) }
    var selectedProgress by remember { mutableStateOf(setOf(0, 1, 2)) } // 改用數值對應資料表
    var searchQuery by remember { mutableStateOf("") }

    val uiState by viewModel.tasks.collectAsState()
    val appContext = LocalContext.current.applicationContext

    var showEdit by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }

    fun openAdd() {
        editing = null
        showEdit = true
    }
    fun openEdit(task: TaskEntity) {
        editing = task
        showEdit = true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openAdd() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            // 頂部控制區
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tasks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.syncOnce(appContext) }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                    Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val progressOptions = listOf("not yet" to 0, "in progress" to 1, "done" to 2)
                        progressOptions.forEach { (label, value) ->
                            TaskFilterChip(label = label, isSelected = selectedProgress.contains(value)) {
                                selectedProgress = if (selectedProgress.contains(value)) selectedProgress - value else selectedProgress + value
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; viewModel.setQuery(searchQuery) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search your plan...") },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape
                )

                QuadrantColorMatrix(selectedSet = selectedQuadrants) { id ->
                    selectedQuadrants = if (selectedQuadrants.contains(id)) selectedQuadrants - id else selectedQuadrants + id
                }
            }

            fun quadrantMatches(q: Int) =  q in selectedQuadrants
            fun progressMatches(p: Int) =  p in selectedProgress
            // 過濾邏輯
            val filteredTasks = uiState.filter { task ->
                quadrantMatches(task.quadrant) && progressMatches(task.progress)
            }


            if (filteredTasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.localId }) { task ->
                        StandardTaskCard(
                            title = task.title,
                            tags = listOf(task.tag, progressToText(task.progress)),
                            date = task.dueTimeMillis?.let(::formatDate) ?: "",
                            status = progressToStatus(task.progress),
                            description = task.detail.ifBlank { "（無詳細）" },
                            onEditClick = { openEdit(task) },
                            onDeleteClick = { viewModel.deleteTask(task.localId) },
                            onCheckedChange = { checked ->
                                val newProgress = if (checked) 2 else 1
                                viewModel.updateTask(
                                    localId = task.localId,
                                    title = task.title,
                                    detail = task.detail,
                                    tag = task.tag,
                                    quadrant = task.quadrant,
                                    progress = newProgress,
                                    dueTimeMillis = task.dueTimeMillis
                                )
                            },
                            modifier = Modifier.background(
                                getQuadrantColor(task.quadrant).copy(alpha = 0.5f),
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
        if (showEdit) {
            Dialog(
                onDismissRequest = { showEdit = false },
                properties = DialogProperties(usePlatformDefaultWidth = false) // 關鍵：全螢幕
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TaskEditScreen(
                        initialTitle = editing?.title ?: "",
                        initialTag = editing?.tag ?: "Work",
                        initialProgress = editing?.progress ?: 0,
                        initialQuadrant = editing?.quadrant ?: 4,
                        initialDetail = editing?.detail ?: "",
                        initialDueTime = editing?.dueTimeMillis,
                        onBack = { showEdit = false },
                        onSave = { title, tag, progress, quadrant, detail, dueTime ->
                            val t = editing
                            if (t == null) {
                                viewModel.addTask(
                                    title = title,
                                    detail = detail,
                                    tag = tag,
                                    quadrant = quadrant,
                                    progress = progress,
                                    dueTimeMillis = dueTime
                                )
                            } else {
                                viewModel.updateTask(
                                    localId = t.localId,
                                    title = title,
                                    detail = detail,
                                    tag = tag,
                                    quadrant = quadrant,
                                    progress = progress,
                                    dueTimeMillis = dueTime
                                )
                            }
                            showEdit = false
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorQuadrantBtn("?", selectedSet.contains(4), 4, Modifier
            .weight(1.5f)
            .fillMaxHeight()) { onToggle(4) }
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
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("請選擇上方分類以顯示內容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

private fun progressToStatus(progress: Int): String = when (progress) {
    1 -> "in progress"
    2 -> "done"
    else -> "not yet"
}

private fun progressToText(progress: Int): String = when (progress) {
    1 -> "In progress"
    2 -> "Done"
    else -> "Not yet"
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return sdf.format(Date(ms))
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
