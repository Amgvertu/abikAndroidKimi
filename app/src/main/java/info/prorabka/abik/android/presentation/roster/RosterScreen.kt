package info.prorabka.abik.android.presentation.roster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.core.util.filterMoneyInput
import info.prorabka.abik.android.core.util.parseMoneyToKopecks
import info.prorabka.abik.android.domain.model.EntryFeeData
import info.prorabka.abik.android.domain.model.RosterItem
import info.prorabka.abik.android.domain.model.TeamMember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Состав регулярки (ТЗ 10.1): сезоны, активные/исключённые, добавление со взносом. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    onBack: () -> Unit,
    onCreateTeam: () -> Unit = onBack,
    viewModel: RosterViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var showRemoved by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var replaceDialog by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<RosterItem?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Состав регулярки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (ui.loading && ui.seasons.isEmpty()) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (ui.noTeam) {
            Box(Modifier.padding(padding)) { NoTeamPlaceholder(onCreateTeam) }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // --- Выбор сезона ---
            SeasonDropdown(
                seasons = ui.seasons.map { it.name to it.isCurrent },
                selected = ui.selectedSeason,
                onSelect = viewModel::selectSeason,
            )
            if (ui.selectedSeason != null && !ui.isCurrentSeason) {
                Text(
                    "Архивный сезон — только просмотр",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            ui.info?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp))
            }

            // --- Фильтр активные/исключённые ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                FilterChip(
                    selected = !showRemoved,
                    onClick = { showRemoved = false },
                    label = { Text("Активные (${ui.activeRoster.size})") },
                )
                FilterChip(
                    selected = showRemoved,
                    onClick = { showRemoved = true },
                    label = { Text("Исключённые (${ui.removedRoster.size})") },
                )
            }

            // --- Действия управляющего ---
            if (ui.canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { addDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp))
                        Text("Добавить")
                    }
                    TextButton(onClick = { replaceDialog = true }) {
                        Text("Изменить списком")
                    }
                }
            }

            // --- Список ---
            val items = if (showRemoved) ui.removedRoster else ui.activeRoster
            if (items.isEmpty()) {
                Text(
                    if (showRemoved) "Нет исключённых игроков" else "Состав пока пуст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            LazyColumn {
                items(items, key = { it.userId }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.fullName, style = MaterialTheme.typography.bodyLarge)
                            if (item.positions.isNotEmpty()) {
                                Text(
                                    item.positions.joinToString(", ") { it.display },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (ui.canManage && !showRemoved) {
                            IconButton(onClick = { removeTarget = item }) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Исключить из состава",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (addDialog) {
        AddPlayerDialog(
            candidates = ui.candidates,
            groups = ui.groups.map { it.id to it.name },
            subgroups = ui.subgroups.map { it.id to it.name },
            saving = ui.saving,
            onGroupSelected = viewModel::loadSubgroups,
            onDismiss = { addDialog = false },
            onAdd = { userId, fee ->
                viewModel.addMember(userId, fee)
                addDialog = false
            },
        )
    }

    if (replaceDialog) {
        ReplaceRosterDialog(
            members = ui.members,
            preselected = ui.activeRoster.map { it.userId }.toSet(),
            onDismiss = { replaceDialog = false },
            onSave = { ids ->
                viewModel.replace(ids)
                replaceDialog = false
            },
        )
    }

    removeTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Исключить из состава?") },
            text = {
                Text("${item.fullName}\nНачисления и платежи сохранятся.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(item.userId)
                    removeTarget = null
                }) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonDropdown(
    seasons: List<Pair<String, Boolean>>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = seasons.firstOrNull { it.first == selected }
                ?.let { it.first + if (it.second) " (текущий)" else "" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Сезон") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            seasons.forEach { (name, isCurrent) ->
                DropdownMenuItem(
                    text = { Text(name + if (isCurrent) " (текущий)" else "") },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Добавление игрока: выбор из кандидатов + опциональный стартовый взнос. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlayerDialog(
    candidates: List<TeamMember>,
    groups: List<Pair<Long, String>>,
    subgroups: List<Pair<Long, String>>,
    saving: Boolean,
    onGroupSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (userId: Long, fee: EntryFeeData?) -> Unit,
) {
    var playerId by remember { mutableStateOf<Long?>(null) }
    var withFee by remember { mutableStateOf(false) }
    var groupId by remember { mutableStateOf<Long?>(null) }
    var subgroupId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("9000") }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var confirmFee by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в состав") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SimpleDropdown(
                    label = "Игрок",
                    options = candidates.map { it.userId to it.displayName },
                    selected = playerId,
                    onSelect = { playerId = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = withFee,
                        onCheckedChange = { withFee = it },
                    )
                    Text("Начислить стартовый взнос")
                }
                if (withFee) {
                    SimpleDropdown(
                        label = "Группа начисления",
                        options = groups,
                        selected = groupId,
                        onSelect = {
                            groupId = it
                            subgroupId = null
                            onGroupSelected(it)
                        },
                    )
                    if (subgroups.isNotEmpty()) {
                        SimpleDropdown(
                            label = "Подгруппа (необязательно)",
                            options = subgroups,
                            selected = subgroupId,
                            onSelect = { subgroupId = it },
                        )
                    }
                    AbikTextField(
                        value = amount,
                        onValueChange = { amount = it.filterMoneyInput() },
                        label = "Сумма, ₽",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DueDateField(dueDate) { dueDate = it }
                }
                localError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val uid = playerId
                    if (uid == null) {
                        localError = "Выберите игрока"
                        return@TextButton
                    }
                    if (!withFee) {
                        onAdd(uid, null)
                        return@TextButton
                    }
                    val gid = groupId
                    val kopecks = amount.parseMoneyToKopecks()
                    when {
                        gid == null -> localError = "Выберите группу начисления"
                        kopecks == null || kopecks <= 0 -> localError = "Укажите сумму"
                        else -> confirmFee = true
                    }
                },
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )

    // Явное подтверждение создания начисления (ТЗ 10.1)
    if (confirmFee) {
        val playerName = candidates.firstOrNull { it.userId == playerId }?.displayName ?: ""
        AlertDialog(
            onDismissRequest = { confirmFee = false },
            title = { Text("Создать начисление?") },
            text = {
                Text(
                    "$playerName\nСумма: $amount ₽\nСрок оплаты: ${dueDate.format(DATE_FMT)}",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFee = false
                    onAdd(
                        playerId!!,
                        EntryFeeData(
                            groupId = groupId!!,
                            subgroupId = subgroupId,
                            amountKopecks = amount.parseMoneyToKopecks()!!,
                            dueDateIso = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        ),
                    )
                }) { Text("Начислить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFee = false }) { Text("Отмена") }
            },
        )
    }
}

/** Массовое обновление состава: чек-лист участников команды. */
@Composable
private fun ReplaceRosterDialog(
    members: List<TeamMember>,
    preselected: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit,
) {
    var checked by remember { mutableStateOf(preselected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Состав сезона") },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(members, key = { it.userId }) { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = member.userId in checked,
                            onCheckedChange = { on ->
                                checked = if (on) checked + member.userId
                                else checked - member.userId
                            },
                        )
                        Text(member.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(checked) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<Pair<Long, String>>,
    selected: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateField(value: LocalDate, onSelect: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value.format(DATE_FMT),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Срок оплаты") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxSize()) {}
    }
    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onSelect(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate(),
                        )
                    }
                    showPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
