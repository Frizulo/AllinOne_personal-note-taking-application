package com.example.allinone.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.allinone.Screen
import com.example.allinone.ui.theme.AppTheme

data class NavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("Home", Screen.Home.route, Icons.Default.Home),
    NavItem("Tasks", Screen.Tasks.route, Icons.Default.Task),
    NavItem("Schedule", Screen.Schedule.route, Icons.Default.CalendarMonth),
    NavItem("Saved", Screen.Saved.route, Icons.Default.Bookmark),
)

@Composable
fun AllInOneBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    AppTheme { AllInOneBottomBar(currentRoute = Screen.Home.route, onNavigate = {}) }
}
