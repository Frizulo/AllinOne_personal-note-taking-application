package com.example.allinone

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.allinone.data.remote.ApiProvider
import com.example.allinone.data.remote.dto.AuthRequest
import com.example.allinone.di.ServiceLocator
import com.example.allinone.ui.auth.*
import com.example.allinone.ui.home.HomeScreen
import com.example.allinone.ui.tasks.TasksScreen
import com.example.allinone.ui.tasks.TasksViewModel
import com.example.allinone.ui.theme.AppTheme
import com.example.allinone.worker.SyncWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val nav = rememberNavController()

    val authVm = remember { AuthViewModel(ServiceLocator.authRepository(context)) }
    val tasksVm = remember { TasksViewModel(ServiceLocator.tasksRepository(context)) }

    NavHost(navController = nav, startDestination = "landing") {

        composable("landing") {
            LandingScreen(
                onLogin = { nav.navigate("login") },
                onSignUp = { nav.navigate("signup") }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = authVm,
                onSuccess = {
                    enqueueSync(context)
                    nav.navigate(Screen.Home.route) { popUpTo("landing") { inclusive = true } }
                },
                onGoSignUp = { nav.navigate("signup") }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = authVm,
                onSuccess = { nav.navigate("login") },
                onGoLogin = { nav.navigate("login") }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(onGoTasks = { nav.navigate(Screen.Tasks.route) })
        }

        composable(Screen.Tasks.route) {
            TasksScreen(viewModel = tasksVm)
        }

        // 先留空頁面，避免路由錯誤
        composable(Screen.Schedule.route) { HomeScreen(onGoTasks = { nav.navigate(Screen.Tasks.route) }) }
        composable(Screen.Saved.route) { HomeScreen(onGoTasks = { nav.navigate(Screen.Tasks.route) }) }
    }
}

private fun enqueueSync(context: android.content.Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val req = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork("sync_once", ExistingWorkPolicy.REPLACE, req)
}
