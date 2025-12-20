package com.example.allinone.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.allinone.ui.theme.AppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoTasks: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "All In One",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp)
                    )
                }
            )
        },
        bottomBar = {
            // 模擬 BottomNav，與草圖保持一致
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onGoTasks, icon = { Icon(Icons.Default.Task, "Tasks") }, label = { Text("Tasks") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.CalendarMonth, "Schedule") }, label = { Text("Schedule") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Bookmark, "Saved") }, label = { Text("Saved") })
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 1. 問候語
            Text(
                text = "Hi, <username>",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 2. 天氣卡片 (優化：漸層背景與對角線佈局)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // 後端須更新文字 數字 透過 API
                    Column(modifier = Modifier.weight(1f)) {
                        Text("今天... 局部有雨", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                        Text("25°C", color = Color.White, style = MaterialTheme.typography.displayMedium)
                    }
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Weather",
                        modifier = Modifier.size(70.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 今日事件摘要 (優化：提升行動導向價值)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // 後端須更新數字
                    Column(modifier = Modifier.weight(1f)) {
                        Text("今日待辦進度", style = MaterialTheme.typography.titleSmall)
                        Text("還有 5 個任務待處理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    // 視覺化進度
                    CircularProgressIndicator(
                        progress = { 0.6f },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 6.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PARA 模組入口 (優化：加入 Icon 與計數，更具利用性)
            Text("知識管理 (PARA)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // 後端須更新數字
            val paraList = listOf(
                ParaData("Projects", "12", Icons.Default.RocketLaunch, MaterialTheme.colorScheme.tertiaryContainer),
                ParaData("Areas", "8", Icons.Default.Favorite, MaterialTheme.colorScheme.surfaceVariant),
                ParaData("Resources", "45", Icons.Default.Source, MaterialTheme.colorScheme.surfaceVariant),
                ParaData("Archive", "120", Icons.Default.Archive, MaterialTheme.colorScheme.surfaceVariant)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(paraList) { data ->
                    ParaCard(data)
                }
            }
        }
    }
}

data class ParaData(val title: String, val count: String, val icon: ImageVector, val color: Color)

@Composable
fun ParaCard(data: ParaData) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
        colors = CardDefaults.cardColors(containerColor = data.color),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(data.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(data.count, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(data.title, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

//@Preview(showBackground = true, name = "Home")
//@Composable
//fun HomePreview() {
//    AppTheme {
//        HomeScreen()
//    }
//}
