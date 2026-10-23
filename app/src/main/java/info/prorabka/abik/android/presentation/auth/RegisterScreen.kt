package info.prorabka.abik.android.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
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
            text = if (ui.step == RegisterViewModel.Step.FORM) "Регистрация" else "Код из SMS",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (ui.step) {
            RegisterViewModel.Step.FORM -> {
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
                    label = "Пароль (минимум 8 символов)",
                    isPassword = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AbikTextField(
                    value = ui.passwordRepeat,
                    onValueChange = viewModel::onPasswordRepeatChange,
                    label = "Повторите пароль",
                    isPassword = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(checked = ui.acceptedPd, onCheckedChange = viewModel::onAcceptedPdChange)
                    Text(
                        text = "Соглашаюсь на обработку персональных данных",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ErrorText(ui.error)
                Spacer(modifier = Modifier.height(16.dp))
                AbikButton(
                    text = "Получить код",
                    onClick = viewModel::start,
                    loading = ui.loading,
                )
                TextButton(onClick = onBackToLogin) { Text("У меня уже есть аккаунт") }
            }

            RegisterViewModel.Step.CODE -> {
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
                ErrorText(ui.error)
                Spacer(modifier = Modifier.height(16.dp))
                AbikButton(
                    text = "Подтвердить",
                    onClick = { viewModel.confirm(onRegistered) },
                    loading = ui.loading,
                )
                TextButton(onClick = viewModel::resend) { Text("Отправить код ещё раз") }
            }
        }
    }
}
