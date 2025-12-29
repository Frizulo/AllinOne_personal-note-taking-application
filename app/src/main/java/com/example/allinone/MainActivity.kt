package com.example.allinone

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.allinone.di.ServiceLocator
import com.example.allinone.ui.auth.AuthViewModel
import com.example.allinone.ui.auth.LandingScreen
import com.example.allinone.ui.auth.LoginScreen
import com.example.allinone.ui.auth.SignUpScreen
import com.example.allinone.ui.components.AllInOneBottomBar
import com.example.allinone.ui.home.HomeScreen
import com.example.allinone.ui.home.HomeViewModel
import com.example.allinone.ui.schedule.ScheduleScreen
import com.example.allinone.ui.schedule.ScheduleViewModel
import com.example.allinone.ui.tasks.TasksScreen
import com.example.allinone.ui.tasks.TasksViewModel
import com.example.allinone.ui.theme.AppTheme
import com.example.allinone.worker.SyncWorker

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val nav = rememberNavController()

    val authVm: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ServiceLocator.authRepository(context)) as T
            }
        }
    )

    //val authVm = remember { AuthViewModel(ServiceLocator.authRepository(context)) }
    val tasksVm = remember { TasksViewModel(ServiceLocator.tasksRepository(context)) }
    val homeVm = remember { HomeViewModel(ServiceLocator.tasksRepository(context),ServiceLocator.weatherRepository(context)) }
    val scheduleVm = remember {
        ScheduleViewModel(
            tasksRepo = ServiceLocator.tasksRepository(context),
            scheduleRepo = ServiceLocator.scheduleRepository(context),
            tokenStore = ServiceLocator.tokenStore(context)
        )
    }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    // 這些頁面才需要顯示 BottomBar
    val showBottomBar = route in setOf(
        Screen.Home.route,
        Screen.Tasks.route,
        Screen.Schedule.route,
        Screen.Saved.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.padding(top = 0.dp),
        bottomBar = {
            if (showBottomBar) {
                AllInOneBottomBar(
                    currentRoute = route,
                    onNavigate = { destRoute ->
                        nav.navigate(destRoute) {
                            // 避免一直疊同一個目的地
                            launchSingleTop = true
                            restoreState = true
                            // 回到 graph 起點（通常是 landing），但我們只想管理底部頁面的堆疊
                            popUpTo(nav.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "landing",
            modifier = Modifier.padding(innerPadding)
        ) {

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
                        nav.navigate(Screen.Home.route) {
                            popUpTo("landing") { inclusive = true }
                            launchSingleTop = true
                        }
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
                val username by authVm.username.collectAsState()
                HomeScreen(username = username, homeVm)
            }

            composable(Screen.Tasks.route) {
                TasksScreen(viewModel = tasksVm)
            }

            // 先留空頁面，避免路由錯誤
            composable(Screen.Schedule.route) {
                ScheduleScreen(scheduleVm)
            }
            composable(Screen.Saved.route) {
                val username by authVm.username.collectAsState()
                HomeScreen(username = username, homeVm)
            }
        }
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
