package com.example.allinone.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.allinone.ui.theme.AppTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onGoSignUp: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val appContext = LocalContext.current.applicationContext



    AuthFrame(
        title = "Login",
        buttonText = if (uiState.isLoading) "Logging in..." else "Login",
        onButtonClick = { viewModel.login(username, password, appContext, onSuccess) }
    ) {
        AuthTextField(label = "Username", value = username, onValueChange = { username = it })
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(label = "Password", value = password, onValueChange = { password = it }, isPassword = true)

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onGoSignUp) { Text("沒有帳號？註冊") }
    }
}

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    AuthFrame(
        title = "Sign Up",
        buttonText = if (uiState.isLoading) "Signing up..." else "Sign Up",
        onButtonClick = { viewModel.register(username, password, onSuccess) }
    ) {
        AuthTextField(label = "Username", value = username, onValueChange = { username = it })
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(label = "Password", value = password, onValueChange = { password = it }, isPassword = true)

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onGoLogin) { Text("已有帳號？登入") }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginPreview() {
    AppTheme {
        // preview only
        Surface { Text("Preview") }
    }
}
