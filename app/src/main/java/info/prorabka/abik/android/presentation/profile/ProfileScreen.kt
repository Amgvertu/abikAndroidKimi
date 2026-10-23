package info.prorabka.abik.android.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.core.util.phoneToDisplay

/**
 * Вкладка «Профиль» нижнего меню: внутри две вкладки — «Профиль» и «Настройки».
 */
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onChangePhone: () -> Unit,
    onAdmin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Профиль") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Настройки") })
        }

        when (tab) {
            0 -> ProfileTab(
                ui = ui,
                onEdit = onEditProfile,
            )
            1 -> SettingsTab(
                themeMode = themeMode,
                onThemeChange = viewModel::setTheme,
                isDebug = viewModel.isDebugBuild,
                isSuperAdmin = ui.isSuperAdmin,
                onUseRemote = viewModel::useRemoteServer,
                onUseLocal = viewModel::useLocalServer,
                onChangePhone = onChangePhone,
                onAdmin = onAdmin,
            )
        }
    }
}

@Composable
private fun ProfileTab(ui: ProfileViewModel.UiState, onEdit: () -> Unit) {
    if (ui.loading) {
        LoadingBox()
        return
    }
    val profile = ui.profile
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Аватар",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.padding(6.dp))
                Column {
                    Text(
                        profile?.fullName ?: "Профиль не заполнен",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        ui.phone.phoneToDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.jerseyNumber?.let {
                        Text(
                            "№$it",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (profile != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileRow("Дата рождения", profile.birthDate?.toString()?.isoDateToDisplay() ?: "—")
                    ProfileRow("Амплуа", profile.positions.joinToString(", ") { it.display })
                    ProfileRow("Рост", "${profile.heightCm} см")
                    ProfileRow("Вес", "${profile.weightKg} кг")
                    ProfileRow("Хват", profile.stickHand.display)
                    ProfileRow(
                        "Город",
                        listOfNotNull(profile.cityName.ifBlank { null }, profile.regionName)
                            .joinToString(", ")
                            .ifBlank { "—" },
                    )
                }
            }
        }

        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Редактировать профиль")
        }
    }
}

@Composable
private fun SettingsTab(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    isDebug: Boolean,
    isSuperAdmin: Boolean,
    onUseRemote: () -> Unit,
    onUseLocal: () -> Unit,
    onChangePhone: () -> Unit,
    onAdmin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Тема оформления", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChange(ThemeMode.SYSTEM) },
                        label = { Text("Системная") },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        label = { Text("Светлая") },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        label = { Text("Тёмная") },
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Аккаунт", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onChangePhone) { Text("Сменить номер телефона") }
            }
        }

        if (isSuperAdmin) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Администрирование", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onAdmin) { Text("Панель суперадмина") }
                }
            }
        }

        if (isDebug) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Сервер (debug)", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onUseRemote) { Text("varamy.online (прод)") }
                    TextButton(onClick = onUseLocal) { Text("192.168.0.119:8083 (локальный)") }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
