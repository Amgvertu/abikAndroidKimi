package info.prorabka.abik.android.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.core.util.phoneToDisplay
import info.prorabka.abik.android.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AdminDropdown(label: String, items: List<T>, selected: T?, onSelect: (T?) -> Unit, itemLabel: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let(itemLabel) ?: "", onValueChange = {}, readOnly = true,
            label = { Text(label) }, placeholder = { Text("Все") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Все") }, onClick = { onSelect(null); expanded = false })
            items.forEach { item -> DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { onSelect(item); expanded = false }) }
        }
    }
}

@Composable
fun AdminStatsContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var from by remember { mutableStateOf(LocalDate.now().minusDays(29).toString()) }
    var to by remember { mutableStateOf(LocalDate.now().toString()) }
    var filtersExpanded by remember { mutableStateOf(false) }
    val teams = ui.targetTeams.map { it.id to it.name }
    fun applyPeriod(start: LocalDate, end: LocalDate = LocalDate.now()) {
        from = start.toString(); to = end.toString()
        viewModel.applyFilters(from, to, ui.filterRegionId, ui.filterCityId, ui.filterTeamId)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Период и фильтры", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { filtersExpanded = !filtersExpanded }) { Text(if (filtersExpanded) "Скрыть" else "Развернуть") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { val today = LocalDate.now(); applyPeriod(today) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Сегодня", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    OutlinedButton(onClick = { val today = LocalDate.now(); applyPeriod(today.withDayOfMonth(1)) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Текущий месяц", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    OutlinedButton(onClick = { val today = LocalDate.now(); applyPeriod(today.withDayOfYear(1)) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Текущий год", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                }
                if (filtersExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminDateField("С", from, { from = it }, Modifier.weight(1f))
                        AdminDateField("По", to, { to = it }, Modifier.weight(1f))
                    }
                    AdminDropdown("Регион", ui.regions, ui.regions.firstOrNull { it.id == ui.filterRegionId }, { viewModel.selectFilterRegion(it?.id) }, { it.name })
                    AdminDropdown("Город", ui.cities, ui.cities.firstOrNull { it.id == ui.filterCityId }, { viewModel.applyFilters(from, to, ui.filterRegionId, it?.id, ui.filterTeamId) }, { it.name })
                    AdminDropdown("Команда", teams, teams.firstOrNull { it.first == ui.filterTeamId }, { viewModel.applyFilters(from, to, ui.filterRegionId, ui.filterCityId, it?.first) }, { it.second })
                    AbikButton("Применить", onClick = { viewModel.applyFilters(from, to, ui.filterRegionId, ui.filterCityId, ui.filterTeamId) })
                }
            } }
        }
        ui.overview?.let { overview ->
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Пользователи", overview.totalUsers.toString(), Modifier.weight(1f))
                MetricCard("Команды", overview.totalTeams.toString(), Modifier.weight(1f))
            } }
            item { MetricCard("Начисления", "${overview.chargeCount} • ${overview.chargeAmountKopecks.kopecksToRubles()}", Modifier.fillMaxWidth()) }
            item { MetricCard("Отменённые начисления", "${overview.cancelledChargeCount} • ${overview.cancelledChargeAmountKopecks.kopecksToRubles()}", Modifier.fillMaxWidth()) }
            item { MetricCard("Платежи", "${overview.paymentCount} • ${overview.paymentAmountKopecks.kopecksToRubles()}", Modifier.fillMaxWidth()) }
            item { Text("Динамика регистраций", style = MaterialTheme.typography.titleMedium) }
            items(overview.daily.filter { it.users > 0 || it.teams > 0 }) { day ->
                Row(Modifier.fillMaxWidth()) {
                    Text(runCatching { LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("dd.MM")) }.getOrDefault(day.date), Modifier.weight(1f))
                    Text("Пользователи: ${day.users}", Modifier.weight(1.5f))
                    Text("Команды: ${day.teams}", Modifier.weight(1.3f))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AdminDateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun showPicker() {
        val initial = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        android.app.DatePickerDialog(context, { _, year, month, day ->
            onChange(LocalDate.of(year, month + 1, day).toString())
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }
    Box(modifier) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        Box(Modifier.matchParentSize().clickable(onClick = ::showPicker))
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    } }
}

@Composable
fun AdminUsersContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var query by remember { mutableStateOf("") }
    var blockTarget by remember { mutableStateOf<AdminUserRow?>(null) }
    var unblockTarget by remember { mutableStateOf<AdminUserRow?>(null) }
    val users = ui.users.filter { user ->
        query.isBlank() || listOfNotNull(user.fullName, user.phone, user.cityName, user.regionName).any { it.contains(query, true) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AbikTextField(query, { query = it }, "Поиск по имени, телефону или городу") }
        items(users, key = { it.id }) { user ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.userDetails(user.id) }) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(user.fullName ?: user.phone.phoneToDisplay(), style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(user.phone.phoneToDisplay(), user.cityName, user.regionName).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                if (user.teams.isNotEmpty()) Text(user.teams.joinToString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.superAdmin) AssistChip(onClick = {}, label = { Text("Суперадмин") })
                    if (user.adAdmin) AssistChip(onClick = {}, label = { Text("Реклама") })
                    if (user.status == "BLOCKED") AssistChip(onClick = {}, label = { Text("Заблокирован") })
                }
                if (!user.superAdmin) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { viewModel.setAdAdmin(user.id, !user.adAdmin) }) { Text(if (user.adAdmin) "Отозвать рекламу" else "Назначить рекламой") }
                    TextButton(onClick = { if (user.status == "BLOCKED") unblockTarget = user else blockTarget = user }) {
                        Text(if (user.status == "BLOCKED") "Разблокировать" else "Заблокировать", color = if (user.status == "BLOCKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            } }
        }
    }
    blockTarget?.let { user -> ReasonDialog("Блокировка пользователя", "Укажите причину. Все сессии пользователя будут завершены.", onDismiss = { blockTarget = null }) { reason -> viewModel.blockUser(user.id, reason); blockTarget = null } }
    unblockTarget?.let { user -> ConfirmAdminDialog("Разблокировать пользователя?", user.fullName ?: user.phone, onDismiss = { unblockTarget = null }) { viewModel.unblockUser(user.id); unblockTarget = null } }
}

@Composable
fun AdminTeamsContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var query by remember { mutableStateOf("") }
    var blockTarget by remember { mutableStateOf<AdminTeamRow?>(null) }
    var unblockTarget by remember { mutableStateOf<AdminTeamRow?>(null) }
    val teams = ui.teams.filter { query.isBlank() || it.name.contains(query, true) || it.cityName?.contains(query, true) == true }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AbikTextField(query, { query = it }, "Поиск команды") }
        items(teams, key = { it.id }) { team ->
            Card(Modifier.fillMaxWidth().clickable { viewModel.teamDetails(team.id) }) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(team.name, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(team.cityName, team.regionName).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                Text("Участников: ${team.membersCount} • Событий: ${team.eventsCount}")
                Text("Начислено: ${team.chargeAmountKopecks.kopecksToRubles()} • Платежи: ${team.paymentAmountKopecks.kopecksToRubles()}", style = MaterialTheme.typography.bodySmall)
                if (team.status == "BLOCKED") Text("Заблокирована: ${team.blockReason.orEmpty()}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { if (team.status == "BLOCKED") unblockTarget = team else blockTarget = team }) {
                    Text(if (team.status == "BLOCKED") "Разблокировать" else "Заблокировать", color = if (team.status == "BLOCKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            } }
        }
    }
    blockTarget?.let { team -> ReasonDialog("Блокировка команды", "После блокировки участники смогут только просматривать историю.", onDismiss = { blockTarget = null }) { reason -> viewModel.blockTeam(team.id, reason); blockTarget = null } }
    unblockTarget?.let { team -> ConfirmAdminDialog("Разблокировать команду?", team.name, onDismiss = { unblockTarget = null }) { viewModel.unblockTeam(team.id); unblockTarget = null } }
}

@Composable
fun ReasonDialog(title: String, description: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(description); AbikTextField(reason, { reason = it.take(500) }, "Причина", singleLine = false)
    } }, confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }, enabled = reason.isNotBlank()) { Text("Подтвердить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
fun ConfirmAdminDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text("Подтвердить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
fun AdminUserDetailsDialog(details: AdminUserDetails, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(details.user.fullName ?: details.user.phone) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Телефон: ${details.user.phone.phoneToDisplay()}") }
            item { Text("Статус: ${details.user.status}") }
            details.user.blockReason?.let { item { Text("Причина блокировки: $it", color = MaterialTheme.colorScheme.error) } }
            items(details.teams) { team -> Card { Column(Modifier.padding(12.dp)) {
                Text(team.teamName, fontWeight = FontWeight.Bold)
                Text("Роль: ${team.role} • Баланс: ${team.balanceKopecks.kopecksToRubles()}")
                Text("Начисления: ${team.chargeCount} / ${team.chargeAmountKopecks.kopecksToRubles()}")
                Text("Платежи: ${team.paymentCount} / ${team.paymentAmountKopecks.kopecksToRubles()}")
            } } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } })
}

@Composable
fun AdminTeamDetailsDialog(details: AdminTeamDetails, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(details.team.name) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Статус: ${details.team.status} • ${details.team.cityName.orEmpty()}") }
            item { Text("Участники", style = MaterialTheme.typography.titleMedium) }
            items(details.members) { member -> Column {
                Text(member.fullName, fontWeight = FontWeight.Medium)
                Text("${member.role}${if (member.trainer) " • тренер" else ""} • ${member.balanceKopecks.kopecksToRubles()}", style = MaterialTheme.typography.bodySmall)
            } }
            item { Text("Последние события", style = MaterialTheme.typography.titleMedium) }
            items(details.events) { event -> Text("${event.date} ${event.time} • ${event.title} • ${event.status}") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } })
}

