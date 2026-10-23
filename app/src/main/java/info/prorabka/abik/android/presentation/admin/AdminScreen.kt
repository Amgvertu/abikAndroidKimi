package info.prorabka.abik.android.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.util.phoneToDisplay

/** Панель суперадмина: пользователи, команды, справочники. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Суперадмин") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.loading -> LoadingBox(Modifier.padding(padding))
            ui.forbidden -> EmptyState("Доступ только для суперадмина", Modifier.padding(padding))
            else -> Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Пользователи") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Команды") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Справочники") })
                }
                ErrorText(ui.error)
                ui.info?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                when (tab) {
                    0 -> UsersTab(ui, viewModel)
                    1 -> TeamsTab(ui)
                    2 -> DictionariesTab(
                        onAddRegion = { showRegionDialog = true },
                        onAddCity = { showCityDialog = true },
                    )
                }
            }
        }
    }

    if (showRegionDialog) {
        RegionDialog(
            onDismiss = { showRegionDialog = false },
            onCreate = { name, code, tz ->
                viewModel.createRegion(name, code, tz)
                showRegionDialog = false
            },
        )
    }
    if (showCityDialog) {
        CityDialog(
            regions = ui.regions.map { it.id to it.name },
            onDismiss = { showCityDialog = false },
            onCreate = { regionId, name ->
                viewModel.createCity(regionId, name)
                showCityDialog = false
            },
        )
    }
}

@Composable
private fun UsersTab(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    LazyColumn {
        items(ui.users, key = { it.id }) { user ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.phone.phoneToDisplay(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        buildString {
                            append(user.status)
                            if (user.isSuperAdmin) append(" • суперадмин")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!user.isSuperAdmin) {
                    TextButton(onClick = { viewModel.toggleBlock(user) }) {
                        Text(
                            if (user.status == "BLOCKED") "Разблокировать" else "Заблокировать",
                            color = if (user.status == "BLOCKED") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun TeamsTab(ui: AdminViewModel.UiState) {
    LazyColumn {
        items(ui.teams, key = { it.id }) { team ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(team.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    listOfNotNull(team.cityName, team.regionName, team.status).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun DictionariesTab(onAddRegion: () -> Unit, onAddCity: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Справочники регионов и городов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AbikButton(text = "Добавить регион", onClick = onAddRegion)
        AbikButton(text = "Добавить город", onClick = onAddCity)
    }
}

@Composable
private fun RegionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Europe/Moscow") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый регион") },
        text = {
            Column {
                AbikTextField(value = name, onValueChange = { name = it }, label = "Название")
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(value = code, onValueChange = { code = it }, label = "ISO-код (RU-MOW)")
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = "Часовой пояс",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), code.trim(), timezone.trim()) },
                enabled = name.isNotBlank() && code.isNotBlank() && timezone.isNotBlank(),
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDialog(
    regions: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onCreate: (Long, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый город") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedRegion?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Регион") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region.second) },
                                onClick = {
                                    selectedRegion = region
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(value = name, onValueChange = { name = it }, label = "Название города")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedRegion?.let { onCreate(it.first, name.trim()) } },
                enabled = selectedRegion != null && name.isNotBlank(),
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
