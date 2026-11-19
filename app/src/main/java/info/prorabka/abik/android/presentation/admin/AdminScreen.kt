package info.prorabka.abik.android.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.*

/** Админка суперадмина или отдельный кабинет администратора рекламы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    adOnly: Boolean = false,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var tab by remember(adOnly) { mutableIntStateOf(if (adOnly) 4 else 0) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    LaunchedEffect(adOnly) { viewModel.load(adOnly) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (adOnly) "Реклама" else "Администрирование") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { info.prorabka.abik.android.presentation.guide.GuideHelpButton() },
            )
        },
    ) { padding ->
        AbikPullToRefresh(loading = ui.loading, onRefresh = { viewModel.load(adOnly) }) {
            when {
                ui.loading && ui.ads.isEmpty() -> LoadingBox(Modifier.padding(padding))
                ui.forbidden -> EmptyState("Недостаточно прав для работы с этим разделом", Modifier.padding(padding))
                else -> Column(Modifier.fillMaxSize().padding(padding)) {
                    if (!adOnly) {
                        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                            listOf("Статистика", "Пользователи", "Команды", "Рассылки", "Реклама", "Регистрации", "Дополнительно").forEachIndexed { index, title ->
                                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                            }
                        }
                    }
                    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                    ui.info?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                    Box(Modifier.fillMaxSize()) {
                        when (tab) {
                            0 -> AdminStatsContent(ui, viewModel)
                            1 -> AdminUsersContent(ui, viewModel)
                            2 -> AdminTeamsContent(ui, viewModel)
                            3 -> AdminMessagesContent(ui, viewModel)
                            4 -> AdminAdsContent(ui, viewModel)
                            5 -> AdminRegistrationContent(ui, viewModel)
                            6 -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Справочники", style = MaterialTheme.typography.titleMedium)
                                AbikButton("Добавить регион", onClick = { showRegionDialog = true })
                                AbikButton("Добавить город", onClick = { showCityDialog = true })
                                HorizontalDivider()
                                ReleaseTab(viewModel::publishRelease)
                            }
                        }
                    }
                }
            }
        }
    }

    ui.selectedUser?.let { details -> AdminUserDetailsDialog(details, onDismiss = viewModel::closeDetails) }
    ui.selectedTeam?.let { details -> AdminTeamDetailsDialog(details, onDismiss = viewModel::closeDetails) }
    ui.selectedAdStats?.let { stats -> AdminAdStatsDialog(stats, onDismiss = viewModel::closeDetails) }

    if (showRegionDialog) RegionDialog(onDismiss = { showRegionDialog = false }, onCreate = { name, code, timezone ->
        viewModel.createRegion(name, code, timezone); showRegionDialog = false
    })
    if (showCityDialog) CityDialog(regions = ui.regions.map { it.id to it.name }, onDismiss = { showCityDialog = false }, onCreate = { regionId, name ->
        viewModel.createCity(regionId, name); showCityDialog = false
    })
}

@Composable
private fun RegionDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Europe/Moscow") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новый регион") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AbikTextField(name, { name = it }, "Название")
            AbikTextField(code, { code = it }, "ISO-код (RU-MOW)")
            AbikTextField(timezone, { timezone = it }, "Часовой пояс")
        }
    }, confirmButton = {
        TextButton(onClick = { onCreate(name.trim(), code.trim(), timezone.trim()) }, enabled = name.isNotBlank() && code.isNotBlank() && timezone.isNotBlank()) { Text("Добавить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDialog(regions: List<Pair<Long, String>>, onDismiss: () -> Unit, onCreate: (Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Pair<Long, String>?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новый город") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminDropdown("Регион", regions, selected, { selected = it }, { it.second })
            AbikTextField(name, { name = it }, "Название города")
        }
    }, confirmButton = {
        TextButton(onClick = { selected?.let { onCreate(it.first, name.trim()) } }, enabled = selected != null && name.isNotBlank()) { Text("Добавить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
