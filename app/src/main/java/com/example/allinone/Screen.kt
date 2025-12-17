package com.example.allinone
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Schedule : Screen("schedule")
    object Saved : Screen("saved")
}