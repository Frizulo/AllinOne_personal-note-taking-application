package com.example.allinone.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.allinone.ui.theme.LocalAppColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String?,
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    onGoTasks: () -> Unit,
    onGoSchedule: () -> Unit,
    onGoAnalysis: () -> Unit
) {
    val appColors = LocalAppColors.current

    val weather by viewModel.todayWeather.collectAsState()
    val active by viewModel.activeTaskCount.collectAsState()
    val today by viewModel.todayTodoCount.collectAsState()

    val counties = TaiwanCounties
    val currentCountyName = weather?.city ?: counties.first().name

    var showCityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "All In One",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp)
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {

            Text(
                text = "Hi, ${username ?: "User"}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // -------------------------
            // 天氣卡（點擊 -> 浮動視窗修改縣市）
            // -------------------------
            WeatherCardClickable(
                countyName = currentCountyName,
                description = weather?.description,
                tempC = weather?.tempC,
                weatherCode = weather?.weatherCode,
                onClick = { showCityDialog = true }
            )

            if (showCityDialog) {
                CityPickerDialog(
                    currentName = currentCountyName,
                    counties = counties,
                    onDismiss = { showCityDialog = false },
                    onSelect = { c ->
                        showCityDialog = false
                        viewModel.setCity(c)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // -------------------------
            // 今日概覽：讓 HOME 不空
            // -------------------------
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStatCard(
                    title = "今日待辦",
                    value = "$today 個",
                    subtitle = "待處理",
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f),
                    container = appColors.statusNotYet.copy(alpha = 0.25f),
                    onClick = onGoTasks
                )
                MiniStatCard(
                    title = "統計代辦",
                    value = "$active 筆",
                    subtitle = "未完成任務",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    modifier = Modifier.weight(1f),
                    container = appColors.timeMorning.copy(alpha = 0.25f),
                    onClick = onGoTasks
                )

            }

            Spacer(Modifier.height(14.dp))

            // -------------------------
            // 改成三列（不是同一列）
            // -------------------------
            Text("你可以看看...", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickActionRow(
                    title = "任務",
                    subtitle = "查看 / 編輯",
                    icon = Icons.Default.Checklist,
                    onClick = onGoTasks
                )
                QuickActionRow(
                    title = "行程",
                    subtitle = "安排時間 / 查詢",
                    icon = Icons.Default.CalendarMonth,
                    onClick = onGoSchedule
                )
                QuickActionRow(
                    title = "分析",
                    subtitle = "看報表 / 時段分佈",
                    icon = Icons.Default.BarChart,
                    onClick = onGoAnalysis
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeatherCardClickable(
    countyName: String,
    description: String?,
    tempC: Int?,
    weatherCode: Int?,
    onClick: () -> Unit
) {
    val icon = remember(weatherCode) { weatherIconForCode(weatherCode) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {

                // ✅提示可點擊修改
                Text(
                    text = "縣市：$countyName（點擊更改）",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "今天天氣：",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = if (description == null || tempC == null) "載入中…" else "$description ${tempC}°C",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = "Weather",
                modifier = Modifier.size(70.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CityPickerDialog(
    currentName: String,
    counties: List<County>,
    onDismiss: () -> Unit,
    onSelect: (County) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇縣市") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)          // ✅限制 Dialog 內容最大高度
                    .verticalScroll(rememberScrollState()) // ✅可滑
            ) {                counties.forEach { c ->
                    val selected = c.name == currentName
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(c) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.weight(1f))
                            if (selected) {
                                Text(
                                    text = "目前",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("關閉") }
        }
    )
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Open-Meteo weather_code -> icon（簡化、穩定：都用 material icons filled）
 */
private fun weatherIconForCode(code: Int?): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.CloudQueue
        51, 53, 55 -> Icons.Default.Grain
        61, 63, 65 -> Icons.Default.WaterDrop
        71, 73, 75 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.CloudQueue
    }
}
