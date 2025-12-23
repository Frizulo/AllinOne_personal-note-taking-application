package com.example.allinone.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allinone.ui.theme.AppTheme

@Composable
fun StandardTaskCard(
    title: String,
    tags: List<String>,
    date: String,
    status: String = "not yet",
    description: String = "這是一段預設的任務詳細敘述，點擊卡片可展開查看動畫效果。",
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // 這裡的 isDone 是由外部傳入的 status 決定的
    val isDone = status == "done"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onCheckedChange(it) } // 點擊時通知外部
                )

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tags
                            .filter { it.isNotBlank() }
                            .joinToString(" #", prefix = "#"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDone) Color.Gray else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isDone) Color.Gray else MaterialTheme.colorScheme.outline
                    )
                    Text(date, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Text("編輯")
                    }
                    TextButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Text("刪除")
                    }
                }
            }
        }
    }
}

// --- 互動式 Preview：測試點擊會不會變 ---
@Preview(showBackground = true)
@Composable
fun InteractiveTaskCardPreview() {
    AppTheme {
        // 在 Preview 中模擬狀態改變
        var testStatus by remember { mutableStateOf("not yet") }

        Box(modifier = Modifier.padding(16.dp)) {
            StandardTaskCard(
                title = "點擊左側勾選框試試看",
                tags = listOf("", testStatus),
                date = "2025.12.18",
                status = testStatus,
                onCheckedChange = { isChecked ->
                    // 這裡模擬真實 App 的行為
                    testStatus = if (isChecked) "done" else "in progress"
                }
            )
        }
    }
}