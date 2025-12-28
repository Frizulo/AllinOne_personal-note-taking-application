package com.example.allinone.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.allinone.Screen
import com.example.allinone.ui.theme.AppTheme

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AllInOneBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, "home"),
        NavItem("Tasks", Icons.Default.Task, "tasks"),
        NavItem("Schedule", Icons.Default.CalendarMonth, "schedule"),
        NavItem("Saved", Icons.Default.Bookmark, "saved")
    )

    // In real devices, NavigationBar applies navigationBars insets by default, which can look like
    // an extra blank strip above the bar even if Preview looks fine. We disable the default insets
    // and control the bar height ourselves.
    NavigationBar(
        modifier = androidx.compose.ui.Modifier.height(80.dp).padding(top = 0.dp, bottom = 24.dp),
        windowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        // 選中時使用 Primary 色，未選中使用灰色
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                },
                // 移除選中時的圓形背景框，讓設計更簡潔現代
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    AppTheme { AllInOneBottomBar(currentRoute = Screen.Home.route, onNavigate = {}) }
}
