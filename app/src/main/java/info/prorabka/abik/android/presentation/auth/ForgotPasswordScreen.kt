package info.prorabka.abik.android.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText

@Composable
fun ForgotPasswordScreen(
    onPasswordChanged: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
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
            text = "Восстановление пароля",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (ui.step) {
            ForgotPasswordViewModel.Step.PHONE -> {
                Text(
                    text = "Отправим SMS-код на ваш текущий номер",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AbikTextField(
                    value = ui.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Номер телефона",
                    keyboardType = KeyboardType.Phone,
                )
                ErrorText(ui.error)
                Spacer(modifier = Modifier.height(16.dp))
                AbikButton(
                    text = "Отправить код",
                    onClick = viewModel::sendCode,
                    loading = ui.loading,
                )
            }

            ForgotPasswordViewModel.Step.CODE -> {
                Text(
                    text = "Код отправлен на ${ui.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AbikTextField(
                    value = ui.code,
                    onValueChange = viewModel::onCodeChange,
                    label = "Код из SMS (4 цифры)",
                    keyboardType = KeyboardType.NumberPassword,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AbikTextField(
                    value = ui.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = "Новый пароль (минимум 8 символов)",
                    isPassword = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AbikTextField(
                    value = ui.newPasswordRepeat,
                    onValueChange = viewModel::onNewPasswordRepeatChange,
                    label = "Повторите новый пароль",
                    isPassword = true,
                )
                ErrorText(ui.error)
                Spacer(modifier = Modifier.height(16.dp))
                AbikButton(
                    text = "Сменить пароль",
                    onClick = { viewModel.confirm(onPasswordChanged) },
                    loading = ui.loading,
                )
                TextButton(onClick = viewModel::sendCode) { Text("Отправить код ещё раз") }
            }
        }

        TextButton(onClick = onBackToLogin) { Text("Назад ко входу") }
    }
}
