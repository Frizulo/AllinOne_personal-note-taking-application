package com.example.allinone.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allinone.ui.theme.AppTheme

@Composable
fun LandingScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("All in One", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Local-first 任務管理 + 雲端同步")
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("登入", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("註冊", fontSize = 18.sp)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LandingPreview() {
    AppTheme { LandingScreen(onLogin = {}, onSignUp = {}) }
}
