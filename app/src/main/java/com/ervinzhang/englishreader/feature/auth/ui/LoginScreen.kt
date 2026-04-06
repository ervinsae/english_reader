package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ervinzhang.englishreader.core.datastore.SessionStore
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    sessionStore: SessionStore,
    onLogin: () -> Unit,
    onNeedInvite: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("123456") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "手机号登录")
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("测试验证码") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    sessionStore.saveSession(token = "debug-token", userId = phone.ifBlank { "debug-user" })
                    onLogin()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("老用户登录")
        }
        Button(
            onClick = onNeedInvite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("新用户继续注册")
        }
    }
}
