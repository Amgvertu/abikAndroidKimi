package info.prorabka.abik.android.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.StickHand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Экран формы профиля: и онбординг (prefill=false), и редактирование (prefill=true).
 */
@Composable
fun ProfileFormScreen(
    title: String,
    submitText: String,
    prefill: Boolean,
    onDone: () -> Unit,
    onSkip: (() -> Unit)? = null,
    viewModel: ProfileFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(prefill) { viewModel.load(prefill) }

    if (ui.loading) {
        LoadingBox()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        RegionDropdown(ui, viewModel)
        Spacer(modifier = Modifier.height(12.dp))
        CityDropdown(ui, viewModel)
        Spacer(modifier = Modifier.height(12.dp))

        AbikTextField(value = ui.lastName, onValueChange = viewModel::onLastName, label = "Фамилия")
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(value = ui.firstName, onValueChange = viewModel::onFirstName, label = "Имя")
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(value = ui.middleName, onValueChange = viewModel::onMiddleName, label = "Отчество")
        Spacer(modifier = Modifier.height(12.dp))

        BirthDateField(ui.birthDate, viewModel::onBirthDate)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AbikTextField(
                value = ui.height,
                onValueChange = viewModel::onHeight,
                label = "Рост (см)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            AbikTextField(
                value = ui.weight,
                onValueChange = viewModel::onWeight,
                label = "Вес (кг)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Хват клюшки", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StickHand.entries.forEach { hand ->
                FilterChip(
                    selected = ui.stickHand == hand,
                    onClick = { viewModel.onStickHand(hand) },
                    label = { Text(hand.display) },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Амплуа", style = MaterialTheme.typography.labelLarge)
        Position.entries.forEach { position ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = position in ui.positions,
                    onCheckedChange = { viewModel.togglePosition(position) },
                )
                Text(position.display, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Дополнительно (необязательно)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AbikTextField(value = ui.jerseySize, onValueChange = viewModel::onJerseySize, label = "Размер джерси")
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(value = ui.coversSize, onValueChange = viewModel::onCoversSize, label = "Размер чехлов")
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(value = ui.gaitersSize, onValueChange = viewModel::onGaitersSize, label = "Размер гамаш")

        ErrorText(ui.error)
        Spacer(modifier = Modifier.height(16.dp))
        AbikButton(
            text = submitText,
            onClick = { viewModel.save(onDone) },
            loading = ui.saving,
        )
        if (onSkip != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.skip(onSkip) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Заполнить позже")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionDropdown(ui: ProfileFormViewModel.UiState, viewModel: ProfileFormViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ui.selectedRegion?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Регион") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.regions.forEach { region ->
                DropdownMenuItem(
                    text = { Text(region.name) },
                    onClick = {
                        viewModel.selectRegion(region)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDropdown(ui: ProfileFormViewModel.UiState, viewModel: ProfileFormViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (ui.selectedRegion != null) expanded = it },
    ) {
        OutlinedTextField(
            value = ui.selectedCity?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = ui.selectedRegion != null,
            label = { Text("Город") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        viewModel.selectCity(city)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(value: LocalDate?, onSelect: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Дата рождения") },
            modifier = Modifier.fillMaxWidth(),
        )
        // Прозрачная зона для тапа поверх disabled-поля
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(0.dp),
        ) {
            TextButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxSize(),
            ) {}
        }
    }
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onSelect(date)
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
