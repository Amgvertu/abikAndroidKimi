package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.DateField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.EventChargeMode
import info.prorabka.abik.android.domain.model.EventSection
import java.time.LocalDate

private val WEEKDAY_NAMES = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/** Форма создания/редактирования события (ТЗ «Исправление функционала»). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    onBack: () -> Unit,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEdit) "Редактировать событие" else "Новое событие") },
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
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // --- Тип события ---
                Text(
                    "Тип события",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventSection.entries.forEach { section ->
                        FilterChip(
                            selected = ui.section == section,
                            onClick = { viewModel.setSection(section) },
                            label = { Text(section.display) },
                        )
                    }
                }

                // --- Тренировка: повторяющаяся / разовая (только при создании) ---
                if (ui.section == EventSection.TRAINING && !viewModel.isEdit) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = ui.repeatWeekly,
                            onClick = { viewModel.setRepeatWeekly(true) },
                            label = { Text("Повторяющаяся") },
                        )
                        FilterChip(
                            selected = !ui.repeatWeekly,
                            onClick = { viewModel.setRepeatWeekly(false) },
                            label = { Text("Разовая") },
                        )
                    }
                }

                // --- Название: у тренировки генерируется автоматически (ТЗ) ---
                if (ui.section != EventSection.TRAINING) {
                    Spacer(Modifier.height(12.dp))
                    AbikTextField(
                        value = ui.title,
                        onValueChange = viewModel::setTitle,
                        label = "Название",
                    )
                }

                Spacer(Modifier.height(12.dp))
                DateField(
                    displayValue = runCatching {
                        LocalDate.parse(ui.dateIso)
                            .format(EventFormViewModel.DISPLAY_DATE)
                    }.getOrDefault(ui.dateIso),
                    label = if (ui.repeatWeekly && ui.section == EventSection.TRAINING) {
                        "Дата начала"
                    } else {
                        "Дата"
                    },
                    onSelect = viewModel::setDate,
                )

                // --- Повторяющаяся тренировка: дата окончания + дни недели ---
                if (ui.section == EventSection.TRAINING && ui.repeatWeekly && !viewModel.isEdit) {
                    Spacer(Modifier.height(12.dp))
                    DateField(
                        displayValue = ui.untilDateIso?.let {
                            runCatching {
                                LocalDate.parse(it).format(EventFormViewModel.DISPLAY_DATE)
                            }.getOrDefault(it)
                        } ?: "",
                        label = "Дата окончания",
                        onSelect = viewModel::setUntilDate,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Дни недели",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WEEKDAY_NAMES.forEachIndexed { index, name ->
                            val day = index + 1
                            FilterChip(
                                selected = ui.weekdays.contains(day),
                                onClick = { viewModel.toggleWeekday(day) },
                                label = { Text(name) },
                            )
                        }
                    }
                }

                // --- Время: выбор часами (ТЗ) ---
                Spacer(Modifier.height(12.dp))
                TimeField(value = ui.time, onSelect = viewModel::setTime)

                Spacer(Modifier.height(12.dp))
                AbikTextField(
                    value = ui.place,
                    onValueChange = viewModel::setPlace,
                    label = if (ui.placeRequired) "Место" else "Место (необязательно)",
                )

                // --- Разовая тренировка: чекбокс «Игровая» ---
                if (ui.section == EventSection.TRAINING && !ui.repeatWeekly) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.isGameTraining,
                            onCheckedChange = viewModel::setGameTraining,
                        )
                        Text("Игровая", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // --- Регулярка / Двухсторонка: игроки и начисление ---
                if (ui.section != EventSection.TRAINING) {
                    Spacer(Modifier.height(12.dp))
                    AbikTextField(
                        value = ui.requiredPlayers,
                        onValueChange = viewModel::setRequiredPlayers,
                        label = "Требуемое кол-во игроков (необязательно)",
                        keyboardType = KeyboardType.Number,
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Начисление за игру",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = ui.chargeMode == EventChargeMode.NONE,
                            onClick = { viewModel.setChargeMode(EventChargeMode.NONE) },
                            label = { Text("Без начисления") },
                        )
                        FilterChip(
                            selected = ui.chargeMode == EventChargeMode.MANUAL,
                            onClick = { viewModel.setChargeMode(EventChargeMode.MANUAL) },
                            label = { Text("Вручную") },
                        )
                        if (ui.section == EventSection.REGULAR) {
                            FilterChip(
                                selected = ui.chargeMode == EventChargeMode.TEMPLATE,
                                onClick = { viewModel.setChargeMode(EventChargeMode.TEMPLATE) },
                                label = { Text("По шаблону") },
                            )
                        }
                    }

                    if (ui.chargeMode == EventChargeMode.MANUAL) {
                        Spacer(Modifier.height(8.dp))
                        AbikTextField(
                            value = ui.chargeAmountRub,
                            onValueChange = viewModel::setChargeAmount,
                            label = "Сумма оплаты, ₽ (необязательно)",
                            keyboardType = KeyboardType.Number,
                        )
                        if (ui.chargeAmountRub.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            ui.availableDistributions.forEach { d ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setChargeDistribution(d) },
                                ) {
                                    RadioButton(
                                        selected = ui.chargeDistribution == d,
                                        onClick = { viewModel.setChargeDistribution(d) },
                                    )
                                    Text(d.display, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    if (ui.chargeMode == EventChargeMode.TEMPLATE) {
                        Spacer(Modifier.height(8.dp))
                        TemplateDropdown(
                            ui = ui,
                            onSelect = viewModel::setChargeTemplate,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                AbikTextField(
                    value = ui.note,
                    onValueChange = viewModel::setNote,
                    label = "Комментарий (необязательно)",
                    singleLine = false,
                )

                ErrorText(ui.error)

                Spacer(Modifier.height(20.dp))
                AbikButton(
                    text = if (viewModel.isEdit) "Сохранить" else "Создать",
                    onClick = viewModel::save,
                    loading = ui.saving,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (ui.askScope) {
        AlertDialog(
            onDismissRequest = viewModel::dismissScope,
            title = { Text("Событие входит в серию") },
            text = { Text("Применить изменения только к этому событию или ко всей серии?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmScope("single") }) {
                    Text("Только это")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.confirmScope("series") }) {
                        Text(
                            "Всю серию",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = viewModel::dismissScope) { Text("Отмена") }
                }
            },
        )
    }
}

/** Поле времени: выбор часами через TimePicker (ТЗ). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(value: String, onSelect: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Время") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { showDialog = true }) { Text("Выбрать") }
        },
    )

    if (showDialog) {
        val parts = value.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 21,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onSelect("%02d:%02d".format(state.hour, state.minute))
                    showDialog = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDropdown(
    ui: EventFormViewModel.UiState,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = ui.templates.firstOrNull { it.id == ui.chargeTemplateId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Шаблон начисления") },
            placeholder = {
                Text(if (ui.templates.isEmpty()) "Нет активных шаблонов" else "Выберите шаблон")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.templates.forEach { template ->
                DropdownMenuItem(
                    text = { Text("${template.name} — ${template.amountKopecks / 100} ₽") },
                    onClick = {
                        onSelect(template.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
