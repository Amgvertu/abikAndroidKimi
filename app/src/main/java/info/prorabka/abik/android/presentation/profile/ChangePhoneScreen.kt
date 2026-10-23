package info.prorabka.abik.android.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePhoneScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChangePhoneViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Смена номера") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            when (ui.step) {
                ChangePhoneViewModel.Step.OLD_CODE -> {
                    Text(
                        "Подтвердите текущий номер — отправим SMS-код на него",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!ui.started) {
                        AbikButton(
                            text = "Отправить код на текущий номер",
                            onClick = viewModel::start,
                            loading = ui.loading,
                        )
                    } else {
                        AbikTextField(
                            value = ui.oldCode,
                            onValueChange = viewModel::onOldCode,
                            label = "Код из SMS",
                            keyboardType = KeyboardType.NumberPassword,
                        )
                        ErrorText(ui.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        AbikButton(
                            text = "Подтвердить",
                            onClick = viewModel::verifyOld,
                            loading = ui.loading,
                        )
                        TextButton(onClick = viewModel::start) { Text("Отправить ещё раз") }
                    }
                }

                ChangePhoneViewModel.Step.NEW_PHONE -> {
                    Text(
                        "Введите новый номер — отправим на него SMS-код",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AbikTextField(
                        value = ui.newPhone,
                        onValueChange = viewModel::onNewPhone,
                        label = "Новый номер телефона",
                        keyboardType = KeyboardType.Phone,
                    )
                    ErrorText(ui.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    AbikButton(
                        text = "Отправить код",
                        onClick = viewModel::sendNew,
                        loading = ui.loading,
                    )
                }

                ChangePhoneViewModel.Step.NEW_CODE -> {
                    Text(
                        "Код отправлен на ${ui.newPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AbikTextField(
                        value = ui.newCode,
                        onValueChange = viewModel::onNewCode,
                        label = "Код из SMS",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    ErrorText(ui.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    AbikButton(
                        text = "Сменить номер",
                        onClick = { viewModel.confirm(onDone) },
                        loading = ui.loading,
                    )
                    Text(
                        "После смены номера нужно будет войти заново",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
