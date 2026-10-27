package info.prorabka.abik.android.presentation.events

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.EventType
import java.time.LocalDate

private val WEEKDAY_NAMES = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/** Форма создания/редактирования события (в т.ч. серии). */
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
                AbikTextField(
                    value = ui.title,
                    onValueChange = viewModel::setTitle,
                    label = "Название",
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Раздел",
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

                Spacer(Modifier.height(12.dp))
                Text(
                    "Тип",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = when (ui.section) {
                        EventSection.TRAINING -> listOf(EventType.TRAINING)
                        EventSection.REGULAR -> listOf(EventType.GAME, EventType.MATCH)
                    }
                    types.forEach { type ->
                        FilterChip(
                            selected = ui.type == type,
                            onClick = { viewModel.setType(type) },
                            label = { Text(type.display) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                DateField(
                    displayValue = runCatching {
                        LocalDate.parse(ui.dateIso)
                            .format(EventFormViewModel.DISPLAY_DATE)
                    }.getOrDefault(ui.dateIso),
                    label = "Дата",
                    onSelect = viewModel::setDate,
                )

                Spacer(Modifier.height(12.dp))
                AbikTextField(
                    value = ui.time,
                    onValueChange = viewModel::setTime,
                    label = "Время (ЧЧ:ММ)",
                    keyboardType = KeyboardType.Number,
                )

                Spacer(Modifier.height(12.dp))
                AbikTextField(
                    value = ui.place,
                    onValueChange = viewModel::setPlace,
                    label = "Место",
                )

                Spacer(Modifier.height(12.dp))
                AbikTextField(
                    value = ui.note,
                    onValueChange = viewModel::setNote,
                    label = "Комментарий (необязательно)",
                    singleLine = false,
                )

                // Серия — только при создании
                if (!viewModel.isEdit) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Повторять каждую неделю",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Switch(
                            checked = ui.repeatWeekly,
                            onCheckedChange = viewModel::setRepeatWeekly,
                        )
                    }
                    if (ui.repeatWeekly) {
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
                        Spacer(Modifier.height(8.dp))
                        DateField(
                            displayValue = ui.untilDateIso?.let {
                                runCatching {
                                    LocalDate.parse(it).format(EventFormViewModel.DISPLAY_DATE)
                                }.getOrDefault(it)
                            } ?: "",
                            label = "Повторять до (включительно)",
                            onSelect = viewModel::setUntilDate,
                        )
                    }
                }

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
