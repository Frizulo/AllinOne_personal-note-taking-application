package com.example.allinone.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    description: String = "這是一段詳細敘述，點擊卡片即可看到 Ease-InOut 的展開效果。",
    onEditClick: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit = {}, // 勾選狀態回呼
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // 完成時變淡
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- 未展開時的主視圖 ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. 勾選按鈕 (商業核心：快速完成)
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        isChecked = it
                        onCheckedChange(it)
                    }
                )

                Spacer(Modifier.width(8.dp))

                // 2. 標題與標籤
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            // 如果勾選了，文字加上刪除線
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tags.joinToString(" #", prefix = "#"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                // 3. 狀態與箭頭
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(date, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }

            // --- 點擊展開後的詳細區 ---
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 編輯按鈕 (商業價值：深度操作入口)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onEditClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("編輯詳細資訊")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskCardPreview() {
    AppTheme {
        Column(Modifier.padding(16.dp)) {
            StandardTaskCard(
                title = "進行 CWA 天氣 API 串接",
                tags = listOf("後端", "緊急"),
                date = "12/17",
                description = "需要在 ViewModel 實作 Repository 呼叫，並將資料儲存至 Room 本地資料庫，以符合老師的 Version 2 遷移要求。"
            )
        }
    }
}