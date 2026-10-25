package info.prorabka.abik.android.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "abik",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Учёт взносов команды",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(40.dp))

        AbikTextField(
            value = ui.phone,
            onValueChange = viewModel::onPhoneChange,
            label = "Номер телефона",
            keyboardType = KeyboardType.Phone,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(
            value = ui.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Пароль",
            isPassword = true,
        )
        ErrorText(ui.error)
        Spacer(modifier = Modifier.height(24.dp))

        AbikButton(
            text = "Войти",
            onClick = { viewModel.login(onLoginSuccess) },
            loading = ui.loading,
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onForgotPassword) { Text("Забыли пароль?") }
        TextButton(onClick = onRegister) { Text("Регистрация") }

        if (BuildConfig.DEBUG) {
            val currentUrl by viewModel.currentBaseUrl.collectAsState()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Сервер: $currentUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = viewModel::useRemoteServer) {
                    Text("varamy.online", fontSize = 12.sp)
                }
                TextButton(onClick = viewModel::useLocalServer) {
                    Text("192.168.0.119", fontSize = 12.sp)
                }
            }
        }
    }
}
