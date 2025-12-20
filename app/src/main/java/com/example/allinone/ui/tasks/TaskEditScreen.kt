package com.example.allinone.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    initialTitle: String,
    initialTag: String,
    initialProgress: Int = 0,
    initialQuadrant: Int = 4,
    initialDetail: String = "",
    initialDueTime: Long? = null,
    onBack: () -> Unit,
    onSave: (title: String, tag: String, progress: Int, quadrant: Int, detail: String, dueTime: Long?) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var tag by remember { mutableStateOf(initialTag) }
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
                        onClick = { onSave(title, tag, progress, quadrant, detail, dueTime) },
                        enabled = title.isNotBlank()
                    ) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TaskFormContent(
                title = title, onTitleChange = { title = it },
                tag = tag, onTagChange = { tag = it },
                progress = progress, onProgressChange = { progress = it },
                quadrant = quadrant, onQuadrantChange = { quadrant = it },
                detail = detail, onDetailChange = { detail = it },
                dueTime = dueTime, onDueTimeChange = { dueTime = it }
            )
        }
    }
}
