package com.example.allinone.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String?,
    viewModel: HomeViewModel,
    onLogout: () -> Unit
) {
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
                    // ✅ 登出
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
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

            // 1) 問候語
            Text(
                text = "Hi, ${username ?: "User"}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 2) 天氣卡片（icon 依 weather_code）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weather by viewModel.todayWeather.collectAsState()

                    Column(modifier = Modifier.weight(1f)) {
                        if (weather == null) {
                            Text(
                                "今天天氣",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text("載入中…", color = Color.White)
                        } else {
                            Text(
                                "${weather!!.city} 今天天氣：",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "${weather!!.description} ${weather!!.tempC}°C",
                                color = Color.White,
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                    }

                    val icon = remember(weather?.weatherCode) {
                        weatherIconForCode(weather?.weatherCode)
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = "Weather",
                        modifier = Modifier.size(70.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3) 今日待辦摘要
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val today by viewModel.todayTodoCount.collectAsState()
                    val todayTotal by viewModel.todayTodoTotalCount.collectAsState()
                    val progress = if (todayTotal <= 0) 0f else (todayTotal - today).toFloat() / todayTotal.toFloat()

                    Column(modifier = Modifier.weight(1f)) {
                        Text("今日待辦進度", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "還有 $today 個任務待處理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 6.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ✅ 4) 移除「知識管理(PARA)」但保留 activeTaskCount 顯示
            val active by viewModel.activeTaskCount.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("未完成任務", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "共 $active 筆",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "（不含 done）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Open-Meteo weather_code 對應 icon（簡化版，避免使用可能缺少的 icon）。
 */
private fun weatherIconForCode(code: Int?): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.CloudQueue      // fog
        51, 53, 55 -> Icons.Default.Grain       // drizzle
        61, 63, 65 -> Icons.Default.WaterDrop   // rain
        71, 73, 75 -> Icons.Default.AcUnit      // snow
        80, 81, 82 -> Icons.Default.WaterDrop   // showers
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.CloudQueue
    }
}
