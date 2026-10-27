package info.prorabka.abik.android.presentation.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Единая форма платежа: создание и редактирование. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormScreen(
    paymentId: Long,
    preselectUserId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val isEdit = ui.editPaymentId != null

    LaunchedEffect(paymentId, preselectUserId) { viewModel.load(paymentId, preselectUserId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Редактировать платёж" else "Внести платёж") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (ui.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            AbikTextField(
                value = ui.amountRubles,
                onValueChange = viewModel::onAmount,
                label = "Сумма (₽)",
                keyboardType = KeyboardType.Decimal,
            )
            Spacer(modifier = Modifier.height(12.dp))

            MemberDropdown(ui, viewModel)
            Spacer(modifier = Modifier.height(12.dp))

            DateField(ui.paidAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) {
                viewModel.onPaidAt(it)
            }
            Spacer(modifier = Modifier.height(12.dp))

            AbikTextField(
                value = ui.note,
                onValueChange = viewModel::onNote,
                label = "Примечание",
                singleLine = false,
            )

            ErrorText(ui.error)
            Spacer(modifier = Modifier.height(16.dp))
            AbikButton(
                text = if (isEdit) "Сохранить" else "Внести",
                onClick = { viewModel.save(onDone) },
                loading = ui.saving,
            )
            if (isEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.delete(onDone) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Удалить платёж", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberDropdown(
    ui: PaymentFormViewModel.UiState,
    viewModel: PaymentFormViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    // Плательщик исключён из команды — показываем его имя, менять нельзя
    val enabled = !ui.payerExcluded
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = ui.payerDisplayName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Кто внёс") },
            trailingIcon = {
                if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            ui.members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.displayName) },
                    onClick = {
                        viewModel.selectMember(member)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(displayValue: String, onSelect: (java.time.LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Дата внесения") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxSize()) {}
    }
    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onSelect(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate(),
                            )
                        }
                        showPicker = false
                    },
                ) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
