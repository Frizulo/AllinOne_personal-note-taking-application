package com.example.allinone.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.allinone.ui.theme.AppTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    // 改為接收基本參數，以便組員自由對接資料模型
    initialTitle: String = "",
    initialProgress: Int = 0,
    initialQuadrant: Int = 4,
    initialDetail: String = "",
    initialDueTime: Long? = null,
    onBack: () -> Unit,
    // 儲存時回傳所有基本欄位，不強迫使用特定 Data Class
    onSave: (title: String, progress: Int, quadrant: Int, detail: String, dueTime: Long?) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var progress by remember { mutableStateOf(initialProgress) }
    var quadrant by remember { mutableStateOf(initialQuadrant) }
    var detail by remember { mutableStateOf(initialDetail) }
    var dueTime by remember { mutableStateOf(initialDueTime) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (initialTitle.isEmpty()) "新增任務" else "編輯任務") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(
                        onClick = { onSave(title, progress, quadrant, detail, dueTime) },
                        enabled = title.isNotBlank()
                    ) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TaskFormContent(
                title = title, onTitleChange = { title = it },
                progress = progress, onProgressChange = { progress = it },
                quadrant = quadrant, onQuadrantChange = { quadrant = it },
                detail = detail, onDetailChange = { detail = it },
                dueTime = dueTime, onDueTimeChange = { dueTime = it }
            )
        }
    }
}
@Preview(showSystemUi = true)
@Composable
fun TaskEditScreenPreview() {
    AppTheme {
        TaskEditScreen(
            initialTitle = "測試任務",
            onBack = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}