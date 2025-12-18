package com.example.allinone.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.allinone.ui.theme.AppTheme

@Composable
fun SignUpScreen(onSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 修正：將 Lambda 移出括號
    AuthFrame(
        title = "Sign Up",
        buttonText = "Sign Up",
        onButtonClick = onSuccess
    ) {
        AuthTextField(label = "Username", value = username, onValueChange = { username = it })
        Spacer(modifier = Modifier.height(20.dp))
        AuthTextField(label = "Password", value = password, onValueChange = { password = it })
    }
}

/**
 * 登入頁面
 */
@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 修正：將 Lambda 移出括號
    AuthFrame(
        title = "Login",
        buttonText = "Login",
        onButtonClick = onSuccess
    ) {
        AuthTextField(label = "Username", value = username, onValueChange = { username = it })
        Spacer(modifier = Modifier.height(20.dp))
        AuthTextField(label = "Password", value = password, onValueChange = { password = it })
    }
}

// --- Previews ---

@Preview(showSystemUi = true, name = "註冊頁面")
@Composable
fun SignUpPreview() {
    AppTheme {
        SignUpScreen {}
    }
}

@Preview(showSystemUi = true, name = "登入頁面")
@Composable
fun LoginPreview() {
    AppTheme {
        LoginScreen {}
    }
}